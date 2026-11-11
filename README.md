# AfyaReport

AfyaReport is an offline-first Android platform for defining configurable health registers, validating register entries, and generating structured Ministry of Health reports.

## Project status

The repository currently contains the Android build foundation. Product features and register definitions will be designed and implemented incrementally.

AfyaReport starts with no preinstalled registers or patient datasets. Register definitions, reporting sessions, and output templates are separate concepts.

## Technology foundation

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Gradle Kotlin DSL
- Gradle version catalog
- Java 17 bytecode compatibility
- Android API 26 minimum; API 36 compile and target baseline

## Build

The GitHub Actions Android workflow is manually triggered from the Actions tab. It installs the configured Gradle version and builds the debug APK.

For local Android Studio development, open the repository as a Gradle project after installing a compatible Android Studio release, JDK 17, and Android SDK Platform 36.

## Design principles

- Offline-first operation
- No mandatory account or cloud dependency
- No longitudinal patient database
- Reporting sessions are independent workspaces
- Register definitions and output templates are versioned independently
- Official MOH source documents are authoritative for register fields, code keys, and indicators
