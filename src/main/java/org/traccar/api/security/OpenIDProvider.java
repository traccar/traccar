/*
 * Copyright 2017 - 2023 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.api.security;

import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.api.resource.SessionResource;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Request;
import org.traccar.storage.query.Columns;
import org.traccar.helper.LogAction;
import org.traccar.helper.ServletHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import com.google.inject.Inject;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;

import com.nimbusds.openid.connect.sdk.claims.UserInfo;

public class OpenIDProvider {
    public final Boolean force;
    private final ClientID clientId;
    private final Secret clientSecret;
    private URI callbackUrl;
    private URI authUrl;
    private URI tokenUrl;
    private URI userInfoUrl;
    private URI baseUrl;
    private final String adminGroup;

    private Config config;
    private LoginService loginService;
    private Storage storage;

    @Inject
    public OpenIDProvider(Config config, LoginService loginService, Storage storage) {
        force = config.getBoolean(Keys.OIDC_FORCE);
        clientId = new ClientID(config.getString(Keys.OIDC_CLIENTID));
        clientSecret = new Secret(config.getString(Keys.OIDC_CLIENTSECRET));

        this.config = config;
        this.storage = storage;
        this.loginService = loginService;

        try {
            callbackUrl = new URI(config.getString(Keys.WEB_URL, "") + "/api/session/openid/callback");
            authUrl = new URI(config.getString(Keys.OIDC_AUTHURL, ""));
            tokenUrl = new URI(config.getString(Keys.OIDC_TOKENURL, ""));
            userInfoUrl = new URI(config.getString(Keys.OIDC_USERINFOURL, ""));
            baseUrl = new URI(config.getString(Keys.WEB_URL, ""));
        } catch (URISyntaxException e) {
        }

        adminGroup = config.getString(Keys.OIDC_ADMINGROUP);
    }

    public URI createAuthRequest() {
        AuthenticationRequest request = new AuthenticationRequest.Builder(
                new ResponseType("code"),
                new Scope("openid", "profile", "email", "groups"),
                this.clientId,
                this.callbackUrl)
                .endpointURI(this.authUrl)
                .state(new State())
                .nonce(new Nonce())
                .build();

        return request.toURI();
    }

    private OIDCTokenResponse getToken(AuthorizationCode code) {
        // Credentials to authenticate us to the token endpoint
        ClientAuthentication clientAuth = new ClientSecretBasic(this.clientId, this.clientSecret);
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, this.callbackUrl);

        TokenRequest request = new TokenRequest(this.tokenUrl, clientAuth, codeGrant);
        TokenResponse tokenResponse;

        try {
            HTTPResponse tokenReq = request.toHTTPRequest().send();
            tokenResponse = OIDCTokenResponseParser.parse(tokenReq);
            if (!tokenResponse.indicatesSuccess()) {
                return null;
            }

            return (OIDCTokenResponse) tokenResponse.toSuccessResponse();
        } catch (IOException e) {
            return null;
        } catch (ParseException e) {
            return null;
        }
    }

    private AuthorizationCode parseCallback(URI requri) throws WebApplicationException {
        AuthorizationResponse response;

        try {
            response = AuthorizationResponse.parse(requri);
        } catch (ParseException e) {
            return null;
        }

        if (!response.indicatesSuccess()) {
            AuthorizationErrorResponse error = response.toErrorResponse();
            throw new WebApplicationException(Response.status(403).entity(error.getErrorObject().getDescription()).build());
        }

        return response.toSuccessResponse().getAuthorizationCode();
    }

    private UserInfo getUserInfo(BearerAccessToken token) {
        UserInfoResponse userInfoResponse;

        try {
            HTTPResponse httpResponse = new UserInfoRequest(this.userInfoUrl, token)
                    .toHTTPRequest()
                    .send();

            userInfoResponse = UserInfoResponse.parse(httpResponse);
        } catch (IOException e) {
            return null;
        } catch (ParseException e) {
            return null;
        }

        if (!userInfoResponse.indicatesSuccess()) {
            // User info request failed - usually from expiring
            return null;
        }

        return userInfoResponse.toSuccessResponse().getUserInfo();
    }

    private User createUser(String name, String email, Boolean administrator) throws StorageException {
        User user = new User();

        user.setName(name);
        user.setEmail(email);
        user.setFixedEmail(true);
        user.setDeviceLimit(this.config.getInteger(Keys.USERS_DEFAULT_DEVICE_LIMIT));

        int expirationDays = this.config.getInteger(Keys.USERS_DEFAULT_EXPIRATION_DAYS);

        if (expirationDays > 0) {
            user.setExpirationTime(new Date(System.currentTimeMillis() + expirationDays * 86400000L));
        }

        if (administrator) {
            user.setAdministrator(true);
        }

        user.setId(this.storage.addObject(user, new Request(new Columns.Exclude("id"))));

        return user;
    }

    public Response handleCallback(URI requri, HttpServletRequest request) throws StorageException, WebApplicationException {
        // Parse callback
        AuthorizationCode authCode = this.parseCallback(requri);

        if (authCode == null) {
            return Response.status(403).entity( "Invalid OpenID Connect callback.").build();
        }

        // Get token from IDP
        OIDCTokenResponse tokens = this.getToken(authCode);

        if (tokens == null) {
            return Response.status(403).entity("Unable to authenticate with the OpenID Connect provider. Please try again.").build();
        }

        BearerAccessToken bearerToken = tokens.getOIDCTokens().getBearerAccessToken();

        // Get user info from IDP
        UserInfo idpUser = this.getUserInfo(bearerToken);

        if (idpUser == null) {
            return Response.status(500).entity("Failed to access OpenID Connect user info endpoint. Please contact your administrator.").build();
        }

        String email = idpUser.getEmailAddress();
        String name = idpUser.getName();

        // Check if user exists
        User user = this.loginService.lookup(email);

        // If user does not exist, create one
        if (user == null) {
            List<String> groups = idpUser.getStringListClaim("groups");
            Boolean administrator = groups.contains(this.adminGroup);
            user = this.createUser(name, email, administrator);
        }

        // Set user session and redirect to homepage
        request.getSession().setAttribute(SessionResource.USER_ID_KEY, user.getId());
        LogAction.login(user.getId(), ServletHelper.retrieveRemoteAddress(request));
        return Response.seeOther(
                baseUrl).build();
    }
}
