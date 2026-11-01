# Architecture

MotionClue is a hybrid Kotlin + C++ Android app. The sensor processing runs in native code for low latency; the overlay and UI are in Kotlin.

## Module layout

```
app/
├── src/main/
│   ├── cpp/                          # Native C++ engine
│   │   ├── engine/                   # All processing stages
│   │   │   ├── MotionEngine.{h,cpp}  # Top-level orchestrator
│   │   │   ├── SensorInventory.{h,cpp}
│   │   │   ├── SensorSource.{h,cpp}  # ASensorEventQueue thread
│   │   │   ├── SharedFrame.{h,cpp}   # Seqlock buffer writer
│   │   │   ├── GravityEstimator.{h,cpp}
│   │   │   ├── OrientationReference.{h,cpp}
│   │   │   ├── BandPass.h            # IIR bandpass filter
│   │   │   ├── Config.h              # RuntimeConfig struct
│   │   │   ├── Vec3.h                # Minimal vec3 math
│   │   │   ├── WalkingDetector.{h,cpp}
│   │   │   ├── StationaryDetector.{h,cpp}
│   │   │   ├── EnvSensors.{h,cpp}    # Pressure, light, proximity
│   │   │   ├── SpeedEstimator.{h,cpp}
│   │   │   └── DotMotionModel.{h,cpp}# Shift + flow output
│   │   ├── jni_bridge.cpp            # JNI entry points
│   │   └── CMakeLists.txt
│   ├── java/com/sniperoftdm/motionclueandro/
│   │   ├── bridge/                   # Kotlin-to-native boundary
│   │   │   ├── MotionEngine.kt       # Interface
│   │   │   ├── NativeMotionEngine.kt # Real JNI implementation
│   │   │   ├── StubMotionEngine.kt   # Fallback (no native lib)
│   │   │   ├── MotionEngineFactory.kt# Auto-selects native or stub
│   │   │   ├── FrameBuffer.kt        # Seqlock reader
│   │   │   ├── MotionFrame.kt        # Per-frame data holder
│   │   │   └── EngineConfig.kt       # Config array builder
│   │   ├── service/
│   │   │   ├── MotionService.kt      # Foreground service
│   │   │   ├── GpsSpeedSource.kt     # GPS fix provider
│   │   │   └── SpeedSource.kt        # Interface
│   │   ├── overlay/
│   │   │   ├── DotsOverlayView.kt    # Custom View, Choreographer
│   │   │   ├── DotLayout.kt          # Dot position math
│   │   │   └── OverlayController.kt  # Window manager lifecycle
│   │   ├── tile/
│   │   │   └── MotionTileService.kt  # Quick Settings tile
│   │   ├── data/
│   │   │   └── SettingsStore.kt      # DataStore preferences
│   │   ├── ui/                       # Compose UI
│   │   │   ├── MainActivity.kt
│   │   │   ├── MainScreen.kt
│   │   │   ├── SettingsSection.kt
│   │   │   ├── SetupCard.kt
│   │   │   ├── BatterySection.kt
│   │   │   └── Theme.kt
│   │   └── MotionClueApp.kt          # Application, manual DI
│   ├── res/                          # Resources, icons, strings
│   └── AndroidManifest.xml
docs/
├── native_contract_v2.md             # Full native contract spec
├── TESTING.md                        # Manual test checklist
└── media/                            # Demo GIF and screenshots
```

## Kotlin / C++ boundary

The two sides communicate through exactly two channels:

1. **JNI calls** (Kotlin calls C++): the eight methods defined in [native_contract_v2.md](native_contract_v2.md). Kotlin never passes complex objects. Arguments are primitives, a float array (config), or a direct ByteBuffer (shared buffer).

2. **Shared buffer** (C++ writes, Kotlin reads): a 128-byte direct ByteBuffer allocated by Kotlin, passed to `nativeCreate`. Native code writes sensor outputs into it via `memcpy`. Kotlin reads it using absolute-offset getters. The seqlock protocol ensures torn reads are detected and retried.

Native code never calls back into Kotlin. This avoids JNI overhead on the sensor thread hot path.

For full details, see [docs/native_contract_v2.md](native_contract_v2.md).

## Per-frame data flow

```
1. ASensorEvent arrives on the native sensor thread
2. MotionEngine::onSensorEvent dispatches by sensor type
3. processAccelEvent is the main path:
   a. GravityEstimator subtracts gravity from raw accelerometer
   b. OrientationReference rotates linear accel into view frame
   c. BandPass filters noise
   d. DotMotionModel applies spring-damped shift
   e. SpeedEstimator fuses GPS fix with accel (if available)
   f. DotMotionModel computes flow direction and rate
   g. SharedFrame::write publishes the FrameData via seqlock
4. On the UI thread, Choreographer fires DotsOverlayView::doFrame
5. FrameBuffer::read does a seqlock read of the shared buffer
6. DotLayout::compute positions dots based on shift and flow
7. DotsOverlayView::onDraw renders dots to the overlay Canvas
```

## Threading rules

| Thread | Owns | Must not do |
|--------|------|-------------|
| Sensor thread (native) | SensorSource looper, MotionEngine::onSensorEvent, SharedFrame writes | JNI callbacks, allocations, blocking calls, logging per sample |
| Main thread (Kotlin) | Service lifecycle, GPS callbacks, settings flow | Direct sensor reads, long computations |
| UI thread (Kotlin) | Choreographer callbacks, Canvas draw | JNI calls, blocking on sensor data |

The config mutex is the only lock shared between threads (main thread writes config, sensor thread reads it). The shared buffer uses a seqlock instead of a mutex.

## Seqlock protocol

The 128-byte shared buffer uses a simple seqlock for lock-free producer-consumer:

- **Writer (native, sensor thread)**: increments sequence to odd, writes all fields via memcpy, increments sequence to even.
- **Reader (Kotlin, UI thread)**: reads sequence `s1`; if odd, retries. Copies all fields. Reads sequence `s2`. Accepts only if `s1 == s2`. After 3 failed attempts, reuses the previous frame.

This avoids both mutex contention and JNI overhead on the hot path.

## Why native

- Sensor callbacks at 100 Hz must execute fast with zero allocation pressure
- The JVM garbage collector can cause frame drops if allocations happen per sample
- Direct ASensorManager access avoids the overhead of the Java SensorManager wrapper
- The seqlock shared buffer is simpler and lower-overhead in C than in Kotlin
- C++ gives deterministic timing for the spring model and filter stages

## Adding a new sensor

1. Add the sensor field to `SensorInventory.h` (pointer) and update `discover()` in `SensorInventory.cpp` to look it up
2. Register the sensor in `SensorSource.cpp` inside `registerSensors()`
3. Handle its events in `MotionEngine::onSensorEvent()` — dispatch by `ASensorEvent.type`
4. Add the output slot to `FrameData` in `SharedFrame.h` and update the `static_assert`
5. Write the field in `SharedFrame::write()`
6. Add the corresponding field and slot constant to `FrameBuffer.kt` and `MotionFrame.kt`
7. Read it in `FrameBuffer.read()` and use it in the overlay or UI
8. Update the sensorMask bit in the contract doc and `SensorInventory::discover()`
9. Update the sensor table in `README.md`
