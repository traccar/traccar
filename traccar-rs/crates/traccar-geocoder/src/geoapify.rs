use async_trait::async_trait;

use crate::{Geocoder, GeocoderError};

pub struct GeoapifyGeocoder {
    key: String,
    client: reqwest::Client,
}

impl GeoapifyGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            key: key.unwrap_or_default().to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for GeoapifyGeocoder {
    fn name(&self) -> &str {
        "geoapify"
    }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        let url = format!(
            "https://api.geoapify.com/v1/geocode/reverse?lat={}&lon={}&apiKey={}",
            lat, lon, self.key
        );

        let resp: serde_json::Value = self.client.get(&url).send().await?.json().await?;

        resp.get("features")
            .and_then(|f| f.get(0))
            .and_then(|f| f.get("properties"))
            .and_then(|p| p.get("formatted"))
            .and_then(|v| v.as_str())
            .map(String::from)
            .ok_or_else(|| GeocoderError::Parse("No results from Geoapify".into()))
    }
}
