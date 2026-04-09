use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::Path;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    #[serde(default)]
    pub web: WebConfig,
    #[serde(default)]
    pub database: DatabaseConfig,
    #[serde(default)]
    pub logger: LoggerConfig,
    #[serde(default)]
    pub geocoder: GeocoderConfig,
    #[serde(default)]
    pub mail: MailConfig,
    #[serde(default)]
    pub sms: SmsConfig,
    #[serde(default)]
    pub media: MediaConfig,
    #[serde(default)]
    pub protocols: HashMap<String, ProtocolConfig>,
    #[serde(default)]
    pub server: ServerConfig,
    #[serde(default)]
    pub extra: HashMap<String, toml::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WebConfig {
    #[serde(default = "default_web_port")]
    pub port: u16,
    #[serde(default = "default_web_address")]
    pub address: String,
    pub path: Option<String>,
    #[serde(default)]
    pub debug: bool,
    #[serde(default)]
    pub console: bool,
}

fn default_web_port() -> u16 {
    8082
}
fn default_web_address() -> String {
    "0.0.0.0".to_string()
}

impl Default for WebConfig {
    fn default() -> Self {
        Self {
            port: default_web_port(),
            address: default_web_address(),
            path: None,
            debug: false,
            console: false,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DatabaseConfig {
    #[serde(default = "default_db_url")]
    pub url: String,
    pub user: Option<String>,
    pub password: Option<String>,
    #[serde(default)]
    pub save_empty: bool,
    #[serde(default = "default_max_connections")]
    pub max_connections: u32,
}

fn default_db_url() -> String {
    "sqlite:traccar.db".to_string()
}
fn default_max_connections() -> u32 {
    10
}

impl Default for DatabaseConfig {
    fn default() -> Self {
        Self {
            url: default_db_url(),
            user: None,
            password: None,
            save_empty: false,
            max_connections: default_max_connections(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct LoggerConfig {
    #[serde(default)]
    pub console: bool,
    #[serde(default)]
    pub queries: bool,
    #[serde(default)]
    pub full_stack_traces: bool,
    pub level: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct GeocoderConfig {
    #[serde(default)]
    pub enable: bool,
    #[serde(rename = "type")]
    pub geocoder_type: Option<String>,
    pub url: Option<String>,
    pub key: Option<String>,
    pub language: Option<String>,
    pub format: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct MailConfig {
    #[serde(default)]
    pub debug: bool,
    pub smtp_host: Option<String>,
    pub smtp_port: Option<u16>,
    pub smtp_user: Option<String>,
    pub smtp_password: Option<String>,
    pub smtp_from: Option<String>,
    #[serde(default)]
    pub smtp_ssl: bool,
    #[serde(default)]
    pub smtp_tls: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct SmsConfig {
    pub http_url: Option<String>,
    pub aws_region: Option<String>,
    pub aws_access: Option<String>,
    pub aws_secret: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct MediaConfig {
    pub path: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProtocolConfig {
    #[serde(default)]
    pub port: u16,
    pub address: Option<String>,
    #[serde(default)]
    pub ssl: bool,
    pub timeout: Option<u64>,
    pub devices: Option<Vec<String>>,
    pub interval: Option<u64>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct ServerConfig {
    #[serde(default)]
    pub registration: bool,
    #[serde(default)]
    pub readonly: bool,
    #[serde(default)]
    pub force_settings: bool,
    pub timeout: Option<u64>,
}

impl Config {
    pub fn load(path: &Path) -> Result<Self, Box<dyn std::error::Error>> {
        let content = std::fs::read_to_string(path)?;
        let config: Config = toml::from_str(&content)?;
        Ok(config)
    }

    pub fn protocol_port(&self, name: &str) -> Option<u16> {
        self.protocols.get(name).map(|p| p.port).filter(|p| *p > 0)
    }

    pub fn get_string(&self, key: &str) -> Option<String> {
        self.extra.get(key).and_then(|v| v.as_str()).map(String::from)
    }

    pub fn get_bool(&self, key: &str) -> bool {
        self.extra
            .get(key)
            .and_then(|v| v.as_bool())
            .unwrap_or(false)
    }

    pub fn get_integer(&self, key: &str) -> Option<i64> {
        self.extra.get(key).and_then(|v| v.as_integer())
    }
}
