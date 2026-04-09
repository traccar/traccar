use async_trait::async_trait;

use crate::{Geocoder, GeocoderError};

pub struct BingGeocoder {
    key: String,
    client: reqwest::Client,
}

impl BingGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            key: key.unwrap_or_default().to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for BingGeocoder {
    fn name(&self) -> &str {
        "bing"
    }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        let url = format!(
            "https://dev.virtualearth.net/REST/v1/Locations/{},{}?key={}",
            lat, lon, self.key
        );

        let resp: serde_json::Value = self.client.get(&url).send().await?.json().await?;

        resp.get("resourceSets")
            .and_then(|rs| rs.get(0))
            .and_then(|rs| rs.get("resources"))
            .and_then(|r| r.get(0))
            .and_then(|r| r.get("name"))
            .and_then(|v| v.as_str())
            .map(String::from)
            .ok_or_else(|| GeocoderError::Parse("No results from Bing".into()))
    }
}
