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
        .route("/api/devices", get(list).post(create))
        .route("/api/devices/{id}", put(update).delete(axum::routing::delete(remove)))
        .route("/api/devices/{id}/accumulators", put(update_accumulators))
        .route("/api/devices/{id}/image", post(upload_image))
}

#[derive(Deserialize)]
pub struct DeviceQuery {
    pub all: Option<bool>,
    pub user_id: Option<i64>,
    pub unique_id: Option<String>,
    pub id: Option<i64>,
}

async fn list(
    State(state): State<Arc<AppState>>,
    Query(query): Query<DeviceQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    let mut request = traccar_storage::Request::new(traccar_storage::Columns::All);
    if let Some(id) = query.id {
        request = request.with_condition(traccar_storage::Condition::Equals(
            "id".into(),
            serde_json::json!(id),
        ));
    } else if let Some(uid) = &query.unique_id {
        request = request.with_condition(traccar_storage::Condition::Equals(
            "uniqueId".into(),
            serde_json::json!(uid),
        ));
    }
    let result = state
        .storage
        .get_objects("tc_devices", &request)
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
        .add_object("tc_devices", &body, &traccar_storage::Columns::All)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let mut result = body;
    if let Some(obj) = result.as_object_mut() {
        obj.insert("id".into(), serde_json::json!(id));
    }
    Ok(Json(result))
}

async fn update(
    State(state): State<Arc<AppState>>,
    Path(id): Path<i64>,
    Json(body): Json<serde_json::Value>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    let request = traccar_storage::Request::new(traccar_storage::Columns::All)
        .with_condition(traccar_storage::Condition::Equals("id".into(), serde_json::json!(id)));
    state
        .storage
        .update_object("tc_devices", &body, &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(body))
}

async fn remove(
    State(state): State<Arc<AppState>>,
    Path(id): Path<i64>,
) -> Result<StatusCode, StatusCode> {
    let request = traccar_storage::Request::new(traccar_storage::Columns::All)
        .with_condition(traccar_storage::Condition::Equals("id".into(), serde_json::json!(id)));
    state
        .storage
        .remove_object("tc_devices", &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(StatusCode::NO_CONTENT)
}

async fn update_accumulators(
    State(state): State<Arc<AppState>>,
    Path(id): Path<i64>,
    Json(body): Json<serde_json::Value>,
) -> Result<StatusCode, StatusCode> {
    // Update totalDistance and/or hours on the device
    let request = traccar_storage::Request::new(traccar_storage::Columns::Include(vec![
        "attributes".into(),
    ]))
    .with_condition(traccar_storage::Condition::Equals("id".into(), serde_json::json!(id)));
    state
        .storage
        .update_object("tc_devices", &body, &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(StatusCode::NO_CONTENT)
}

async fn upload_image(
    State(_state): State<Arc<AppState>>,
    Path(id): Path<i64>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    // Placeholder: accept image upload and store it
    Ok(Json(serde_json::json!({"id": id, "image": true})))
}
