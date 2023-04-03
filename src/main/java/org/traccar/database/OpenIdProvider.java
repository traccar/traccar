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
import java.security.GeneralSecurityException;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenIdProvider.class);
    
    public final Boolean force;
    private final ClientID clientId;
    private final ClientAuthentication clientAuth;
    private URI callbackUrl;
    private URI authUrl;
    private URI tokenUrl;
    private URI userInfoUrl;
    private URI baseUrl;
    private final String adminGroup;

    private LoginService loginService;

    @Inject
    public OpenIdProvider(Config config, LoginService loginService) {     
        this.loginService = loginService;

        force = config.getBoolean(Keys.OPENID_FORCE);
        clientId = new ClientID(config.getString(Keys.OPENID_CLIENTID));
        clientAuth = new ClientSecretBasic(clientId, new Secret(config.getString(Keys.OPENID_CLIENTSECRET)));

        try {
            callbackUrl = new URI(config.getString(Keys.WEB_URL, "") + "/api/session/openid/callback");
            authUrl = new URI(config.getString(Keys.OPENID_AUTHURL, ""));
            tokenUrl = new URI(config.getString(Keys.OPENID_TOKENURL, ""));
            userInfoUrl = new URI(config.getString(Keys.OPENID_USERINFOURL, ""));
            baseUrl = new URI(config.getString(Keys.WEB_URL, ""));
        } catch(URISyntaxException error) {
            LOGGER.error("Invalid URIs provided in OpenID configuration");
        }

        adminGroup = config.getString(Keys.OPENID_ADMINGROUP);
    }

    public URI createAuthUri() {
        AuthenticationRequest.Builder request = new AuthenticationRequest.Builder(
                new ResponseType("code"),
                new Scope("openid", "profile", "email", "groups"),
                clientId,
                callbackUrl);
                
        return request.endpointURI(authUrl)
                .state(new State())
                .build()
                .toURI();
    }

    private OIDCTokenResponse getToken(AuthorizationCode code) throws IOException, ParseException, GeneralSecurityException {
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callbackUrl);
        TokenRequest tokenRequest = new TokenRequest(tokenUrl, clientAuth, codeGrant);

        HTTPResponse tokenResponse = tokenRequest.toHTTPRequest().send();
        TokenResponse token = OIDCTokenResponseParser.parse(tokenResponse);
        if (!token.indicatesSuccess()) {
            throw new GeneralSecurityException("Unable to authenticate with the OpenID Connect provider.");
        }

        return (OIDCTokenResponse) token.toSuccessResponse();
    }

    private UserInfo getUserInfo(BearerAccessToken token) throws IOException, ParseException, GeneralSecurityException {
        HTTPResponse httpResponse = new UserInfoRequest(userInfoUrl, token)
                .toHTTPRequest()
                .send();

        UserInfoResponse userInfoResponse = UserInfoResponse.parse(httpResponse);

        if (!userInfoResponse.indicatesSuccess()) {
            throw new GeneralSecurityException("Failed to access OpenID Connect user info endpoint. Please contact your administrator.");
        }

        return userInfoResponse.toSuccessResponse().getUserInfo();
    }

    public URI handleCallback(URI requestUri, HttpServletRequest request) throws StorageException, ParseException, IOException, GeneralSecurityException {
        AuthorizationResponse response = AuthorizationResponse.parse(requestUri);

        if (!response.indicatesSuccess()) {
            throw new GeneralSecurityException(response.toErrorResponse().getErrorObject().getDescription());
        }

        AuthorizationCode authCode = response.toSuccessResponse().getAuthorizationCode();

        if (authCode == null) {
            throw new GeneralSecurityException( "Malformed OpenID callback.");
        }

        OIDCTokenResponse tokens = getToken(authCode);

        BearerAccessToken bearerToken = tokens.getOIDCTokens().getBearerAccessToken();

        UserInfo userInfo = getUserInfo(bearerToken);

        User user = loginService.login(userInfo.getEmailAddress(), userInfo.getName(), userInfo.getStringListClaim("groups").contains(adminGroup));

        request.getSession().setAttribute(SessionResource.USER_ID_KEY, user.getId());
        LogAction.login(user.getId(), ServletHelper.retrieveRemoteAddress(request));

        return baseUrl;
    }
}
