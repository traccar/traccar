use async_trait::async_trait;
use crate::{Geocoder, GeocoderError};

pub struct NominatimGeocoder {
    url: String,
    key: Option<String>,
    client: reqwest::Client,
}

impl NominatimGeocoder {
    pub fn new(url: Option<&str>, key: Option<&str>) -> Self {
        Self {
            url: url.unwrap_or("https://nominatim.openstreetmap.org/reverse").to_string(),
            key: key.map(String::from),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for NominatimGeocoder {
    fn name(&self) -> &str { "nominatim" }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        let url = format!("{}?format=json&lat={}&lon={}", self.url, lat, lon);
        tracing::debug!("Geocoding via Nominatim: {}", url);
        
        let response = self.client.get(&url)
            .header("User-Agent", "Traccar/1.0")
            .send()
            .await
            .map_err(|e| GeocoderError::Request(e.to_string()))?;
        
        let body: serde_json::Value = response.json()
            .await
            .map_err(|e| GeocoderError::Parse(e.to_string()))?;
        
        body.get("display_name")
            .and_then(|v| v.as_str())
            .map(String::from)
            .ok_or_else(|| GeocoderError::Parse("No display_name in response".into()))
    }
}
