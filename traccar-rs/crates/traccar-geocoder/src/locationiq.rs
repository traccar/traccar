use async_trait::async_trait;
use crate::{Geocoder, GeocoderError};

pub struct LocationiqGeocoder {
    url: String,
    key: Option<String>,
    client: reqwest::Client,
}

impl LocationiqGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            url: String::new(),
            key: key.map(String::from),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for LocationiqGeocoder {
    fn name(&self) -> &str { "locationiq" }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        tracing::debug!("Geocoding ({}, {}) via locationiq", lat, lon);
        Ok(format!("{:.6}, {:.6}", lat, lon))
    }
}
