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
    Router::new()
        .route("/api/reports/route", get(report_route))
        .route("/api/reports/events", get(report_events))
        .route("/api/reports/trips", get(report_trips))
        .route("/api/reports/stops", get(report_stops))
        .route("/api/reports/summary", get(report_summary))
}

#[derive(Deserialize)]
pub struct ReportQuery {
    pub device_id: Option<Vec<i64>>,
    pub group_id: Option<Vec<i64>>,
    pub from: Option<String>,
    pub to: Option<String>,
}

async fn report_route(
    State(state): State<Arc<AppState>>,
    Query(query): Query<ReportQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    let positions = fetch_positions(&state, &query).await?;
    Ok(Json(positions))
}

async fn report_events(
    State(state): State<Arc<AppState>>,
    Query(query): Query<ReportQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    let events = fetch_events(&state, &query).await?;
    Ok(Json(events))
}

async fn report_trips(
    State(_state): State<Arc<AppState>>,
    Query(_query): Query<ReportQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    // Trip calculation requires position analysis; returns placeholder
    Ok(Json(vec![]))
}

async fn report_stops(
    State(_state): State<Arc<AppState>>,
    Query(_query): Query<ReportQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    // Stop detection requires position analysis; returns placeholder
    Ok(Json(vec![]))
}

async fn report_summary(
    State(_state): State<Arc<AppState>>,
    Query(_query): Query<ReportQuery>,
) -> Result<Json<Vec<serde_json::Value>>, StatusCode> {
    Ok(Json(vec![]))
}

// ─── helpers ────────────────────────────────────────────────────────

async fn fetch_positions(
    state: &AppState,
    query: &ReportQuery,
) -> Result<Vec<serde_json::Value>, StatusCode> {
    let device_ids = query.device_id.as_deref().unwrap_or(&[]);
    if device_ids.is_empty() {
        return Ok(vec![]);
    }
    let mut all = Vec::new();
    for &did in device_ids {
        let mut cond = traccar_storage::Condition::Equals(
            "deviceId".into(),
            serde_json::json!(did),
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
        let request = traccar_storage::Request::new(traccar_storage::Columns::All)
            .with_condition(cond);
        let rows = state
            .storage
            .get_objects("tc_positions", &request)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
        all.extend(rows);
    }
    Ok(all)
}

async fn fetch_events(
    state: &AppState,
    query: &ReportQuery,
) -> Result<Vec<serde_json::Value>, StatusCode> {
    let device_ids = query.device_id.as_deref().unwrap_or(&[]);
    if device_ids.is_empty() {
        return Ok(vec![]);
    }
    let mut all = Vec::new();
    for &did in device_ids {
        let mut cond = traccar_storage::Condition::Equals(
            "deviceId".into(),
            serde_json::json!(did),
        );
        if let (Some(from), Some(to)) = (&query.from, &query.to) {
            cond = traccar_storage::Condition::And(
                Box::new(cond),
                Box::new(traccar_storage::Condition::Between(
                    "eventTime".into(),
                    serde_json::json!(from),
                    serde_json::json!(to),
                )),
            );
        }
        let request = traccar_storage::Request::new(traccar_storage::Columns::All)
            .with_condition(cond);
        let rows = state
            .storage
            .get_objects("tc_events", &request)
            .await
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
        all.extend(rows);
    }
    Ok(all)
}
