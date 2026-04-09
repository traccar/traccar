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
        .route("/api/permissions", post(create).delete(axum::routing::delete(remove)))
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequest {
    /// e.g. "User", "Device", "Group"
    pub owner_class: Option<String>,
    pub owner_id: Option<i64>,
    pub property_class: Option<String>,
    pub property_id: Option<i64>,
    // Shorthand fields used by the Traccar web client
    pub user_id: Option<i64>,
    pub device_id: Option<i64>,
    pub group_id: Option<i64>,
    pub geofence_id: Option<i64>,
}

impl PermissionRequest {
    fn to_entry(&self) -> traccar_storage::PermissionEntry {
        let (owner_class, owner_id) = if let (Some(c), Some(id)) = (&self.owner_class, self.owner_id) {
            (c.clone(), id)
        } else {
            ("User".into(), self.user_id.unwrap_or(0))
        };
        let (property_class, property_id) = if let (Some(c), Some(id)) = (&self.property_class, self.property_id) {
            (c.clone(), id)
        } else if let Some(did) = self.device_id {
            ("Device".into(), did)
        } else if let Some(gid) = self.group_id {
            ("Group".into(), gid)
        } else if let Some(gid) = self.geofence_id {
            ("Geofence".into(), gid)
        } else {
            ("Unknown".into(), 0)
        };
        traccar_storage::PermissionEntry {
            owner_class,
            owner_id,
            property_class,
            property_id,
        }
    }
}

async fn create(
    State(state): State<Arc<AppState>>,
    Json(body): Json<PermissionRequest>,
) -> Result<StatusCode, StatusCode> {
    let entry = body.to_entry();
    state
        .storage
        .add_permission(&entry)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(StatusCode::NO_CONTENT)
}

async fn remove(
    State(state): State<Arc<AppState>>,
    Json(body): Json<PermissionRequest>,
) -> Result<StatusCode, StatusCode> {
    let entry = body.to_entry();
    state
        .storage
        .remove_permission(&entry)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(StatusCode::NO_CONTENT)
}
