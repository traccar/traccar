use axum::http::StatusCode;
use std::sync::Arc;

use crate::auth::AuthUser;
use crate::AppState;

/// Service for checking various permission levels.
pub struct PermissionsService {
    state: Arc<AppState>,
}

#[derive(Debug, thiserror::Error)]
pub enum PermissionError {
    #[error("Permission denied")]
    Denied,
    #[error("Read-only access")]
    ReadOnly,
    #[error("Not found")]
    NotFound,
}

impl From<PermissionError> for StatusCode {
    fn from(err: PermissionError) -> Self {
        match err {
            PermissionError::Denied => StatusCode::FORBIDDEN,
            PermissionError::ReadOnly => StatusCode::FORBIDDEN,
            PermissionError::NotFound => StatusCode::NOT_FOUND,
        }
    }
}

impl PermissionsService {
    pub fn new(state: Arc<AppState>) -> Self {
        Self { state }
    }

    /// Verify that the user has basic access (is authenticated and not disabled).
    pub async fn check_permission(
        &self,
        user: &AuthUser,
        object_class: &str,
        object_id: i64,
    ) -> Result<(), PermissionError> {
        if user.administrator {
            return Ok(());
        }
        // Check permission in storage
        let perms = self
            .state
            .storage
            .get_permissions("User", user.id, object_class, object_id)
            .await
            .map_err(|_| PermissionError::Denied)?;
        if perms.is_empty() {
            return Err(PermissionError::Denied);
        }
        Ok(())
    }

    /// Verify the user is an administrator.
    pub fn check_admin(user: &AuthUser) -> Result<(), PermissionError> {
        if user.administrator {
            Ok(())
        } else {
            Err(PermissionError::Denied)
        }
    }

    /// Verify the user can view/edit their own resources or is an admin.
    pub fn check_user(user: &AuthUser, target_user_id: i64) -> Result<(), PermissionError> {
        if user.administrator || user.id == target_user_id {
            Ok(())
        } else {
            Err(PermissionError::Denied)
        }
    }

    /// Verify the user has edit rights (not read-only).
    pub fn check_edit(user: &AuthUser, is_readonly: bool) -> Result<(), PermissionError> {
        if is_readonly && !user.administrator {
            Err(PermissionError::ReadOnly)
        } else {
            Ok(())
        }
    }
}
