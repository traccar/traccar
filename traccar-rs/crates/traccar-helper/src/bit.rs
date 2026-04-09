/// Read a specific bit from a value.
pub fn bit_check(value: i64, bit: u32) -> bool {
    (value >> bit) & 1 == 1
}

/// Set a specific bit in a value.
pub fn bit_set(value: i64, bit: u32) -> i64 {
    value | (1 << bit)
}

/// Clear a specific bit in a value.
pub fn bit_clear(value: i64, bit: u32) -> i64 {
    value & !(1 << bit)
}

/// Extract a range of bits from a value.
pub fn bit_range(value: i64, from: u32, to: u32) -> i64 {
    let mask = (1i64 << (to - from)) - 1;
    (value >> from) & mask
}

/// Convert between different byte-order representations.
pub fn swap_bytes_16(value: u16) -> u16 {
    value.swap_bytes()
}

pub fn swap_bytes_32(value: u32) -> u32 {
    value.swap_bytes()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_bit_check() {
        assert!(bit_check(0b1010, 1));
        assert!(!bit_check(0b1010, 0));
        assert!(bit_check(0b1010, 3));
    }

    #[test]
    fn test_bit_range() {
        assert_eq!(bit_range(0b11010110, 1, 4), 0b011); // bits 1..4 = 011
    }
}
