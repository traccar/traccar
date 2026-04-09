mod config;
mod error;

use std::path::PathBuf;
use std::sync::Arc;
use tracing::{error, info};

use crate::config::Config;

#[tokio::main]
async fn main() {
    // Initialize logging
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "info".into()),
        )
        .init();

    info!("Traccar GPS Tracking Server (Rust) starting...");
    log_system_info();

    // Load configuration
    let config_path = std::env::args()
        .nth(1)
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from("config.toml"));

    let config = match Config::load(&config_path) {
        Ok(c) => c,
        Err(e) => {
            error!("Failed to load configuration from {:?}: {}", config_path, e);
            std::process::exit(1);
        }
    };

    info!("Configuration loaded from {:?}", config_path);

    // Initialize database
    let storage = match traccar_storage::database::DatabaseStorage::new(
        &config.database.url,
        config.database.max_connections,
    )
    .await
    {
        Ok(s) => s,
        Err(e) => {
            error!("Failed to connect to database: {}", e);
            std::process::exit(1);
        }
    };

    info!("Database connected: {}", config.database.url);

    // Run migrations
    if let Err(e) = storage.run_migrations().await {
        error!("Failed to run database migrations: {}", e);
        std::process::exit(1);
    }

    info!("Database migrations completed");

    let storage: Arc<dyn traccar_storage::Storage> = Arc::new(storage);

    // Convert to API config (serialize/deserialize between compatible types)
    let api_config: traccar_api::Config =
        serde_json::from_value(serde_json::to_value(&config).unwrap()).unwrap_or_default();

    // Build API state
    let api_state = Arc::new(traccar_api::AppState {
        config: Arc::new(api_config),
        storage: storage.clone(),
    });

    // Build API router
    let router = traccar_api::build_router(api_state.clone());

    // Start protocol servers
    let protocol_config = Arc::new(config);
    let protocol_storage = storage.clone();
    tokio::spawn(async move {
        if let Err(e) =
            traccar_protocol::start_servers(&protocol_config, protocol_storage).await
        {
            error!("Protocol server error: {}", e);
        }
    });

    // Start scheduled tasks
    let schedule_storage = storage.clone();
    tokio::spawn(async move {
        traccar_schedule::run_scheduler(&(), schedule_storage).await;
    });

    // Start web server
    let bind_addr = format!(
        "{}:{}",
        api_state.config.web.address, api_state.config.web.port
    );
    info!("Web server listening on {}", bind_addr);

    let listener = match tokio::net::TcpListener::bind(&bind_addr).await {
        Ok(l) => l,
        Err(e) => {
            error!("Failed to bind web server to {}: {}", bind_addr, e);
            std::process::exit(1);
        }
    };

    // Graceful shutdown
    let shutdown = async {
        tokio::signal::ctrl_c()
            .await
            .expect("Failed to install signal handler");
        info!("Shutdown signal received, stopping server...");
    };

    info!("Server started successfully");

    axum::serve(listener, router)
        .with_graceful_shutdown(shutdown)
        .await
        .unwrap_or_else(|e| {
            error!("Server error: {}", e);
        });

    info!("Server stopped");
}

fn log_system_info() {
    info!("OS: {} {}", std::env::consts::OS, std::env::consts::ARCH);
    info!(
        "Traccar version: {}",
        env!("CARGO_PKG_VERSION")
    );
}
