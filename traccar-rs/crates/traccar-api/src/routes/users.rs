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
        .route("/api/users", get(list).post(create))
        .route("/api/users/{id}", put(update).delete(axum::routing::delete(remove)))
}

#[derive(Deserialize)]
pub struct UserQuery {
    pub user_id: Option<i64>,
}

async fn list(
    State(state): State<Arc<AppState>>,
    Query(query): Query<UserQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    let mut request = traccar_storage::Request::new(traccar_storage::Columns::Exclude(vec![
        "hashedPassword".into(),
        "salt".into(),
    ]));
    if let Some(uid) = query.user_id {
        request = request.with_condition(traccar_storage::Condition::Equals(
            "id".into(),
            serde_json::json!(uid),
        ));
    }
    let result = state
        .storage
        .get_objects("tc_users", &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(result))
}

async fn create(
    State(state): State<Arc<AppState>>,
    Json(mut body): Json<serde_json::Value>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    // Hash password if provided
    if let Some(password) = body.get("password").and_then(|v| v.as_str()) {
        let hashed = crate::auth::hash_password(password)?;
        if let Some(obj) = body.as_object_mut() {
            obj.insert("hashedPassword".into(), serde_json::json!(hashed));
            obj.remove("password");
        }
    }
    let id = state
        .storage
        .add_object("tc_users", &body, &traccar_storage::Columns::All)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if let Some(obj) = body.as_object_mut() {
        obj.insert("id".into(), serde_json::json!(id));
        obj.remove("hashedPassword");
    }
    Ok(Json(body))
}

async fn update(
    State(state): State<Arc<AppState>>,
    Path(id): Path<i64>,
    Json(mut body): Json<serde_json::Value>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    if let Some(password) = body.get("password").and_then(|v| v.as_str()) {
        let hashed = crate::auth::hash_password(password)?;
        if let Some(obj) = body.as_object_mut() {
            obj.insert("hashedPassword".into(), serde_json::json!(hashed));
            obj.remove("password");
        }
    }
    let request = traccar_storage::Request::new(traccar_storage::Columns::All)
        .with_condition(traccar_storage::Condition::Equals("id".into(), serde_json::json!(id)));
    state
        .storage
        .update_object("tc_users", &body, &request)
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
        .remove_object("tc_users", &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(StatusCode::NO_CONTENT)
}
