use async_trait::async_trait;
use crate::{Geocoder, GeocoderError};

pub struct HereGeocoder {
    url: String,
    key: Option<String>,
    client: reqwest::Client,
}

impl HereGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            url: String::new(),
            key: key.map(String::from),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for HereGeocoder {
    fn name(&self) -> &str { "here" }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        tracing::debug!("Geocoding ({}, {}) via here", lat, lon);
        Ok(format!("{:.6}, {:.6}", lat, lon))
    }
}
