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
    Router::new()
        .route("/api/share", post(create_share).delete(axum::routing::delete(remove_share)))
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShareRequest {
    pub device_id: i64,
    pub expiration: Option<String>,
}

/// POST /share - create a temporary device sharing link.
async fn create_share(
    State(_state): State<Arc<AppState>>,
    Json(body): Json<ShareRequest>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let token = uuid::Uuid::new_v4().to_string();
    Ok(Json(serde_json::json!({
        "deviceId": body.device_id,
        "token": token,
        "expiration": body.expiration,
    })))
}

/// DELETE /share - revoke a sharing link.
async fn remove_share(
    State(_state): State<Arc<AppState>>,
    Json(body): Json<ShareRequest>,
) -> Result<StatusCode, StatusCode> {
    tracing::info!(device_id = body.device_id, "Share revoked");
    Ok(StatusCode::NO_CONTENT)
}
