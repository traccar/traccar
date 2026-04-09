use axum::{
    extract::State,
    http::StatusCode,
    routing::post,
    Json, Router,
};
use serde::Deserialize;
use std::sync::Arc;

use crate::AppState;

pub fn routes() -> Router<Arc<AppState>> {
    Router::new().route("/api/password/reset", post(reset_password))
}

#[derive(Deserialize)]
pub struct ResetRequest {
    pub email: String,
}

/// POST /password/reset - initiate a password reset for the given email.
async fn reset_password(
    State(state): State<Arc<AppState>>,
    Json(body): Json<ResetRequest>,
) -> Result<StatusCode, StatusCode> {
    // Verify the user exists
    let request = traccar_storage::Request::new(traccar_storage::Columns::Include(vec![
        "id".into(),
        "email".into(),
    ]))
    .with_condition(traccar_storage::Condition::Equals(
        "email".into(),
        serde_json::json!(body.email),
    ));
    let _user = state
        .storage
        .get_object("tc_users", &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
        .ok_or(StatusCode::NOT_FOUND)?;

    // In a full implementation this would generate a reset token and send
    // it via email. For now we just acknowledge the request.
    tracing::info!(email = %body.email, "Password reset requested");
    Ok(StatusCode::NO_CONTENT)
}
