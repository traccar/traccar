/// Speed conversion utilities.
/// Internal unit is knots (nautical miles per hour).

pub fn knots_from_kph(kph: f64) -> f64 {
    kph * 0.539957
}

pub fn knots_from_mph(mph: f64) -> f64 {
    mph * 0.868976
}

pub fn knots_from_mps(mps: f64) -> f64 {
    mps * 1.943844
}

pub fn kph_from_knots(knots: f64) -> f64 {
    knots * 1.852
}

pub fn mph_from_knots(knots: f64) -> f64 {
    knots * 1.150779
}

pub fn mps_from_knots(knots: f64) -> f64 {
    knots * 0.514444
}

/// Distance conversion utilities.

pub fn km_from_meters(meters: f64) -> f64 {
    meters / 1000.0
}

pub fn miles_from_meters(meters: f64) -> f64 {
    meters * 0.000621371
}

/// Calculate distance between two points using Haversine formula (in meters).
pub fn haversine_distance(lat1: f64, lon1: f64, lat2: f64, lon2: f64) -> f64 {
    let r = 6371000.0; // Earth radius in meters
    let d_lat = (lat2 - lat1).to_radians();
    let d_lon = (lon2 - lon1).to_radians();
    let a = (d_lat / 2.0).sin().powi(2)
        + lat1.to_radians().cos() * lat2.to_radians().cos() * (d_lon / 2.0).sin().powi(2);
    let c = 2.0 * a.sqrt().atan2((1.0 - a).sqrt());
    r * c
}

/// Calculate course (bearing) between two points in degrees.
pub fn course_between(lat1: f64, lon1: f64, lat2: f64, lon2: f64) -> f64 {
    let lat1 = lat1.to_radians();
    let lat2 = lat2.to_radians();
    let d_lon = (lon2 - lon1).to_radians();
    let y = d_lon.sin() * lat2.cos();
    let x = lat1.cos() * lat2.sin() - lat1.sin() * lat2.cos() * d_lon.cos();
    let bearing = y.atan2(x).to_degrees();
    (bearing + 360.0) % 360.0
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_speed_conversions() {
        let kph = 100.0;
        let knots = knots_from_kph(kph);
        let back = kph_from_knots(knots);
        assert!((back - kph).abs() < 0.01);
    }

    #[test]
    fn test_haversine() {
        // New York to London approx 5570 km
        let d = haversine_distance(40.7128, -74.0060, 51.5074, -0.1278);
        assert!((d / 1000.0 - 5570.0).abs() < 50.0);
    }
}
