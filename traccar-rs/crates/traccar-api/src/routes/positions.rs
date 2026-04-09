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
    Router::new().route("/api/positions", get(list))
}

#[derive(Deserialize)]
pub struct PositionQuery {
    pub device_id: Option<i64>,
    pub id: Option<i64>,
    pub from: Option<String>,
    pub to: Option<String>,
}

async fn list(
    State(state): State<Arc<AppState>>,
    Query(query): Query<PositionQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    let mut request = traccar_storage::Request::new(traccar_storage::Columns::All);

    if let Some(id) = query.id {
        request = request.with_condition(traccar_storage::Condition::Equals(
            "id".into(),
            serde_json::json!(id),
        ));
    } else if let Some(device_id) = query.device_id {
        let mut cond = traccar_storage::Condition::Equals(
            "deviceId".into(),
            serde_json::json!(device_id),
        );
        if let (Some(from), Some(to)) = (&query.from, &query.to) {
            cond = traccar_storage::Condition::And(
                Box::new(cond),
                Box::new(traccar_storage::Condition::Between(
                    "fixTime".into(),
                    serde_json::json!(from),
                    serde_json::json!(to),
                )),
            );
        }
        request = request.with_condition(cond);
    } else {
        // Without filters, return latest positions for all devices
        request = request.with_condition(traccar_storage::Condition::LatestPositions {
            device_id: 0,
        });
    }

    let result = state
        .storage
        .get_objects("tc_positions", &request)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(result))
}
