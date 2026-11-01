#pragma once
#include <algorithm>
#include <cmath>

namespace motionclue {

// ── Compile-time switches ──
constexpr bool USE_HW_GRAVITY = false;  // true = use hardware gravity/linear-accel for A/B tests

// ── Standard constants ──
constexpr float STANDARD_GRAVITY = 9.80665f;  // m/s^2

// ── Gravity estimator (stage 2) ──
constexpr float GRAVITY_TAU_S = 10.0f;          // complementary filter time constant
constexpr float GRAVITY_ACCEL_GATE = 0.8f;       // ||a| - g| threshold to reduce correction
constexpr float GRAVITY_GYRO_GATE = 1.0f;        // gyro magnitude gate (rad/s)
constexpr float GRAVITY_SETTLE_TIME_S = 2.0f;    // valid-correction time before settled flag

// ── Orientation reference (stage 3) ──
constexpr float ORIENT_REF_TAU_S = 30.0f;        // drift reference time constant

// ── Band-pass filter (stage 4) ──
constexpr float BP_LOW_HZ = 0.08f;               // high-pass corner
constexpr float BP_HIGH_HZ = 2.0f;               // low-pass corner

// ── View frame (stage 6) ──
constexpr float TILT_BLEND_LOW_DEG = 30.0f;      // start blending upright to flat
constexpr float TILT_BLEND_HIGH_DEG = 50.0f;     // fully flat above this

// ── Walking detector (stage 7) ──
constexpr float WALK_STEP_WINDOW_S = 2.0f;       // step counting window
constexpr int   WALK_MIN_STEPS = 2;               // minimum steps to trigger
constexpr float WALK_HOLD_TIME_S = 3.0f;          // hold walking state after last step
constexpr float WALK_SHIFT_GAIN = 0.3f;           // shift gain while walking
constexpr float WALK_FREQ_LOW_HZ = 1.5f;          // fallback band-energy low
constexpr float WALK_FREQ_HIGH_HZ = 2.5f;         // fallback band-energy high

// ── Stationary detector (stage 8) ──
constexpr float STAT_TIME_S = 3.0f;               // minimum stationary duration
constexpr float STAT_SPEED_THRESHOLD = 0.5f;      // m/s
constexpr float STAT_CLIMB_THRESHOLD = 0.3f;      // m/s climb rate

// ── Speed estimator (stage 11) ──
constexpr float GPS_STALE_S = 3.0f;              // GPS considered stale
constexpr float GPS_DEAD_S = 5.0f;                // GPS considered dead

// ── Dot motion model (stage 12) ──
constexpr float FLOW_FADE_TAU_S = 1.5f;          // flow start/stop fade

// ── Environment sensors (stage 9) ──
constexpr float PRESSURE_SMOOTH_S = 5.0f;         // pressure smoothing
constexpr float PRESSURE_CLIMB_WINDOW_S = 1.0f;   // climb rate window

// ── Rate measurement ──
constexpr int RATE_WINDOW_SAMPLES = 100;           // samples per rate window

// ── Sensor type constants (raw Android values, minSdk 26 safe) ──
constexpr int SENSOR_TYPE_ACCELEROMETER        = 1;
constexpr int SENSOR_TYPE_MAGNETIC_FIELD       = 2;
constexpr int SENSOR_TYPE_GYROSCOPE            = 4;
constexpr int SENSOR_TYPE_LIGHT                = 5;
constexpr int SENSOR_TYPE_PRESSURE             = 6;
constexpr int SENSOR_TYPE_PROXIMITY            = 8;
constexpr int SENSOR_TYPE_GRAVITY              = 9;
constexpr int SENSOR_TYPE_LINEAR_ACCELERATION  = 10;
constexpr int SENSOR_TYPE_ROTATION_VECTOR      = 11;
constexpr int SENSOR_TYPE_GAME_ROTATION_VECTOR = 15;
constexpr int SENSOR_TYPE_STEP_DETECTOR        = 18;

// ── sensorMask bit positions (contract v2) ──
constexpr int MASK_BIT_ACCEL         = 0;
constexpr int MASK_BIT_GYRO          = 1;
constexpr int MASK_BIT_MAG           = 2;
constexpr int MASK_BIT_GRAVITY_HW    = 3;
constexpr int MASK_BIT_LINEAR_HW     = 4;
constexpr int MASK_BIT_ROT_VEC       = 5;
constexpr int MASK_BIT_GAME_ROT_VEC  = 6;
constexpr int MASK_BIT_PRESSURE      = 7;
constexpr int MASK_BIT_LIGHT         = 8;
constexpr int MASK_BIT_PROXIMITY     = 9;
constexpr int MASK_BIT_STEP_DETECTOR = 10;

// ── Flag bit positions (contract v2) ──
constexpr int FLAG_SENSORS_RUNNING   = 0;
constexpr int FLAG_DIRECTION_LEARNED = 1;
constexpr int FLAG_SPEED_VALID       = 2;
constexpr int FLAG_SENSOR_ERROR      = 3;
constexpr int FLAG_GRAVITY_SETTLED   = 4;
constexpr int FLAG_STATIONARY        = 5;
constexpr int FLAG_WALKING           = 6;
constexpr int FLAG_PROXIMITY_NEAR    = 7;

// ── Slow sensor rate ──
constexpr int SLOW_SENSOR_PERIOD_US = 100000;  // 10 Hz

// ── Runtime config (mirrors the 12-float config array) ──
struct RuntimeConfig {
    float sensitivity     = 1.0f;    // [0] 0.2-3.0
    float hybridBlend     = 0.5f;    // [1] 0-1
    float springStiffness = 18.0f;   // [2]
    float springDamping   = 6.0f;    // [3]
    float referenceAccel  = 3.0f;    // [4] m/s^2
    float flowGain        = 0.5f;    // [5]
    float deadZone        = 0.15f;   // [6] m/s^2
    int   displayRotation = 0;       // [7] 0-3
    float flowFloorKmh    = 5.0f;    // [8]
    float flowCapUnits    = 4.0f;    // [9]
    bool  useMagnetometer = true;    // [10]
    float sampleRateHz    = 100.0f;  // [11] 50-200

    float accelWeight() const { return std::min(1.0f, 2.0f * (1.0f - hybridBlend)); }
    float flowWeight() const  { return std::min(1.0f, 2.0f * hybridBlend); }
    float flowFloorMps() const { return flowFloorKmh / 3.6f; }
    int   samplePeriodUs() const { return static_cast<int>(1000000.0f / sampleRateHz); }
};

} // namespace motionclue
