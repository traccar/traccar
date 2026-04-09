pub mod auth;
pub mod middleware;
pub mod routes;
pub mod websocket;

use std::sync::Arc;
use axum::Router;
use tower_http::cors::{Any, CorsLayer};
use tower_http::trace::TraceLayer;
use traccar_storage::Storage;

/// Shared application state available to all route handlers.
pub struct AppState {
    pub config: Arc<Config>,
    pub storage: Arc<dyn Storage>,
}

/// Build the full Axum router with all API routes, middleware, and layers.
pub fn build_router(state: Arc<AppState>) -> Router {
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    Router::new()
        .merge(routes::all_routes())
        .with_state(state)
        .layer(TraceLayer::new_for_http())
        .layer(cors)
}

// ─── Minimal config definition ──────────────────────────────────────
// Mirrors the subset of configuration the API layer needs.
// The binary crate owns the canonical `Config`; this keeps traccar-api
// self-contained so it compiles and tests independently.

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct Config {
    #[serde(default)]
    pub web: WebConfig,
    #[serde(default)]
    pub database: DatabaseConfig,
    #[serde(default)]
    pub geocoder: GeocoderConfig,
    #[serde(default)]
    pub mail: MailConfig,
    #[serde(default)]
    pub sms: SmsConfig,
    #[serde(default)]
    pub server: ServerConfig,
    #[serde(default)]
    pub media: MediaConfig,
    #[serde(default)]
    pub extra: HashMap<String, toml::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WebConfig {
    #[serde(default = "default_port")]
    pub port: u16,
    #[serde(default = "default_addr")]
    pub address: String,
    pub path: Option<String>,
    #[serde(default)]
    pub debug: bool,
    #[serde(default)]
    pub console: bool,
}
fn default_port() -> u16 { 8082 }
fn default_addr() -> String { "0.0.0.0".into() }
impl Default for WebConfig {
    fn default() -> Self {
        Self { port: 8082, address: "0.0.0.0".into(), path: None, debug: false, console: false }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DatabaseConfig {
    #[serde(default = "default_db")]
    pub url: String,
    pub user: Option<String>,
    pub password: Option<String>,
    #[serde(default)]
    pub save_empty: bool,
    #[serde(default = "default_max")]
    pub max_connections: u32,
}
fn default_db() -> String { "sqlite:traccar.db".into() }
fn default_max() -> u32 { 10 }
impl Default for DatabaseConfig {
    fn default() -> Self {
        Self { url: default_db(), user: None, password: None, save_empty: false, max_connections: 10 }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct GeocoderConfig {
    #[serde(default)]
    pub enable: bool,
    #[serde(rename = "type")]
    pub geocoder_type: Option<String>,
    pub url: Option<String>,
    pub key: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct MailConfig {
    pub smtp_host: Option<String>,
    pub smtp_port: Option<u16>,
    pub smtp_user: Option<String>,
    pub smtp_password: Option<String>,
    pub smtp_from: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct SmsConfig {
    pub http_url: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct ServerConfig {
    #[serde(default)]
    pub registration: bool,
    #[serde(default)]
    pub readonly: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct MediaConfig {
    pub path: Option<String>,
}
