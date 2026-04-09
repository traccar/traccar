use axum::{
    extract::State,
    http::StatusCode,
    routing::{get, put},
    Json, Router,
};
use std::sync::Arc;

use crate::AppState;

pub fn routes() -> Router<Arc<AppState>> {
    Router::new().route("/api/server", get(get_server).put(update_server))
}

async fn get_server(
    State(state): State<Arc<AppState>>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let request = traccar_storage::Request::new(traccar_storage::Columns::All);
    let server = state
        .storage
        .get_object("tc_servers", &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
        .unwrap_or(serde_json::json!({
            "id": 1,
            "registration": false,
            "readonly": false,
        }));
    Ok(Json(server))
}

async fn update_server(
    State(state): State<Arc<AppState>>,
    Json(body): Json<serde_json::Value>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let request = traccar_storage::Request::new(traccar_storage::Columns::All)
        .with_condition(traccar_storage::Condition::Equals("id".into(), serde_json::json!(1)));
    state
        .storage
        .update_object("tc_servers", &body, &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(body))
}
