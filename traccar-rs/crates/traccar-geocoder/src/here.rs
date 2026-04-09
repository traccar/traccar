use async_trait::async_trait;

use crate::{Geocoder, GeocoderError};

pub struct HereGeocoder {
    key: String,
    client: reqwest::Client,
}

impl HereGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            key: key.unwrap_or_default().to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for HereGeocoder {
    fn name(&self) -> &str {
        "here"
    }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        let url = format!(
            "https://revgeocode.search.hereapi.com/v1/revgeocode?at={},{}&apiKey={}",
            lat, lon, self.key
        );

        let resp: serde_json::Value = self.client.get(&url).send().await?.json().await?;

        resp.get("items")
            .and_then(|i| i.get(0))
            .and_then(|i| i.get("address"))
            .and_then(|a| a.get("label"))
            .and_then(|v| v.as_str())
            .map(String::from)
            .ok_or_else(|| GeocoderError::Parse("No results from HERE".into()))
    }
}
