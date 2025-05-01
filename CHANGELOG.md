# Changelog

## Unreleased

### Added
- `alpha` extension for feature attribute builder
- PNG export

### Changed
- Features are now sealed. New `CustomFeature` is not drawn by default draw.
- avoid drawing features with VisibleAttribute false
- Move SVG export to `features` and make it usable for maps as well
- Kotlin 2.1

### Deprecated

### Removed

### Fixed
- Text measurement outside of screen errors
- Add alpha attribute comprehension for all standard features.
- Package name for SerializeableAttribute


### Security

## 0.3.0 - 2024-06-04

### Changed

- Package changed to `space.kscience`
- Kotlin 2.0

### Fixed

- Use of generated resources for Wasm
