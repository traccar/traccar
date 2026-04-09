use axum::{
    extract::{Query, State},
    http::StatusCode,
    routing::{get, post},
    Json, Router,
};
use serde::Deserialize;
use std::sync::Arc;

use crate::AppState;

pub fn routes() -> Router<Arc<AppState>> {
    Router::new()
        .route("/api/commands", get(list).post(create))
        .route("/api/commands/send", post(send_command))
}

#[derive(Deserialize)]
pub struct CommandQuery {
    pub device_id: Option<i64>,
    pub all: Option<bool>,
}

async fn list(
    State(state): State<Arc<AppState>>,
    Query(query): Query<CommandQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    let mut request = traccar_storage::Request::new(traccar_storage::Columns::All);
    if let Some(device_id) = query.device_id {
        request = request.with_condition(traccar_storage::Condition::Equals(
            "deviceId".into(),
            serde_json::json!(device_id),
        ));
    }
    let result = state
        .storage
        .get_objects("tc_commands", &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(result))
}

async fn create(
    State(state): State<Arc<AppState>>,
    Json(body): Json<serde_json::Value>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let id = state
        .storage
        .add_object("tc_commands", &body, &traccar_storage::Columns::All)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let mut result = body;
    if let Some(obj) = result.as_object_mut() {
        obj.insert("id".into(), serde_json::json!(id));
    }
    Ok(Json(result))
}

async fn send_command(
    State(state): State<Arc<AppState>>,
    Json(body): Json<serde_json::Value>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    // Queue the command for delivery to the device via its protocol connection
    let id = state
        .storage
        .add_object("tc_commands_queue", &body, &traccar_storage::Columns::All)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(serde_json::json!({"id": id, "queued": true})))
}
