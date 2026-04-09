use axum::{
    extract::{
        ws::{Message, WebSocket, WebSocketUpgrade},
        State,
    },
    response::IntoResponse,
    routing::get,
    Router,
};
use dashmap::DashMap;
use futures::{SinkExt, StreamExt};
use std::sync::Arc;
use tokio::sync::broadcast;
use tracing::{debug, info};

use crate::AppState;

/// Manages WebSocket connections per user.
pub struct WebSocketManager {
    /// Maps user_id -> broadcast sender for that user's updates.
    senders: DashMap<i64, broadcast::Sender<String>>,
}

impl WebSocketManager {
    pub fn new() -> Self {
        Self {
            senders: DashMap::new(),
        }
    }

    /// Get or create a broadcast channel for the given user, returning a receiver.
    pub fn subscribe(&self, user_id: i64) -> broadcast::Receiver<String> {
        let sender = self
            .senders
            .entry(user_id)
            .or_insert_with(|| broadcast::channel(256).0);
        sender.subscribe()
    }

    /// Send a JSON payload to all connections for the given user.
    pub fn send_to_user(&self, user_id: i64, message: String) {
        if let Some(sender) = self.senders.get(&user_id) {
            let _ = sender.send(message);
        }
    }

    /// Broadcast a payload to all connected users.
    pub fn broadcast_all(&self, message: &str) {
        for entry in self.senders.iter() {
            let _ = entry.value().send(message.to_string());
        }
    }
}

impl Default for WebSocketManager {
    fn default() -> Self {
        Self::new()
    }
}

/// WebSocket route.
pub fn routes() -> Router<Arc<AppState>> {
    Router::new().route("/api/socket", get(ws_handler))
}

/// Upgrade HTTP connection to WebSocket.
async fn ws_handler(
    ws: WebSocketUpgrade,
    State(_state): State<Arc<AppState>>,
) -> impl IntoResponse {
    ws.on_upgrade(handle_socket)
}

/// Handle a single WebSocket session.
async fn handle_socket(mut socket: WebSocket) {
    info!("WebSocket client connected");

    // Send initial empty state
    let _ = socket
        .send(Message::Text(
            serde_json::json!({"positions": [], "events": []}).to_string(),
        ))
        .await;

    // Read messages until the client disconnects
    while let Some(msg) = socket.recv().await {
        match msg {
            Ok(Message::Text(text)) => {
                debug!("WebSocket received: {}", text);
            }
            Ok(Message::Close(_)) => break,
            Err(_) => break,
            _ => {}
        }
    }

    info!("WebSocket client disconnected");
}
