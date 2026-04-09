use thiserror::Error;

#[derive(Debug, Error)]
pub enum TraccarError {
    #[error("Storage error: {0}")]
    Storage(String),

    #[error("Protocol error: {0}")]
    Protocol(String),

    #[error("Auth error: {0}")]
    Auth(String),

    #[error("Not found")]
    NotFound,

    #[error("Permission denied")]
    PermissionDenied,

    #[error("Bad request: {0}")]
    BadRequest(String),

    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),

    #[error("JSON error: {0}")]
    Json(#[from] serde_json::Error),

    #[error("Config error: {0}")]
    Config(String),

    #[error("{0}")]
    Other(String),
}

impl From<sqlx::Error> for TraccarError {
    fn from(e: sqlx::Error) -> Self {
        TraccarError::Storage(e.to_string())
    }
}

pub type TraccarResult<T> = Result<T, TraccarError>;
