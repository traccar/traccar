use bytes::BytesMut;
use std::io;
use tokio_util::codec::Decoder;

// ─── Delimiter frame decoder ────────────────────────────────────────

/// Splits incoming bytes by one or more delimiter sequences.
/// Similar to Netty's `DelimiterBasedFrameDecoder`.
pub struct DelimiterFrameDecoder {
    delimiters: Vec<Vec<u8>>,
    max_frame_length: usize,
    strip_delimiter: bool,
}

impl DelimiterFrameDecoder {
    pub fn new(max_frame_length: usize, strip_delimiter: bool, delimiters: Vec<Vec<u8>>) -> Self {
        Self {
            delimiters,
            max_frame_length,
            strip_delimiter,
        }
    }

    /// Convenience: create a decoder splitting by multiple string delimiters.
    pub fn from_strings(max_frame_length: usize, strip_delimiter: bool, delimiters: &[&str]) -> Self {
        let delimiters = delimiters.iter().map(|d| d.as_bytes().to_vec()).collect();
        Self::new(max_frame_length, strip_delimiter, delimiters)
    }

    fn find_delimiter(&self, buf: &BytesMut) -> Option<(usize, usize)> {
        let mut earliest: Option<(usize, usize)> = None;

        for delimiter in &self.delimiters {
            if delimiter.is_empty() {
                continue;
            }
            for i in 0..buf.len() {
                if i + delimiter.len() <= buf.len() && &buf[i..i + delimiter.len()] == delimiter.as_slice() {
                    match earliest {
                        None => earliest = Some((i, delimiter.len())),
                        Some((prev_pos, _)) if i < prev_pos => {
                            earliest = Some((i, delimiter.len()));
                        }
                        _ => {}
                    }
                    break; // found first occurrence for this delimiter
                }
            }
        }

        earliest
    }
}

impl Decoder for DelimiterFrameDecoder {
    type Item = BytesMut;
    type Error = io::Error;

    fn decode(&mut self, buf: &mut BytesMut) -> Result<Option<BytesMut>, io::Error> {
        if let Some((pos, delim_len)) = self.find_delimiter(buf) {
            if pos > self.max_frame_length {
                buf.split_to(pos + delim_len);
                return Err(io::Error::new(
                    io::ErrorKind::InvalidData,
                    "Frame exceeds maximum length",
                ));
            }

            if self.strip_delimiter {
                let frame = buf.split_to(pos);
                buf.split_to(delim_len); // discard delimiter
                Ok(Some(frame))
            } else {
                let frame = buf.split_to(pos + delim_len);
                Ok(Some(frame))
            }
        } else if buf.len() > self.max_frame_length {
            buf.clear();
            Err(io::Error::new(
                io::ErrorKind::InvalidData,
                "Frame exceeds maximum length without delimiter",
            ))
        } else {
            Ok(None)
        }
    }
}

// ─── Length-field frame decoder ─────────────────────────────────────

/// Reads a length prefix from the stream and then reads that many bytes as the frame.
/// Configurable offset, field size, and adjustment.
pub struct LengthFieldFrameDecoder {
    /// Byte offset of the length field from the start of the message.
    pub length_field_offset: usize,
    /// Size of the length field in bytes (1, 2, or 4).
    pub length_field_length: usize,
    /// Value to add to the length field value (can be negative for header-inclusive lengths).
    pub length_adjustment: i32,
    /// Number of bytes to strip from the beginning of the decoded frame (e.g., to strip the header).
    pub initial_bytes_to_strip: usize,
    /// Maximum frame length.
    pub max_frame_length: usize,
}

impl LengthFieldFrameDecoder {
    pub fn new(
        max_frame_length: usize,
        length_field_offset: usize,
        length_field_length: usize,
        length_adjustment: i32,
        initial_bytes_to_strip: usize,
    ) -> Self {
        Self {
            length_field_offset,
            length_field_length,
            length_adjustment,
            initial_bytes_to_strip,
            max_frame_length,
        }
    }

    fn read_length(&self, buf: &BytesMut) -> Option<usize> {
        let offset = self.length_field_offset;
        if buf.len() < offset + self.length_field_length {
            return None;
        }

        let raw_length = match self.length_field_length {
            1 => buf[offset] as u64,
            2 => u16::from_be_bytes([buf[offset], buf[offset + 1]]) as u64,
            4 => u32::from_be_bytes([
                buf[offset],
                buf[offset + 1],
                buf[offset + 2],
                buf[offset + 3],
            ]) as u64,
            _ => return None,
        };

        let adjusted = raw_length as i64 + self.length_adjustment as i64;
        if adjusted < 0 {
            return None;
        }

        Some(adjusted as usize)
    }
}

impl Decoder for LengthFieldFrameDecoder {
    type Item = BytesMut;
    type Error = io::Error;

    fn decode(&mut self, buf: &mut BytesMut) -> Result<Option<BytesMut>, io::Error> {
        let total_header = self.length_field_offset + self.length_field_length;

        if buf.len() < total_header {
            return Ok(None);
        }

        let data_length = match self.read_length(buf) {
            Some(len) => len,
            None => return Ok(None),
        };

        let frame_length = total_header + data_length;

        if frame_length > self.max_frame_length {
            buf.split_to(buf.len().min(frame_length));
            return Err(io::Error::new(
                io::ErrorKind::InvalidData,
                "Frame exceeds maximum length",
            ));
        }

        if buf.len() < frame_length {
            return Ok(None);
        }

        let mut frame = buf.split_to(frame_length);
        if self.initial_bytes_to_strip > 0 && self.initial_bytes_to_strip <= frame.len() {
            let _ = frame.split_to(self.initial_bytes_to_strip);
        }

        Ok(Some(frame))
    }
}

// ─── Fixed-length frame decoder ─────────────────────────────────────

/// Reads frames of a fixed size.
pub struct FixedLengthFrameDecoder {
    frame_length: usize,
}

impl FixedLengthFrameDecoder {
    pub fn new(frame_length: usize) -> Self {
        Self { frame_length }
    }
}

impl Decoder for FixedLengthFrameDecoder {
    type Item = BytesMut;
    type Error = io::Error;

    fn decode(&mut self, buf: &mut BytesMut) -> Result<Option<BytesMut>, io::Error> {
        if buf.len() >= self.frame_length {
            Ok(Some(buf.split_to(self.frame_length)))
        } else {
            Ok(None)
        }
    }
}

// ─── Line-based frame decoder ───────────────────────────────────────

/// Splits incoming data on newlines (`\n` or `\r\n`), stripping the line ending.
pub struct LineBasedFrameDecoder {
    max_line_length: usize,
}

impl LineBasedFrameDecoder {
    pub fn new(max_line_length: usize) -> Self {
        Self { max_line_length }
    }
}

impl Decoder for LineBasedFrameDecoder {
    type Item = BytesMut;
    type Error = io::Error;

    fn decode(&mut self, buf: &mut BytesMut) -> Result<Option<BytesMut>, io::Error> {
        // Find first \n
        if let Some(newline_pos) = buf.iter().position(|&b| b == b'\n') {
            if newline_pos > self.max_line_length {
                buf.split_to(newline_pos + 1);
                return Err(io::Error::new(
                    io::ErrorKind::InvalidData,
                    "Line exceeds maximum length",
                ));
            }

            let mut frame = buf.split_to(newline_pos + 1);

            // Strip the trailing \n
            frame.truncate(frame.len() - 1);

            // Strip trailing \r if present
            if !frame.is_empty() && frame[frame.len() - 1] == b'\r' {
                frame.truncate(frame.len() - 1);
            }

            Ok(Some(frame))
        } else if buf.len() > self.max_line_length {
            buf.clear();
            Err(io::Error::new(
                io::ErrorKind::InvalidData,
                "Line exceeds maximum length without newline",
            ))
        } else {
            Ok(None)
        }
    }
}

// ─── Implement FrameDecoder trait ───────────────────────────────────

impl crate::FrameDecoder for DelimiterFrameDecoder {}
impl crate::FrameDecoder for LengthFieldFrameDecoder {}
impl crate::FrameDecoder for FixedLengthFrameDecoder {}
impl crate::FrameDecoder for LineBasedFrameDecoder {}

#[cfg(test)]
mod tests {
    use super::*;
    use bytes::BufMut;

    #[test]
    fn test_delimiter_decoder() {
        let mut decoder = DelimiterFrameDecoder::from_strings(1024, true, &["\r\n", ";"]);
        let mut buf = BytesMut::new();
        buf.put_slice(b"hello\r\nworld;");

        let frame1 = decoder.decode(&mut buf).unwrap().unwrap();
        assert_eq!(&frame1[..], b"hello");

        let frame2 = decoder.decode(&mut buf).unwrap().unwrap();
        assert_eq!(&frame2[..], b"world");
    }

    #[test]
    fn test_delimiter_decoder_incomplete() {
        let mut decoder = DelimiterFrameDecoder::from_strings(1024, true, &["\r\n"]);
        let mut buf = BytesMut::new();
        buf.put_slice(b"hello");

        assert!(decoder.decode(&mut buf).unwrap().is_none());
    }

    #[test]
    fn test_length_field_decoder() {
        let mut decoder = LengthFieldFrameDecoder::new(1024, 0, 2, 0, 2);
        let mut buf = BytesMut::new();
        buf.put_u16(5); // length = 5
        buf.put_slice(b"hello");

        let frame = decoder.decode(&mut buf).unwrap().unwrap();
        assert_eq!(&frame[..], b"hello");
    }

    #[test]
    fn test_fixed_length_decoder() {
        let mut decoder = FixedLengthFrameDecoder::new(4);
        let mut buf = BytesMut::new();
        buf.put_slice(b"abcdef");

        let frame = decoder.decode(&mut buf).unwrap().unwrap();
        assert_eq!(&frame[..], b"abcd");
        assert_eq!(buf.len(), 2);
    }

    #[test]
    fn test_line_based_decoder() {
        let mut decoder = LineBasedFrameDecoder::new(1024);
        let mut buf = BytesMut::new();
        buf.put_slice(b"line1\r\nline2\n");

        let frame1 = decoder.decode(&mut buf).unwrap().unwrap();
        assert_eq!(&frame1[..], b"line1");

        let frame2 = decoder.decode(&mut buf).unwrap().unwrap();
        assert_eq!(&frame2[..], b"line2");
    }
}
