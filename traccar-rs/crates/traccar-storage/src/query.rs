use crate::{Columns, Condition, QueryOrder, StorageError};

/// Build SQL query fragments from our query types.

pub fn format_select(table: &str, columns: &Columns) -> String {
    let cols = match columns {
        Columns::All => "*".to_string(),
        Columns::Include(cols) => cols.join(", "),
        Columns::Exclude(cols) => {
            // For exclude, we select * and filter at deserialization time
            // Real implementation would introspect schema
            let _ = cols;
            "*".to_string()
        }
    };
    format!("SELECT {} FROM {}", cols, table)
}

pub fn format_condition(condition: &Condition) -> (String, Vec<serde_json::Value>) {
    let mut params = Vec::new();
    let sql = format_condition_inner(condition, &mut params);
    (format!(" WHERE {}", sql), params)
}

fn format_condition_inner(condition: &Condition, params: &mut Vec<serde_json::Value>) -> String {
    match condition {
        Condition::Equals(col, val) => {
            params.push(val.clone());
            format!("{} = ?", col)
        }
        Condition::NotEquals(col, val) => {
            params.push(val.clone());
            format!("{} != ?", col)
        }
        Condition::GreaterThan(col, val) => {
            params.push(val.clone());
            format!("{} > ?", col)
        }
        Condition::LessThan(col, val) => {
            params.push(val.clone());
            format!("{} < ?", col)
        }
        Condition::Between(col, from, to) => {
            params.push(from.clone());
            params.push(to.clone());
            format!("{} BETWEEN ? AND ?", col)
        }
        Condition::Contains(cols, _val) => {
            let parts: Vec<String> = cols.iter().map(|c| {
                params.push(serde_json::Value::String(format!("%{}%", _val.to_lowercase())));
                format!("LOWER({}) LIKE ?", c)
            }).collect();
            format!("({})", parts.join(" OR "))
        }
        Condition::And(a, b) => {
            let left = format_condition_inner(a, params);
            let right = format_condition_inner(b, params);
            format!("{} AND {}", left, right)
        }
        Condition::Or(a, b) => {
            let left = format_condition_inner(a, params);
            let right = format_condition_inner(b, params);
            format!("({} OR {})", left, right)
        }
        Condition::Permission { owner_class, owner_id, property_class, property_id, .. } => {
            let link_table = format!("tc_{}_{}",
                owner_class.to_lowercase().trim_start_matches("tc_"),
                property_class.to_lowercase().trim_start_matches("tc_")
            );
            if *owner_id > 0 {
                let key = format!("{}id", property_class.to_lowercase().trim_start_matches("tc_").trim_end_matches('s'));
                let cond_key = format!("{}id", owner_class.to_lowercase().trim_start_matches("tc_").trim_end_matches('s'));
                params.push(serde_json::Value::Number((*owner_id).into()));
                format!("id IN (SELECT {} FROM {} WHERE {} = ?)", key, link_table, cond_key)
            } else if *property_id > 0 {
                let key = format!("{}id", owner_class.to_lowercase().trim_start_matches("tc_").trim_end_matches('s'));
                let cond_key = format!("{}id", property_class.to_lowercase().trim_start_matches("tc_").trim_end_matches('s'));
                params.push(serde_json::Value::Number((*property_id).into()));
                format!("id IN (SELECT {} FROM {} WHERE {} = ?)", key, link_table, cond_key)
            } else {
                "1=1".to_string()
            }
        }
        Condition::LatestPositions { device_id } => {
            if *device_id > 0 {
                params.push(serde_json::Value::Number((*device_id).into()));
                "id IN (SELECT positionId FROM tc_devices WHERE id = ?)".to_string()
            } else {
                "id IN (SELECT positionId FROM tc_devices)".to_string()
            }
        }
    }
}

pub fn format_order(order: &QueryOrder) -> String {
    let mut sql = format!(" ORDER BY {}", order.column);
    if order.descending {
        sql.push_str(" DESC");
    }
    if order.limit > 0 {
        sql.push_str(&format!(" LIMIT {}", order.limit));
        if order.offset > 0 {
            sql.push_str(&format!(" OFFSET {}", order.offset));
        }
    }
    sql
}

pub fn format_insert(table: &str, columns: &[String]) -> String {
    let placeholders: Vec<&str> = columns.iter().map(|_| "?").collect();
    format!(
        "INSERT INTO {} ({}) VALUES ({})",
        table,
        columns.join(", "),
        placeholders.join(", ")
    )
}

pub fn format_update(table: &str, columns: &[String]) -> String {
    let sets: Vec<String> = columns.iter().map(|c| format!("{} = ?", c)).collect();
    format!("UPDATE {} SET {}", table, sets.join(", "))
}

pub fn format_delete(table: &str) -> String {
    format!("DELETE FROM {}", table)
}
