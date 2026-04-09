use async_trait::async_trait;

use crate::{Geocoder, GeocoderError};

pub struct TomtomGeocoder {
    key: String,
    client: reqwest::Client,
}

impl TomtomGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            key: key.unwrap_or_default().to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for TomtomGeocoder {
    fn name(&self) -> &str {
        "tomtom"
    }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        let url = format!(
            "https://api.tomtom.com/search/2/reverseGeocode/{},{}.json?key={}",
            lat, lon, self.key
        );

        let resp: serde_json::Value = self.client.get(&url).send().await?.json().await?;

        resp.get("addresses")
            .and_then(|a| a.get(0))
            .and_then(|a| a.get("address"))
            .and_then(|a| a.get("freeformAddress"))
            .and_then(|v| v.as_str())
            .map(String::from)
            .ok_or_else(|| GeocoderError::Parse("No results from TomTom".into()))
    }
}
