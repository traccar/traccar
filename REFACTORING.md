# Refactoring Summary

This document describes the refactoring techniques applied to the Traccar codebase. Each refactoring was applied in a separate commit with clear, easy-to-understand commit messages and 10-minute intervals between commits.

## Commit History

| Time  | Commit message | Technique |
|-------|----------------|-----------|
| 10:00 | Extract method: move address resolution into resolveAddress() | Set I – Extract method |
| 10:10 | Extract class: add ReportAddressResolver for address resolution | Set II – Extract class |
| 10:20 | Move method: delegate address resolution to ReportAddressResolver | Set II – Move method |
| 10:30 | Introduce explaining variable: clarify conditions in calculateFuel | Set I – Introduce explaining variable |
| 10:40 | Decompose conditional: clarify bounds check in containsPoint | Set I – Decompose conditional |
| 10:50 | Rename variable: trips to isTripReport for clarity | Set I – Rename variable |
| 11:00 | Replace conditional with polymorphism: use TripStopReportCalculator | Set II – Replace conditional with polymorphism |
| 11:10 | Add refactoring documentation | Documentation |

## Set I – Techniques (4)

1. **Extract method** – Address-resolution logic was moved into a private `resolveAddress()` method and used in three places in ReportUtils.
2. **Rename variable** – The variable `trips` was renamed to `isTripReport` in slow and fast trip/stop methods for clarity.
3. **Decompose conditional** – In `GeofenceGeometry.containsPoint()`, the condition was split into named booleans: `boundsDoNotCrossDateLine`, `latitudeOutsideBounds`, `longitudeOutsideBounds`.
4. **Introduce explaining variable** – In `calculateFuel()`, conditions were clarified with variables: `hasFuelUsedInBoth`, `hasFuelInBoth`, `hasFuelLevelInBoth`, `deviceHasFuelCapacity`.

## Set II – Techniques (3)

1. **Extract class** – New class `ReportAddressResolver` holds address-resolution logic.
2. **Move method** – Address resolution was moved from ReportUtils into `ReportAddressResolver.resolveAddress()`.
3. **Replace conditional with polymorphism** – The if/else in `calculateTripOrStop` was replaced by the `TripStopReportCalculator` interface and two implementations.

## Files changed

- **Modified:** `ReportUtils.java`, `GeofenceGeometry.java`
- **Added:** `ReportAddressResolver.java`, `TripStopReportCalculator.java`, `TripReportItemCalculator.java`, `StopReportItemCalculator.java`
