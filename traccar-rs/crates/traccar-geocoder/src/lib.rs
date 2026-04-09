pub mod nominatim;
pub mod google;
pub mod bing;
pub mod locationiq;
pub mod opencage;
pub mod mapbox;
pub mod tomtom;
pub mod here;
pub mod geoapify;

use async_trait::async_trait;

#[derive(Debug, thiserror::Error)]
pub enum GeocoderError {
    #[error("Geocoder error: {0}")]
    Request(String),
    #[error("Parse error: {0}")]
    Parse(String),
}

#[async_trait]
pub trait Geocoder: Send + Sync {
    fn name(&self) -> &str;
    async fn reverse_geocode(&self, lat: f64, lon: f64) -> Result<String, GeocoderError>;
}

pub fn create_geocoder(geocoder_type: &str, url: Option<&str>, key: Option<&str>) -> Box<dyn Geocoder> {
    match geocoder_type {
        "nominatim" => Box::new(nominatim::NominatimGeocoder::new(url, key)),
        "google" => Box::new(google::GoogleGeocoder::new(key)),
        "bing" => Box::new(bing::BingGeocoder::new(key)),
        "locationiq" => Box::new(locationiq::LocationiqGeocoder::new(key)),
        "opencage" => Box::new(opencage::OpencageGeocoder::new(key)),
        "mapbox" => Box::new(mapbox::MapboxGeocoder::new(key)),
        "tomtom" => Box::new(tomtom::TomtomGeocoder::new(key)),
        "here" => Box::new(here::HereGeocoder::new(key)),
        "geoapify" => Box::new(geoapify::GeoapifyGeocoder::new(key)),
        _ => Box::new(nominatim::NominatimGeocoder::new(url, key)),
    }
}
