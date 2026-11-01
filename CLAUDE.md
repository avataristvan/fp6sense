# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
cd fp6sense
./gradlew assembleDebug        # Build debug APK
./gradlew installDebug         # Build and install on connected device
./gradlew test                 # Unit tests
./gradlew lint                 # Lint checks
./gradlew clean                # Clean build outputs
```

**Java home:** `/usr/lib/jvm/java-21-openjdk`  
**Android SDK:** `~/Android/Sdk`

## App Overview

**fp6sense** is a Fairphone 6 sensor visualization app. It provides live sensor dashboards for:

| Feature | Route | Sensors Used |
|---------|-------|--------------|
| GNSS / Location | `gnss` | `LocationManager`, `GnssStatus` |
| Motion | `motion` | Accelerometer + Gyroscope |
| Environment | `environment` | Barometer + Light |
| Magnetic & Compass | `magnetic` | Hall sensor |
| Proximity | `proximity` | Proximity + Environment (ambient light) |
| Simulated IR | `infrared` | Camera2 RAW + OpenGL ES 3.0 GPU renderer |
| Vital Signs | `vital` | SCG (seismocardiography via accelerometer) |
| Vibration Mic | `vibration` | Linear acceleration + Gyro → WAV file |

Package: `app.fp6sense`  
Min SDK: 26 · Target SDK: 34 · Compose + Material3

## Architecture

Clean Architecture per feature: `domain/` → `data/` → `presentation/`

**Sensor data pattern** (used by every feature except infrared):
1. `domain/repository/` — interface with `getSensorData(): Flow<T>` and `isSensorAvailable(): Boolean`
2. `data/repository/` — `callbackFlow { ... awaitClose { unregister } }` wrapping `SensorManager` or `LocationManager`
3. `presentation/ViewModel` — collects the Flow in `viewModelScope`, exposes `StateFlow`
4. `presentation/Screen` — `collectAsState()` to observe and render

**Base class:** `AndroidSensorRepository` (`core/data/`) provides shared `SensorManager` lazy init and a `getSensorValues(): Flow<FloatArray>` helper for simple single-sensor cases. Features that need multiple sensors (motion, vibration) bypass it and register multiple listeners manually.

**Dependency injection:** Manual only — `MainActivity` wires repositories into ViewModels via a generic `ViewModelFactory<T>`. No Hilt/Dagger.

**Navigation:** Single-activity, `NavHost` with string routes matching the table above.

## Infrared Feature

Unique implementation: no ViewModel or repository. `InfraredScreen` manages `RawCameraManager` (Camera2 API, RAW_SENSOR format) and `InfraredGpuRenderer` (OpenGL ES 3.0) directly via `remember`/`DisposableEffect`. The GPU renderer decodes 16-bit Bayer RAW frames and applies a false-colour heat-map in a GLSL fragment shader.

## Vibration Feature

`SensorWavRecorder` runs a dedicated recording thread that samples `LINEAR_ACCELERATION` (Z-axis → left channel) and `GYROSCOPE` (X-axis → right channel) at 44100 Hz into a stereo PCM WAV file stored in `context.cacheDir`. Signal processing uses logarithmic companding (`μ`-law style) before quantising to 16-bit.

## Permissions

Declared in `AndroidManifest.xml`: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `CAMERA`, `HIGH_SAMPLING_RATE_SENSORS`. Runtime permission requests are handled in the relevant Compose screens using `accompanist-permissions`.
