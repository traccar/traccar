/*
 * Copyright 2023 Daniel Raper (me@danr.uk)
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
package org.traccar.database;

import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.api.resource.SessionResource;
import org.traccar.api.security.LoginService;
import org.traccar.model.User;
import org.traccar.storage.StorageException;
import org.traccar.helper.LogAction;
import org.traccar.helper.ServletHelper;

import java.net.URI;
import java.net.URISyntaxException;
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

public class OpenIdProvider {
    public final Boolean force;
    private final ClientID clientId;
    private final Secret clientSecret;
    private URI callbackUrl;
    private URI authUrl;
    private URI tokenUrl;
    private URI userInfoUrl;
    private URI baseUrl;
    private final String adminGroup;

    private LoginService loginService;

    @Inject
    public OpenIdProvider(Config config, LoginService loginService) {
        force = config.getBoolean(Keys.OPENID_FORCE);
        clientId = new ClientID(config.getString(Keys.OPENID_CLIENTID));
        clientSecret = new Secret(config.getString(Keys.OPENID_CLIENTSECRET));

        this.loginService = loginService;

        try {
            callbackUrl = new URI(config.getString(Keys.WEB_URL, "") + "/api/session/openid/callback");
            authUrl = new URI(config.getString(Keys.OPENID_AUTHURL, ""));
            tokenUrl = new URI(config.getString(Keys.OPENID_TOKENURL, ""));
            userInfoUrl = new URI(config.getString(Keys.OPENID_USERINFOURL, ""));
            baseUrl = new URI(config.getString(Keys.WEB_URL, ""));
        } catch (URISyntaxException e) {
        }

        adminGroup = config.getString(Keys.OPENID_ADMINGROUP);
    }

    public URI createAuthUri() {
        AuthenticationRequest request = new AuthenticationRequest.Builder(
                new ResponseType("code"),
                new Scope("openid", "profile", "email", "groups"),
                clientId,
                callbackUrl)
                .endpointURI(authUrl)
                .state(new State())
                .nonce(new Nonce())
                .build();

        return request.toURI();
    }

    private OIDCTokenResponse getToken(AuthorizationCode code) {
        // Credentials to authenticate us to the token endpoint
        ClientAuthentication clientAuth = new ClientSecretBasic(clientId, clientSecret);
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callbackUrl);

        TokenRequest request = new TokenRequest(tokenUrl, clientAuth, codeGrant);
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

    private UserInfo getUserInfo(BearerAccessToken token) throws IOException, ParseException {
        UserInfoResponse userInfoResponse;

        HTTPResponse httpResponse = new UserInfoRequest(userInfoUrl, token)
                .toHTTPRequest()
                .send();

        userInfoResponse = UserInfoResponse.parse(httpResponse);

        if (!userInfoResponse.indicatesSuccess()) {
            // User info request failed - usually from expiring
            return null;
        }

        return userInfoResponse.toSuccessResponse().getUserInfo();
    }

    public Response handleCallback(URI requestUri, HttpServletRequest request) throws StorageException, ParseException, IOException, WebApplicationException {
        AuthorizationResponse response = AuthorizationResponse.parse(requestUri);

        if (!response.indicatesSuccess()) {
            AuthorizationErrorResponse error = response.toErrorResponse();
            throw new WebApplicationException(Response.status(403).entity(error.getErrorObject().getDescription()).build());
        }

        AuthorizationCode authCode = response.toSuccessResponse().getAuthorizationCode();

        if (authCode == null) {
            return Response.status(403).entity( "Invalid OpenID Connect callback.").build();
        }

        OIDCTokenResponse tokens = getToken(authCode);

        if (tokens == null) {
            return Response.status(403).entity("Unable to authenticate with the OpenID Connect provider. Please try again.").build();
        }

        BearerAccessToken bearerToken = tokens.getOIDCTokens().getBearerAccessToken();

        UserInfo userInfo = getUserInfo(bearerToken);

        if (userInfo == null) {
            return Response.status(500).entity("Failed to access OpenID Connect user info endpoint. Please contact your administrator.").build();
        }

        User user = loginService.login(userInfo.getEmailAddress(), userInfo.getName(), userInfo.getStringListClaim("groups").contains(adminGroup));

        request.getSession().setAttribute(SessionResource.USER_ID_KEY, user.getId());
        LogAction.login(user.getId(), ServletHelper.retrieveRemoteAddress(request));
        return Response.seeOther(
                baseUrl).build();
    }
}
