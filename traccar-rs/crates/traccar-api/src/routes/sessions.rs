use axum::{
    extract::State,
    http::StatusCode,
    routing::{get, post},
    Json, Router,
};
use serde::Deserialize;
use std::sync::Arc;

use crate::{auth, AppState};

pub fn routes() -> Router<Arc<AppState>> {
    Router::new()
        .route("/api/session", get(get_session).post(create_session).delete(axum::routing::delete(delete_session)))
}

#[derive(Deserialize)]
pub struct LoginRequest {
    pub email: Option<String>,
    pub password: Option<String>,
    pub token: Option<String>,
}

/// GET /session - return the current user from the bearer token.
async fn get_session(
    user: Result<auth::AuthUser, StatusCode>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let user = user?;
    Ok(Json(serde_json::json!({
        "id": user.id,
        "email": user.email,
        "administrator": user.administrator,
    })))
}

/// POST /session - authenticate with email/password and return a token.
async fn create_session(
    State(state): State<Arc<AppState>>,
    Json(body): Json<LoginRequest>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let email = body.email.as_deref().ok_or(StatusCode::BAD_REQUEST)?;
    let password = body.password.as_deref().ok_or(StatusCode::BAD_REQUEST)?;

    // Look up user by email
    let request = traccar_storage::Request::new(traccar_storage::Columns::All)
        .with_condition(traccar_storage::Condition::Equals(
            "email".into(),
            serde_json::json!(email),
        ));
    let user_value = state
        .storage
        .get_object("tc_users", &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
        .ok_or(StatusCode::UNAUTHORIZED)?;

    // Verify password
    let hashed = user_value
        .get("hashedPassword")
        .and_then(|v| v.as_str())
        .ok_or(StatusCode::UNAUTHORIZED)?;
    if !auth::verify_password(password, hashed)? {
        return Err(StatusCode::UNAUTHORIZED);
    }

    let user_id = user_value.get("id").and_then(|v| v.as_i64()).unwrap_or(0);
    let is_admin = user_value
        .get("administrator")
        .and_then(|v| v.as_bool())
        .unwrap_or(false);

    let token = auth::create_token(user_id, email, is_admin)?;

    Ok(Json(serde_json::json!({
        "id": user_id,
        "email": email,
        "administrator": is_admin,
        "token": token,
    })))
}

/// DELETE /session - logout (token-based, so this is a no-op on the server).
async fn delete_session() -> StatusCode {
    StatusCode::NO_CONTENT
}
