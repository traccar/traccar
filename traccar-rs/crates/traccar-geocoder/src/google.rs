use async_trait::async_trait;

use crate::{Geocoder, GeocoderError};

pub struct GoogleGeocoder {
    key: String,
    client: reqwest::Client,
}

impl GoogleGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            key: key.unwrap_or_default().to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for GoogleGeocoder {
    fn name(&self) -> &str {
        "google"
    }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        let url = format!(
            "https://maps.googleapis.com/maps/api/geocode/json?latlng={},{}&key={}",
            lat, lon, self.key
        );

        let resp: serde_json::Value = self.client.get(&url).send().await?.json().await?;

        resp.get("results")
            .and_then(|r| r.get(0))
            .and_then(|r| r.get("formatted_address"))
            .and_then(|v| v.as_str())
            .map(String::from)
            .ok_or_else(|| GeocoderError::Parse("No results from Google".into()))
    }
}
