/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api.resource;

import org.traccar.api.BaseResource;
import org.traccar.api.security.OidcSessionManager;
import org.traccar.api.security.OidcSessionManager.AuthorizationCode;
import org.traccar.api.signature.TokenManager;
import org.traccar.config.Config;
import org.traccar.model.User;
import org.traccar.storage.StorageException;
import com.nimbusds.jose.JOSEException;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Path("oidc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class OidcResource extends BaseResource {

    @Inject
    private Config config;

    @Inject
    private TokenManager tokenManager;

    @Inject
    private OidcSessionManager sessionManager;

    @PermitAll
    @GET
    @Path("authorize")
    public Response authorize(
            @QueryParam("client_id") String clientId,
            @QueryParam("redirect_uri") String redirectUri,
            @QueryParam("state") String state,
            @QueryParam("scope") String scope,
            @QueryParam("response_type") String responseType,
            @QueryParam("code_challenge") String codeChallenge,
            @QueryParam("code_challenge_method") String codeChallengeMethod,
            @QueryParam("nonce") String nonce) {

        URI target = URI.create(redirectUri);
        String code = sessionManager.issueCode(
                getUserId(), clientId, target, scope, nonce, codeChallenge, codeChallengeMethod);

        UriBuilder redirectBuilder = UriBuilder.fromUri(target).queryParam("code", code);
        if (state != null) {
            redirectBuilder.queryParam("state", state);
        }

        return Response.seeOther(redirectBuilder.build()).build();
    }

    @PermitAll
    @POST
    @Path("token")
    public Response token(
            @FormParam("grant_type") String grantType,
            @FormParam("code") String code,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("client_id") String clientId,
            @FormParam("code_verifier") String codeVerifier,
            @HeaderParam("Authorization") String authorization)
            throws StorageException, IOException, GeneralSecurityException, JOSEException {

        if (clientId == null && authorization != null && authorization.startsWith("Basic ")) {
            String credentials = new String(Base64.getDecoder().decode(
                    authorization.substring("Basic ".length())), StandardCharsets.UTF_8);
            clientId = credentials.substring(0, credentials.indexOf(':'));
        }

        AuthorizationCode authCode = sessionManager.consumeCode(
                code, clientId, redirectUri != null ? URI.create(redirectUri) : null, codeVerifier);
        if (authCode == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        String token = tokenManager.generateToken(authCode.userId());
        TokenManager.TokenData tokenData = tokenManager.decodeToken(token);
        long expiresIn = Math.max(0, (tokenData.getExpiration().getTime() - System.currentTimeMillis()) / 1000);
        Set<String> scopes = sessionManager.parseScopes(authCode.scope());
        User user = permissionsService.getUser(authCode.userId());
        String idToken = sessionManager.generateIdToken(authCode, clientId, tokenData, scopes, user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", token);
        response.put("token_type", "Bearer");
        response.put("expires_in", expiresIn);
        response.put("id_token", idToken);
        response.put("scope", authCode.scope());
        return Response.ok(response).build();
    }

    @GET
    @Path("userinfo")
    public Map<String, Object> userInfo() throws StorageException {
        User user = permissionsService.getUser(getUserId());
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("sub", String.valueOf(user.getId()));
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        return profile;
    }

    @PermitAll
    @GET
    @Path("jwks")
    public Map<String, Object> jwks() throws GeneralSecurityException, StorageException, JOSEException {
        return sessionManager.getJwks();
    }

}
