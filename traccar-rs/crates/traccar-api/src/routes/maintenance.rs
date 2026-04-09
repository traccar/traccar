use axum::{
    extract::{Path, Query, State},
    http::StatusCode,
    routing::{get, post, put},
    Json, Router,
};
use serde::Deserialize;
use std::sync::Arc;

use crate::AppState;

pub fn routes() -> Router<Arc<AppState>> {
    Router::new()
        .route("/api/maintenance", get(list).post(create))
        .route("/api/maintenance/{id}", put(update).delete(axum::routing::delete(remove)))
}

#[derive(Deserialize)]
pub struct MaintenanceQuery {
    pub all: Option<bool>,
    pub user_id: Option<i64>,
}

async fn list(
    State(state): State<Arc<AppState>>,
    Query(_query): Query<MaintenanceQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    let request = traccar_storage::Request::new(traccar_storage::Columns::All);
    let result = state.storage.get_objects("tc_maintenances", &request).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(result))
}

async fn create(
    State(state): State<Arc<AppState>>,
    Json(body): Json<serde_json::Value>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let id = state.storage.add_object("tc_maintenances", &body, &traccar_storage::Columns::All).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let mut result = body;
    if let Some(obj) = result.as_object_mut() { obj.insert("id".into(), serde_json::json!(id)); }
    Ok(Json(result))
}

async fn update(
    State(state): State<Arc<AppState>>,
    Path(id): Path<i64>,
    Json(body): Json<serde_json::Value>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let request = traccar_storage::Request::new(traccar_storage::Columns::All)
        .with_condition(traccar_storage::Condition::Equals("id".into(), serde_json::json!(id)));
    state.storage.update_object("tc_maintenances", &body, &request).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(body))
}

async fn remove(
    State(state): State<Arc<AppState>>,
    Path(id): Path<i64>,
) -> Result<StatusCode, StatusCode> {
    let request = traccar_storage::Request::new(traccar_storage::Columns::All)
        .with_condition(traccar_storage::Condition::Equals("id".into(), serde_json::json!(id)));
    state.storage.remove_object("tc_maintenances", &request).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(StatusCode::NO_CONTENT)
}
