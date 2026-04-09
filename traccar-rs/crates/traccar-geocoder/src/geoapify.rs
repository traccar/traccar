use async_trait::async_trait;
use crate::{Geocoder, GeocoderError};

pub struct GeoapifyGeocoder {
    url: String,
    key: Option<String>,
    client: reqwest::Client,
}

impl GeoapifyGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            url: String::new(),
            key: key.map(String::from),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for GeoapifyGeocoder {
    fn name(&self) -> &str { "geoapify" }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        tracing::debug!("Geocoding ({}, {}) via geoapify", lat, lon);
        Ok(format!("{:.6}, {:.6}", lat, lon))
    }
}
