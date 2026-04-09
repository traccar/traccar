use async_trait::async_trait;
use sqlx::sqlite::{SqlitePool, SqlitePoolOptions, SqliteRow};
use sqlx::{Column, Row};
use serde_json::Value;
use tracing::info;

use crate::{Columns, PermissionEntry, Request, Storage, StorageError};
use crate::query;

pub struct DatabaseStorage {
    pool: SqlitePool,
}

impl DatabaseStorage {
    pub async fn new(database_url: &str, max_connections: u32) -> Result<Self, StorageError> {
        info!("Connecting to database: {}", database_url);
        let pool = SqlitePoolOptions::new()
            .max_connections(max_connections)
            .connect(database_url)
            .await?;
        Ok(Self { pool })
    }

    pub fn pool(&self) -> &SqlitePool {
        &self.pool
    }

    pub async fn run_migrations(&self) -> Result<(), StorageError> {
        sqlx::migrate!("../../migrations")
            .run(&self.pool)
            .await
            .map_err(|e| StorageError::Database(format!("Migration error: {}", e)))?;
        Ok(())
    }

    fn row_to_json(row: &SqliteRow) -> Value {
        let mut map = serde_json::Map::new();
        for col in row.columns() {
            let name = col.name();
            // Try different types
            if let Ok(v) = row.try_get::<i64, _>(name) {
                map.insert(name.to_string(), Value::Number(v.into()));
            } else if let Ok(v) = row.try_get::<f64, _>(name) {
                if let Some(n) = serde_json::Number::from_f64(v) {
                    map.insert(name.to_string(), Value::Number(n));
                }
            } else if let Ok(v) = row.try_get::<String, _>(name) {
                // Try to parse as JSON first (for attributes column)
                if let Ok(json) = serde_json::from_str::<Value>(&v) {
                    map.insert(name.to_string(), json);
                } else {
                    map.insert(name.to_string(), Value::String(v));
                }
            } else if let Ok(v) = row.try_get::<bool, _>(name) {
                map.insert(name.to_string(), Value::Bool(v));
            } else {
                map.insert(name.to_string(), Value::Null);
            }
        }
        Value::Object(map)
    }
}

#[async_trait]
impl Storage for DatabaseStorage {
    async fn get_objects(&self, table: &str, request: &Request) -> Result<Vec<Value>, StorageError> {
        let mut sql = query::format_select(table, &request.columns);
        let mut params: Vec<Value> = Vec::new();

        if let Some(ref condition) = request.condition {
            let (cond_sql, cond_params) = query::format_condition(condition);
            sql.push_str(&cond_sql);
            params.extend(cond_params);
        }

        if let Some(ref order) = request.order {
            sql.push_str(&query::format_order(order));
        }

        let mut query = sqlx::query(&sql);
        for param in &params {
            query = match param {
                Value::Number(n) => {
                    if let Some(i) = n.as_i64() {
                        query.bind(i)
                    } else if let Some(f) = n.as_f64() {
                        query.bind(f)
                    } else {
                        query.bind(n.to_string())
                    }
                }
                Value::String(s) => query.bind(s.clone()),
                Value::Bool(b) => query.bind(*b),
                _ => query.bind(param.to_string()),
            };
        }

        let rows = query.fetch_all(&self.pool).await?;
        Ok(rows.iter().map(Self::row_to_json).collect())
    }

    async fn add_object(&self, table: &str, entity: &Value, columns: &Columns) -> Result<i64, StorageError> {
        let obj = entity.as_object().ok_or(StorageError::InvalidQuery("Entity must be an object".into()))?;

        let cols: Vec<String> = match columns {
            Columns::All => obj.keys().filter(|k| *k != "id").cloned().collect(),
            Columns::Include(c) => c.clone(),
            Columns::Exclude(exc) => obj.keys().filter(|k| !exc.contains(k)).cloned().collect(),
        };

        let sql = query::format_insert(table, &cols);
        let mut query = sqlx::query(&sql);

        for col in &cols {
            let val = obj.get(col).unwrap_or(&Value::Null);
            query = match val {
                Value::Number(n) => {
                    if let Some(i) = n.as_i64() {
                        query.bind(i)
                    } else if let Some(f) = n.as_f64() {
                        query.bind(f)
                    } else {
                        query.bind(n.to_string())
                    }
                }
                Value::String(s) => query.bind(s.clone()),
                Value::Bool(b) => query.bind(*b),
                Value::Null => query.bind(Option::<String>::None),
                other => query.bind(other.to_string()),
            };
        }

        let result = query.execute(&self.pool).await?;
        Ok(result.last_insert_rowid())
    }

    async fn update_object(&self, table: &str, entity: &Value, request: &Request) -> Result<(), StorageError> {
        let obj = entity.as_object().ok_or(StorageError::InvalidQuery("Entity must be an object".into()))?;

        let cols: Vec<String> = match &request.columns {
            Columns::All => obj.keys().filter(|k| *k != "id").cloned().collect(),
            Columns::Include(c) => c.clone(),
            Columns::Exclude(exc) => obj.keys().filter(|k| !exc.contains(k)).cloned().collect(),
        };

        let mut sql = query::format_update(table, &cols);
        let mut params: Vec<Value> = cols.iter()
            .map(|c| obj.get(c).cloned().unwrap_or(Value::Null))
            .collect();

        if let Some(ref condition) = request.condition {
            let (cond_sql, cond_params) = query::format_condition(condition);
            sql.push_str(&cond_sql);
            params.extend(cond_params);
        }

        let mut query = sqlx::query(&sql);
        for val in &params {
            query = match val {
                Value::Number(n) => {
                    if let Some(i) = n.as_i64() {
                        query.bind(i)
                    } else if let Some(f) = n.as_f64() {
                        query.bind(f)
                    } else {
                        query.bind(n.to_string())
                    }
                }
                Value::String(s) => query.bind(s.clone()),
                Value::Bool(b) => query.bind(*b),
                Value::Null => query.bind(Option::<String>::None),
                other => query.bind(other.to_string()),
            };
        }

        query.execute(&self.pool).await?;
        Ok(())
    }

    async fn remove_object(&self, table: &str, request: &Request) -> Result<(), StorageError> {
        let mut sql = query::format_delete(table);
        let mut params: Vec<Value> = Vec::new();

        if let Some(ref condition) = request.condition {
            let (cond_sql, cond_params) = query::format_condition(condition);
            sql.push_str(&cond_sql);
            params.extend(cond_params);
        }

        let mut query = sqlx::query(&sql);
        for val in &params {
            query = match val {
                Value::Number(n) => {
                    if let Some(i) = n.as_i64() {
                        query.bind(i)
                    } else if let Some(f) = n.as_f64() {
                        query.bind(f)
                    } else {
                        query.bind(n.to_string())
                    }
                }
                Value::String(s) => query.bind(s.clone()),
                Value::Bool(b) => query.bind(*b),
                Value::Null => query.bind(Option::<String>::None),
                other => query.bind(other.to_string()),
            };
        }

        query.execute(&self.pool).await?;
        Ok(())
    }

    async fn get_permissions(
        &self,
        owner_class: &str,
        owner_id: i64,
        property_class: &str,
        property_id: i64,
    ) -> Result<Vec<PermissionEntry>, StorageError> {
        let table = format!("tc_{}_{}", owner_class, property_class);
        let owner_key = format!("{}id", owner_class.trim_end_matches('s'));
        let property_key = format!("{}id", property_class.trim_end_matches('s'));

        let mut sql = format!("SELECT * FROM {}", table);
        let mut conditions = Vec::new();
        let mut bind_values = Vec::new();

        if owner_id > 0 {
            conditions.push(format!("{} = ?", owner_key));
            bind_values.push(owner_id);
        }
        if property_id > 0 {
            conditions.push(format!("{} = ?", property_key));
            bind_values.push(property_id);
        }

        if !conditions.is_empty() {
            sql.push_str(" WHERE ");
            sql.push_str(&conditions.join(" AND "));
        }

        let mut query = sqlx::query(&sql);
        for val in &bind_values {
            query = query.bind(*val);
        }

        let rows = query.fetch_all(&self.pool).await?;
        let mut results = Vec::new();
        for row in &rows {
            let oid: i64 = row.try_get(&*owner_key).unwrap_or(0);
            let pid: i64 = row.try_get(&*property_key).unwrap_or(0);
            results.push(PermissionEntry {
                owner_class: owner_class.to_string(),
                owner_id: oid,
                property_class: property_class.to_string(),
                property_id: pid,
            });
        }
        Ok(results)
    }

    async fn add_permission(&self, entry: &PermissionEntry) -> Result<(), StorageError> {
        let table = format!("tc_{}_{}", entry.owner_class, entry.property_class);
        let owner_key = format!("{}id", entry.owner_class.trim_end_matches('s'));
        let property_key = format!("{}id", entry.property_class.trim_end_matches('s'));

        let sql = format!("INSERT INTO {} ({}, {}) VALUES (?, ?)", table, owner_key, property_key);
        sqlx::query(&sql)
            .bind(entry.owner_id)
            .bind(entry.property_id)
            .execute(&self.pool)
            .await?;
        Ok(())
    }

    async fn remove_permission(&self, entry: &PermissionEntry) -> Result<(), StorageError> {
        let table = format!("tc_{}_{}", entry.owner_class, entry.property_class);
        let owner_key = format!("{}id", entry.owner_class.trim_end_matches('s'));
        let property_key = format!("{}id", entry.property_class.trim_end_matches('s'));

        let sql = format!("DELETE FROM {} WHERE {} = ? AND {} = ?", table, owner_key, property_key);
        sqlx::query(&sql)
            .bind(entry.owner_id)
            .bind(entry.property_id)
            .execute(&self.pool)
            .await?;
        Ok(())
    }
}
