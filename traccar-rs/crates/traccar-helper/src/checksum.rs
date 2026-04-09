/// Compute CRC-16/CCITT-FALSE
pub fn crc16_ccitt(data: &[u8]) -> u16 {
    let mut crc: u16 = 0xFFFF;
    for &byte in data {
        crc ^= (byte as u16) << 8;
        for _ in 0..8 {
            if crc & 0x8000 != 0 {
                crc = (crc << 1) ^ 0x1021;
            } else {
                crc <<= 1;
            }
        }
    }
    crc
}

/// Compute CRC-16 (X.25 / ISO 14443)
pub fn crc16_x25(data: &[u8]) -> u16 {
    let mut crc: u16 = 0xFFFF;
    for &byte in data {
        crc ^= byte as u16;
        for _ in 0..8 {
            if crc & 1 != 0 {
                crc = (crc >> 1) ^ 0x8408;
            } else {
                crc >>= 1;
            }
        }
    }
    crc ^ 0xFFFF
}

/// Compute simple XOR checksum
pub fn xor_checksum(data: &[u8]) -> u8 {
    data.iter().fold(0u8, |acc, &b| acc ^ b)
}

/// Compute Luhn checksum
pub fn luhn(number: u64) -> u8 {
    let mut sum = 0u32;
    let mut alt = false;
    let digits: Vec<u32> = format!("{}", number)
        .chars()
        .map(|c| c.to_digit(10).unwrap())
        .collect();
    for &d in digits.iter().rev() {
        let mut n = d;
        if alt {
            n *= 2;
            if n > 9 {
                n -= 9;
            }
        }
        sum += n;
        alt = !alt;
    }
    ((10 - (sum % 10)) % 10) as u8
}

/// Compute modulus-256 sum
pub fn sum_checksum(data: &[u8]) -> u8 {
    data.iter().fold(0u8, |acc, &b| acc.wrapping_add(b))
}

/// Compute CRC-8
pub fn crc8(data: &[u8]) -> u8 {
    let mut crc: u8 = 0;
    for &byte in data {
        crc ^= byte;
        for _ in 0..8 {
            if crc & 0x80 != 0 {
                crc = (crc << 1) ^ 0x07;
            } else {
                crc <<= 1;
            }
        }
    }
    crc
}

/// Compute CRC-32
pub fn crc32(data: &[u8]) -> u32 {
    let mut crc: u32 = 0xFFFFFFFF;
    for &byte in data {
        crc ^= byte as u32;
        for _ in 0..8 {
            if crc & 1 != 0 {
                crc = (crc >> 1) ^ 0xEDB88320;
            } else {
                crc >>= 1;
            }
        }
    }
    crc ^ 0xFFFFFFFF
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_xor() {
        assert_eq!(xor_checksum(&[0x01, 0x02, 0x03]), 0x00);
        assert_eq!(xor_checksum(&[0xFF, 0x00]), 0xFF);
    }

    #[test]
    fn test_crc16_ccitt() {
        let data = b"123456789";
        assert_eq!(crc16_ccitt(data), 0x29B1);
    }

    #[test]
    fn test_sum() {
        assert_eq!(sum_checksum(&[1, 2, 3, 4]), 10);
    }
}
