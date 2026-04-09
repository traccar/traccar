use async_trait::async_trait;

use crate::{Geocoder, GeocoderError};

pub struct LocationiqGeocoder {
    key: String,
    client: reqwest::Client,
}

impl LocationiqGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            key: key.unwrap_or_default().to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for LocationiqGeocoder {
    fn name(&self) -> &str {
        "locationiq"
    }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        let url = format!(
            "https://us1.locationiq.com/v1/reverse.php?key={}&lat={}&lon={}&format=json",
            self.key, lat, lon
        );

        let resp: serde_json::Value = self.client.get(&url).send().await?.json().await?;

        resp.get("display_name")
            .and_then(|v| v.as_str())
            .map(String::from)
            .ok_or_else(|| GeocoderError::Parse("No display_name from LocationIQ".into()))
    }
}
