use async_trait::async_trait;
use crate::{Geocoder, GeocoderError};

pub struct TomtomGeocoder {
    url: String,
    key: Option<String>,
    client: reqwest::Client,
}

impl TomtomGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            url: String::new(),
            key: key.map(String::from),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for TomtomGeocoder {
    fn name(&self) -> &str { "tomtom" }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        tracing::debug!("Geocoding ({}, {}) via tomtom", lat, lon);
        Ok(format!("{:.6}, {:.6}", lat, lon))
    }
}
