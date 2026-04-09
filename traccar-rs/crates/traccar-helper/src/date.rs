use chrono::{DateTime, NaiveDate, NaiveDateTime, NaiveTime, TimeZone, Utc};

/// Build a DateTime<Utc> from date/time components.
pub fn make_datetime(
    year: i32,
    month: u32,
    day: u32,
    hour: u32,
    min: u32,
    sec: u32,
) -> Option<DateTime<Utc>> {
    let date = NaiveDate::from_ymd_opt(year, month, day)?;
    let time = NaiveTime::from_hms_opt(hour, min, sec)?;
    let naive = NaiveDateTime::new(date, time);
    Some(Utc.from_utc_datetime(&naive))
}

/// Build DateTime<Utc> from date/time components with milliseconds.
pub fn make_datetime_ms(
    year: i32,
    month: u32,
    day: u32,
    hour: u32,
    min: u32,
    sec: u32,
    millis: u32,
) -> Option<DateTime<Utc>> {
    let date = NaiveDate::from_ymd_opt(year, month, day)?;
    let time = NaiveTime::from_hms_milli_opt(hour, min, sec, millis)?;
    let naive = NaiveDateTime::new(date, time);
    Some(Utc.from_utc_datetime(&naive))
}

/// Parse a Unix timestamp in seconds to DateTime<Utc>.
pub fn from_unix_secs(secs: i64) -> DateTime<Utc> {
    DateTime::from_timestamp(secs, 0).unwrap_or_else(Utc::now)
}

/// Parse a Unix timestamp in milliseconds to DateTime<Utc>.
pub fn from_unix_millis(millis: i64) -> DateTime<Utc> {
    DateTime::from_timestamp_millis(millis).unwrap_or_else(Utc::now)
}

/// Correct 2-digit year to 4-digit year.
pub fn correct_year(year: i32) -> i32 {
    if year < 100 {
        2000 + year
    } else {
        year
    }
}
