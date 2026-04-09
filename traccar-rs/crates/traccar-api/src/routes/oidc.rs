use axum::{
    extract::{Query, State},
    http::StatusCode,
    routing::get,
    Json, Router,
};
use serde::Deserialize;
use std::sync::Arc;

use crate::AppState;

pub fn routes() -> Router<Arc<AppState>> {
    Router::new().route("/api/oidc/callback", get(oidc_callback))
}

#[derive(Deserialize)]
pub struct OidcCallback {
    pub code: Option<String>,
    pub state: Option<String>,
    pub error: Option<String>,
}

/// GET /oidc/callback - handle the OAuth2/OIDC redirect callback.
async fn oidc_callback(
    State(_state): State<Arc<AppState>>,
    Query(params): Query<OidcCallback>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    if let Some(err) = &params.error {
        tracing::warn!(error = %err, "OIDC callback error");
        return Err(StatusCode::BAD_REQUEST);
    }

    let code = params.code.as_deref().ok_or(StatusCode::BAD_REQUEST)?;

    // In a full implementation this would exchange the code for tokens,
    // look up or create the user, and return a session token.
    tracing::info!("OIDC callback received with code");
    Ok(Json(serde_json::json!({
        "status": "ok",
        "code": code,
    })))
}
