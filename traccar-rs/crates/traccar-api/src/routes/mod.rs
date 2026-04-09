pub mod attributes;
pub mod audit;
pub mod calendars;
pub mod commands;
pub mod devices;
pub mod drivers;
pub mod events;
pub mod geofences;
pub mod groups;
pub mod health;
pub mod maintenance;
pub mod notifications;
pub mod oidc;
pub mod orders;
pub mod passwords;
pub mod permissions;
pub mod positions;
pub mod reports;
pub mod server;
pub mod sessions;
pub mod share;
pub mod statistics;
pub mod users;

use axum::Router;
use std::sync::Arc;
use crate::AppState;

/// Merge all API route modules and the WebSocket route into a single Router.
pub fn all_routes() -> Router<Arc<AppState>> {
    Router::new()
        .merge(health::routes())
        .merge(sessions::routes())
        .merge(server::routes())
        .merge(devices::routes())
        .merge(users::routes())
        .merge(positions::routes())
        .merge(events::routes())
        .merge(commands::routes())
        .merge(geofences::routes())
        .merge(groups::routes())
        .merge(notifications::routes())
        .merge(reports::routes())
        .merge(permissions::routes())
        .merge(drivers::routes())
        .merge(maintenance::routes())
        .merge(calendars::routes())
        .merge(attributes::routes())
        .merge(statistics::routes())
        .merge(orders::routes())
        .merge(passwords::routes())
        .merge(share::routes())
        .merge(oidc::routes())
        .merge(audit::routes())
        .merge(crate::websocket::routes())
}
