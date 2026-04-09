pub mod database;
pub mod query;

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use serde_json::Value;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum StorageError {
    #[error("Database error: {0}")]
    Database(String),
    #[error("Not found")]
    NotFound,
    #[error("Invalid query: {0}")]
    InvalidQuery(String),
}

impl From<sqlx::Error> for StorageError {
    fn from(e: sqlx::Error) -> Self {
        StorageError::Database(e.to_string())
    }
}

// ─── Query types ─────────────────────────────────────────────────────

#[derive(Debug, Clone)]
pub enum Columns {
    All,
    Include(Vec<String>),
    Exclude(Vec<String>),
}

#[derive(Debug, Clone)]
pub enum Condition {
    Equals(String, Value),
    NotEquals(String, Value),
    GreaterThan(String, Value),
    LessThan(String, Value),
    Between(String, Value, Value),
    Contains(Vec<String>, String),
    And(Box<Condition>, Box<Condition>),
    Or(Box<Condition>, Box<Condition>),
    Permission {
        owner_class: String,
        owner_id: i64,
        property_class: String,
        property_id: i64,
        include_groups: bool,
    },
    LatestPositions {
        device_id: i64,
    },
}

impl Condition {
    pub fn merge(conditions: Vec<Condition>) -> Option<Condition> {
        conditions.into_iter().reduce(|a, b| Condition::And(Box::new(a), Box::new(b)))
    }
}

#[derive(Debug, Clone)]
pub struct QueryOrder {
    pub column: String,
    pub descending: bool,
    pub limit: i64,
    pub offset: i64,
}

#[derive(Debug, Clone)]
pub struct Request {
    pub columns: Columns,
    pub condition: Option<Condition>,
    pub order: Option<QueryOrder>,
}

impl Request {
    pub fn new(columns: Columns) -> Self {
        Self {
            columns,
            condition: None,
            order: None,
        }
    }

    pub fn with_condition(mut self, condition: Condition) -> Self {
        self.condition = Some(condition);
        self
    }

    pub fn with_order(mut self, order: QueryOrder) -> Self {
        self.order = Some(order);
        self
    }
}

// ─── Permission model ────────────────────────────────────────────────

#[derive(Debug, Clone)]
pub struct PermissionEntry {
    pub owner_class: String,
    pub owner_id: i64,
    pub property_class: String,
    pub property_id: i64,
}

// ─── Storage trait ───────────────────────────────────────────────────

#[async_trait]
pub trait Storage: Send + Sync {
    async fn get_objects(&self, table: &str, request: &Request) -> Result<Vec<Value>, StorageError>;
    async fn get_object(&self, table: &str, request: &Request) -> Result<Option<Value>, StorageError> {
        let objects = self.get_objects(table, request).await?;
        Ok(objects.into_iter().next())
    }
    async fn add_object(&self, table: &str, entity: &Value, columns: &Columns) -> Result<i64, StorageError>;
    async fn update_object(&self, table: &str, entity: &Value, request: &Request) -> Result<(), StorageError>;
    async fn remove_object(&self, table: &str, request: &Request) -> Result<(), StorageError>;
    async fn get_permissions(
        &self,
        owner_class: &str,
        owner_id: i64,
        property_class: &str,
        property_id: i64,
    ) -> Result<Vec<PermissionEntry>, StorageError>;
    async fn add_permission(&self, entry: &PermissionEntry) -> Result<(), StorageError>;
    async fn remove_permission(&self, entry: &PermissionEntry) -> Result<(), StorageError>;
}
