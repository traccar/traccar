use regex::Regex;

/// Parser for GPS data strings, wrapping regex captures.
pub struct Parser {
    values: Vec<String>,
    index: usize,
}

impl Parser {
    /// Create a parser from a regex pattern and input string.
    /// Returns None if no match.
    pub fn new(pattern: &str, input: &str) -> Option<Self> {
        let re = Regex::new(pattern).ok()?;
        let caps = re.captures(input)?;
        let values: Vec<String> = caps
            .iter()
            .skip(1)
            .map(|m| m.map_or(String::new(), |m| m.as_str().to_string()))
            .collect();
        Some(Self { values, index: 0 })
    }

    pub fn has_next(&self) -> bool {
        self.index < self.values.len()
    }

    pub fn next_str(&mut self) -> Option<String> {
        if self.index < self.values.len() {
            let val = self.values[self.index].clone();
            self.index += 1;
            if val.is_empty() {
                None
            } else {
                Some(val)
            }
        } else {
            None
        }
    }

    pub fn next_int(&mut self, default: i64) -> i64 {
        self.next_str()
            .and_then(|s| s.parse().ok())
            .unwrap_or(default)
    }

    pub fn next_double(&mut self, default: f64) -> f64 {
        self.next_str()
            .and_then(|s| s.parse().ok())
            .unwrap_or(default)
    }

    pub fn next_hex_int(&mut self, default: i64) -> i64 {
        self.next_str()
            .and_then(|s| i64::from_str_radix(&s, 16).ok())
            .unwrap_or(default)
    }

    /// Parse coordinate in DDmm.mmmm format with hemisphere indicator
    pub fn next_coordinate(&mut self, hemisphere_key: &str) -> f64 {
        let raw = self.next_double(0.0);
        let degrees = (raw / 100.0).floor();
        let minutes = raw - degrees * 100.0;
        let mut result = degrees + minutes / 60.0;

        if let Some(h) = self.next_str() {
            if h == "S" || h == "W" || h == hemisphere_key {
                result = -result;
            }
        }
        result
    }

    pub fn skip(&mut self, count: usize) {
        self.index = (self.index + count).min(self.values.len());
    }
}

/// Convert BCD bytes to a string of decimal digits.
pub fn bcd_to_string(data: &[u8]) -> String {
    let mut result = String::with_capacity(data.len() * 2);
    for &b in data {
        result.push(char::from_digit(((b >> 4) & 0x0F) as u32, 10).unwrap_or('0'));
        result.push(char::from_digit((b & 0x0F) as u32, 10).unwrap_or('0'));
    }
    result
}

/// Convert hex string to bytes.
pub fn hex_to_bytes(hex: &str) -> Vec<u8> {
    hex::decode(hex).unwrap_or_default()
}

/// Convert bytes to hex string.
pub fn bytes_to_hex(data: &[u8]) -> String {
    hex::encode(data)
}
