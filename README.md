# Maps-kt

A Kotlin Multiplatform library for interactive maps and geospatial data visualization using Compose Multiplatform.

![](docs/images/Screenshot%202023-01-12%20110429.png)

## Overview

Maps-kt provides a comprehensive set of tools for working with maps, geospatial data, and cartographic projections in Kotlin. It offers a UI-agnostic core with Compose Multiplatform implementations, allowing you to create interactive maps with markers, layers, and custom visualizations across multiple platforms.

## Features

- **Multiplatform Support**: Works on JVM, JavaScript, Native, and WebAssembly platforms
- **Compose Integration**: Seamless integration with Compose Multiplatform for modern UI development
- **Map Projections**: Support for Mercator, Web Mercator, and other cartographic projections
- **Geospatial Data**: Tools for working with coordinates, distances, angles, and ellipsoid geometry
- **Tile Providers**: Integration with OpenStreetMap and other tile providers
- **GeoJSON Support**: Parse and visualize GeoJSON data
- **Path Optimization**: Trajectory and path optimization capabilities

## License

This project is licensed under the Apache 2.0 License.

## Modules


### [demo](demo)
>
> **Maturity**: EXPERIMENTAL

### [maps-kt-compose](maps-kt-compose)
> Compose-multiplaform implementation for web-mercator tiled maps
>
> **Maturity**: DEVELOPMENT
>
> **Features:**
> - [osm](maps-kt-compose/#) : OpenStreetMap tile provider.


### [maps-kt-core](maps-kt-core)
> Core cartography, UI-agnostic
>
> **Maturity**: DEVELOPMENT
>
> **Features:**
> - [angles and distances](maps-kt-core/#) : Type-safe angle and distance measurements.
> - [ellipsoid](maps-kt-core/#) : Ellipsoid geometry and distances
> - [mercator](maps-kt-core/#) : Mercator and web-mercator projections


### [maps-kt-features](maps-kt-features)
>
> **Maturity**: EXPERIMENTAL

### [maps-kt-geojson](maps-kt-geojson)
> GeoJson format support
>
> **Maturity**: DEVELOPMENT

### [maps-kt-geotools](maps-kt-geotools)
>
> **Maturity**: EXPERIMENTAL

### [maps-kt-scheme](maps-kt-scheme)
>
> **Maturity**: DEVELOPMENT

### [trajectory-kt](trajectory-kt)
> Path and trajectory optimization
>
> **Maturity**: EXPERIMENTAL

### [demo/maps](demo/maps)
>
> **Maturity**: EXPERIMENTAL

### [demo/maps-wasm](demo/maps-wasm)
>
> **Maturity**: EXPERIMENTAL

### [demo/polygon-editor](demo/polygon-editor)
>
> **Maturity**: EXPERIMENTAL

### [demo/scheme](demo/scheme)
>
> **Maturity**: EXPERIMENTAL

### [demo/trajectory-playground](demo/trajectory-playground)
>
> **Maturity**: EXPERIMENTAL

