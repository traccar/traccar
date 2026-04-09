use async_trait::async_trait;

use crate::{Geocoder, GeocoderError};

pub struct MapboxGeocoder {
    key: String,
    client: reqwest::Client,
}

impl MapboxGeocoder {
    pub fn new(key: Option<&str>) -> Self {
        Self {
            key: key.unwrap_or_default().to_string(),
            client: reqwest::Client::new(),
        }
    }
}

#[async_trait]
impl Geocoder for MapboxGeocoder {
    fn name(&self) -> &str {
        "mapbox"
    }

    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError> {
        let url = format!(
            "https://api.mapbox.com/geocoding/v5/mapbox.places/{},{}.json?access_token={}",
            lon, lat, self.key
        );

        let resp: serde_json::Value = self.client.get(&url).send().await?.json().await?;

        resp.get("features")
            .and_then(|f| f.get(0))
            .and_then(|f| f.get("place_name"))
            .and_then(|v| v.as_str())
            .map(String::from)
            .ok_or_else(|| GeocoderError::Parse("No results from Mapbox".into()))
    }
}
