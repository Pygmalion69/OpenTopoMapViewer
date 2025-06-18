# OpenTopoMapViewer Developer Guidelines

## Project Overview
OpenTopoMapViewer is an Android application for viewing OpenTopoMap with features like location tracking, GPX import, and hiking/cycling routes. The app is available in two flavors: a Google Play version (with ads) and a FOSS version (no ads).

## Tech Stack
- **Language**: Kotlin with Java interoperability
- **Min SDK**: 24
- **Target SDK**: 35
- **Build System**: Gradle
- **Key Libraries**:
  - OSMDroid (v6.1.18): Map functionality
  - Room (v2.7.1): Database operations
  - Retrofit (v2.11.0): Network requests
  - AndroidX components: UI and lifecycle management
  - GPX Parser: For handling GPX files

## Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/org/nitri/opentopo/
│   │   │   ├── adapter/      # RecyclerView/ListView adapters
│   │   │   ├── domain/       # Domain models and business logic
│   │   │   ├── model/        # Data models
│   │   │   ├── nearby/       # Nearby locations functionality
│   │   │   ├── overlay/      # Map overlays
│   │   │   ├── util/         # Utility classes
│   │   │   └── view/         # Custom views
│   │   └── res/              # Resources (layouts, strings, etc.)
│   ├── test/                 # Unit tests
│   └── androidTest/          # Instrumentation tests
├── foss/                     # FOSS flavor specific files
└── play/                     # Google Play flavor specific files
```

## Build Configuration
The project has two product flavors:
- **foss**: Free and open-source version without ads (for F-Droid)
- **play**: Google Play version with ads

## Building the Project
To build the project:
```bash
# Build debug version
./gradlew assembleDebug

# Build release version
./gradlew assembleRelease

# Build specific flavor
./gradlew assembleFossDebug
./gradlew assemblePlayRelease
```

## Running Tests
The project uses JUnit 4 for testing:
```bash
# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

## Best Practices
1. **Code Organization**:
   - Keep related functionality in the same package
   - Follow the existing package structure
   - Use Kotlin for new code when possible

2. **UI Development**:
   - Use AndroidX components
   - Follow Material Design guidelines
   - Support different screen sizes

3. **Map Functionality**:
   - Use OSMDroid for map operations
   - Add new overlays in the overlay package
   - Follow the existing pattern for map interactions

4. **Testing**:
   - Write unit tests for business logic
   - Use instrumentation tests for UI components
   - Test both FOSS and Play flavors

5. **Version Control**:
   - Write clear commit messages
   - Create feature branches for new functionality
   - Test thoroughly before merging

## Release Process
1. Update version code and name in app/build.gradle
2. Test both FOSS and Play flavors
3. Build release APKs
4. Test the release APKs
5. Create a release tag in Git
6. Publish to F-Droid and Google Play