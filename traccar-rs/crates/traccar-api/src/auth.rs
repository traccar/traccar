use axum::{
    async_trait,
    extract::FromRequestParts,
    http::{header, request::Parts, StatusCode},
};
use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, Validation};
use serde::{Deserialize, Serialize};
use std::sync::Arc;

use crate::AppState;

/// Secret used for JWT signing. In production this should come from config.
const JWT_SECRET: &[u8] = b"traccar-rs-secret-key";

/// Token lifetime in seconds (24 hours).
const TOKEN_EXPIRY_SECS: i64 = 86400;

/// JWT claims.
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Claims {
    pub sub: i64,        // user id
    pub email: String,
    pub admin: bool,
    pub exp: i64,        // expiration (unix timestamp)
    pub iat: i64,        // issued at
}

/// Authenticated user extracted from the Authorization header.
#[derive(Debug, Clone)]
pub struct AuthUser {
    pub id: i64,
    pub email: String,
    pub administrator: bool,
}

/// Create a signed JWT for the given user.
pub fn create_token(user_id: i64, email: &str, admin: bool) -> Result<String, StatusCode> {
    let now = chrono::Utc::now().timestamp();
    let claims = Claims {
        sub: user_id,
        email: email.to_string(),
        admin,
        iat: now,
        exp: now + TOKEN_EXPIRY_SECS,
    };
    encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(JWT_SECRET),
    )
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)
}

/// Verify and decode a JWT, returning the claims.
pub fn verify_token(token: &str) -> Result<Claims, StatusCode> {
    decode::<Claims>(
        token,
        &DecodingKey::from_secret(JWT_SECRET),
        &Validation::default(),
    )
    .map(|data| data.claims)
    .map_err(|_| StatusCode::UNAUTHORIZED)
}

/// Hash a plaintext password with bcrypt.
pub fn hash_password(password: &str) -> Result<String, StatusCode> {
    bcrypt::hash(password, bcrypt::DEFAULT_COST).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)
}

/// Verify a plaintext password against a bcrypt hash.
pub fn verify_password(password: &str, hash: &str) -> Result<bool, StatusCode> {
    bcrypt::verify(password, hash).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)
}

/// Axum extractor: pulls the Bearer token from the Authorization header,
/// validates it, and yields an `AuthUser`.
#[async_trait]
impl FromRequestParts<Arc<AppState>> for AuthUser {
    type Rejection = StatusCode;

    async fn from_request_parts(
        parts: &mut Parts,
        _state: &Arc<AppState>,
    ) -> Result<Self, Self::Rejection> {
        let auth_header = parts
            .headers
            .get(header::AUTHORIZATION)
            .and_then(|v| v.to_str().ok())
            .ok_or(StatusCode::UNAUTHORIZED)?;

        let token = auth_header
            .strip_prefix("Bearer ")
            .ok_or(StatusCode::UNAUTHORIZED)?;

        let claims = verify_token(token)?;

        Ok(AuthUser {
            id: claims.sub,
            email: claims.email,
            administrator: claims.admin,
        })
    }
}
