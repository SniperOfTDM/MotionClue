// app/src/main/cpp/engine/MotionEngine.cpp
#include "MotionEngine.h"
#include "SensorSource.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define LOG_TAG "MotionEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace motionclue {

// ── Construction / destruction ──

MotionEngine::MotionEngine() {
    sensorManager_ = ASensorManager_getInstance();
    if (sensorManager_) {
        inventory_.discover(sensorManager_);
        LOGI("Sensor discovery: mask=0x%x", inventory_.sensorMask);
    } else {
        LOGE("ASensorManager_getInstance returned null");
    }
}

MotionEngine::~MotionEngine() {
    stop();
    sharedFrame_.disable();
}

// ── Init ──

bool MotionEngine::init(uint8_t* buffer, size_t capacity) {
    return sharedFrame_.init(buffer, capacity);
}

// ── Start / stop ──

bool MotionEngine::start() {
    if (started_.load()) return true;  // idempotent

    if (!inventory_.hasAccelerometer()) {
        LOGE("No accelerometer");
        frame_.flags |= (1 << FLAG_SENSOR_ERROR);
        sharedFrame_.write(frame_);
        return false;
    }
    if (!sensorManager_) {
        frame_.flags |= (1 << FLAG_SENSOR_ERROR);
        sharedFrame_.write(frame_);
        return false;
    }

    RuntimeConfig cfg;
    {
        std::lock_guard<std::mutex> lock(configMutex_);
        cfg = config_;
    }

    // Reset sensor-thread state for fresh start
    liveSensorMask_       = 0;
    rateWindowStartNs_    = 0;
    rateWindowCount_      = 0;
    lastAccelTimestampNs_ = 0;
    droppedSamples_       = 0;
    frame_.measuredRateHz = 0.0f;
    frame_.droppedSamples = 0;

    sensorSource_ = std::make_unique<SensorSource>(sensorManager_, inventory_, *this);
    if (!sensorSource_->start(cfg.samplePeriodUs())) {
        LOGE("SensorSource start failed");
        frame_.flags |= (1 << FLAG_SENSOR_ERROR);
        sharedFrame_.write(frame_);
        return false;
    }

    started_.store(true);
    frame_.flags |= (1 << FLAG_SENSORS_RUNNING);
    return true;
}

void MotionEngine::stop() {
    if (!started_.exchange(false)) return;

    if (sensorSource_) {
        sensorSource_->stop();   // blocks until sensor thread exits
        sensorSource_.reset();
    }

    frame_.flags &= ~(1 << FLAG_SENSORS_RUNNING);
    sharedFrame_.write(frame_);
}

// ── Control calls (may come from any thread) ──

void MotionEngine::setFix(float speedMps, float speedAccMps,
                           float bearingDeg, float bearingAccDeg,
                           int64_t elapsedRealtimeNanos) {
    std::lock_guard<std::mutex> lock(fixMutex_);
    latestFix_.speedMps      = speedMps;
    latestFix_.speedAccMps   = speedAccMps;
    latestFix_.bearingDeg    = bearingDeg;
    latestFix_.bearingAccDeg = bearingAccDeg;
    latestFix_.timestamp     = elapsedRealtimeNanos;
}

void MotionEngine::setConfig(const float* params, int length) {
    if (!params || length < 12) return;
    std::lock_guard<std::mutex> lock(configMutex_);
    config_.sensitivity     = std::clamp(params[0], 0.2f, 3.0f);
    config_.hybridBlend     = std::clamp(params[1], 0.0f, 1.0f);
    config_.springStiffness = params[2];
    config_.springDamping   = params[3];
    config_.referenceAccel  = params[4];
    config_.flowGain        = params[5];
    config_.deadZone        = params[6];
    config_.displayRotation = static_cast<int>(params[7]);
    config_.flowFloorKmh    = params[8];
    config_.flowCapUnits    = params[9];
    config_.useMagnetometer = params[10] > 0.5f;
    config_.sampleRateHz    = std::clamp(params[11], 50.0f, 200.0f);
}

void MotionEngine::resetCalibration() {
    // Hand to sensor thread via atomic flag (never hold a lock the callback waits on)
    resetPending_.store(true, std::memory_order_relaxed);
}

std::string MotionEngine::describeSensors() const {
    return inventory_.describeText;
}

// ── Sensor callback (sensor thread only) ──

void MotionEngine::onSensorEvent(const ASensorEvent& event) {
    // No allocation, no JNI, no blocking calls.
    const int type = event.type;

    switch (type) {
        case SENSOR_TYPE_ACCELEROMETER:
            liveSensorMask_ |= (1u << MASK_BIT_ACCEL);
            processAccelEvent(event);
            return;
        case SENSOR_TYPE_GYROSCOPE:
            liveSensorMask_ |= (1u << MASK_BIT_GYRO);
            lastGyro_ = {event.data[0], event.data[1], event.data[2]};
            frame_.yawRate = event.data[2];  // device Z-axis gyro
            break;
        case SENSOR_TYPE_MAGNETIC_FIELD:
            liveSensorMask_ |= (1u << MASK_BIT_MAG);
            break;
        case SENSOR_TYPE_GRAVITY:
            liveSensorMask_ |= (1u << MASK_BIT_GRAVITY_HW);
            break;
        case SENSOR_TYPE_LINEAR_ACCELERATION:
            liveSensorMask_ |= (1u << MASK_BIT_LINEAR_HW);
            break;
        case SENSOR_TYPE_ROTATION_VECTOR:
            liveSensorMask_ |= (1u << MASK_BIT_ROT_VEC);
            envSensors_.onRotationVector(event.data, 4);
            frame_.headingRad = envSensors_.headingRad();
            if ((liveSensorMask_ & (1u << MASK_BIT_GAME_ROT_VEC)) == 0) {
                orientRef_.update(event.data, 0.01f);
            }
            break;
        case SENSOR_TYPE_GAME_ROTATION_VECTOR:
            liveSensorMask_ |= (1u << MASK_BIT_GAME_ROT_VEC);
            orientRef_.update(event.data, 0.01f);
            break;
        case SENSOR_TYPE_PRESSURE:
            liveSensorMask_ |= (1u << MASK_BIT_PRESSURE);
            envSensors_.onPressure(event.data[0], 0.1f);
            frame_.pressureHpa = envSensors_.pressureHpa();
            frame_.climbRate   = envSensors_.climbRate();
            break;
        case SENSOR_TYPE_LIGHT:
            liveSensorMask_ |= (1u << MASK_BIT_LIGHT);
            envSensors_.onLight(event.data[0]);
            frame_.ambientLux = envSensors_.lux();
            break;
        case SENSOR_TYPE_PROXIMITY:
            liveSensorMask_ |= (1u << MASK_BIT_PROXIMITY);
            envSensors_.onProximity(event.data[0]);
            frame_.proximityCm = envSensors_.proximityCm();
            if (envSensors_.near()) {
                frame_.flags |= (1 << FLAG_PROXIMITY_NEAR);
            } else {
                frame_.flags &= ~(1 << FLAG_PROXIMITY_NEAR);
            }
            break;
        case SENSOR_TYPE_STEP_DETECTOR:
            liveSensorMask_ |= (1u << MASK_BIT_STEP_DETECTOR);
            walkingDet_.onStepEvent(event.timestamp);
            break;
        default:
            break;
    }
}

void MotionEngine::processAccelEvent(const ASensorEvent& event) {
    const int64_t tsNs = event.timestamp;

    // Handle pending reset (from control thread)
    if (resetPending_.exchange(false, std::memory_order_relaxed)) {
        gravityEst_.reset();
        orientRef_.reset();
        bandPassX_.reset();  bandPassY_.reset();  bandPassZ_.reset();
        walkingDet_.reset();
        stationaryDet_.reset();
        envSensors_.reset();
        speedEst_.reset();
        dotMotion_.reset();
        rateWindowStartNs_    = 0;
        rateWindowCount_      = 0;
        lastAccelTimestampNs_ = 0;
        droppedSamples_       = 0;
        liveSensorMask_       = 0;
        lastProcessedFixNs_   = 0;
    }

    // Compute dt before updating lastAccelTimestampNs_
    float dt = 0.01f;
    if (lastAccelTimestampNs_ > 0 && tsNs > lastAccelTimestampNs_) {
        dt = static_cast<float>(tsNs - lastAccelTimestampNs_) * 1e-9f;
        if (dt > 0.1f) dt = 0.01f;
    }

    // ── Rate measurement ──
    rateWindowCount_++;
    if (rateWindowStartNs_ == 0) {
        rateWindowStartNs_ = tsNs;
    } else if (rateWindowCount_ >= RATE_WINDOW_SAMPLES) {
        const float elapsedS = static_cast<float>(tsNs - rateWindowStartNs_) * 1e-9f;
        if (elapsedS > 0.0f) {
            frame_.measuredRateHz = static_cast<float>(rateWindowCount_) / elapsedS;
        }
        rateWindowStartNs_ = tsNs;
        rateWindowCount_ = 0;
    }

    // ── Dropped sample detection ──
    if (lastAccelTimestampNs_ > 0 && frame_.measuredRateHz > 0.0f) {
        const float expectedNs = 1e9f / frame_.measuredRateHz;
        const int64_t gap = tsNs - lastAccelTimestampNs_;
        if (static_cast<float>(gap) > 2.0f * expectedNs) {
            const int missed = static_cast<int>(static_cast<float>(gap) / expectedNs) - 1;
            if (missed > 0) droppedSamples_ += missed;
        }
    }
    lastAccelTimestampNs_ = tsNs;

    // ── Read latest config snapshot ──
    RuntimeConfig cfg;
    {
        std::lock_guard<std::mutex> lock(configMutex_);
        cfg = config_;
    }

    // Reconfigure band-pass filter if sample rate changed
    if (cfg.sampleRateHz != lastSampleRateHz_) {
        lastSampleRateHz_ = cfg.sampleRateHz;
        bandPassX_.setCoefficients(BP_LOW_HZ, BP_HIGH_HZ, cfg.sampleRateHz);
        bandPassY_.setCoefficients(BP_LOW_HZ, BP_HIGH_HZ, cfg.sampleRateHz);
        bandPassZ_.setCoefficients(BP_LOW_HZ, BP_HIGH_HZ, cfg.sampleRateHz);
    }

    // ── Check for new GPS fix (non-blocking) ──
    {
        std::unique_lock<std::mutex> lock(fixMutex_, std::try_to_lock);
        if (lock.owns_lock() && latestFix_.timestamp > lastProcessedFixNs_) {
            lastProcessedFixNs_ = latestFix_.timestamp;
            speedEst_.onFix(latestFix_.speedMps, latestFix_.speedAccMps, latestFix_.timestamp);
        }
    }

    // ── Store raw accel for debug output (device frame) ──
    frame_.longAccel = event.data[1];   // Y: forward in default portrait
    frame_.latAccel  = event.data[0];   // X: right in default portrait
    const Vec3 rawAccel{event.data[0], event.data[1], event.data[2]};

    // ── 1. Gravity estimation & linear accel extraction ──
    gravityEst_.update(rawAccel, lastGyro_, dt);
    const Vec3 linAccel = gravityEst_.linearAccel(rawAccel);

    // ── 2. Rotate to view frame (display orientation) ──
    const Vec3 viewAccel = OrientationReference::toViewFrame(linAccel, cfg.displayRotation);

    // ── 3. Band-pass filtering (removes DC drift and high-freq vibrations) ──
    const float bpX = bandPassX_.process(viewAccel.x);
    const float bpY = bandPassY_.process(viewAccel.y);

    // ── 4. Dead-zone & sensitivity scaling ──
    float deadX = 0.0f;
    if (std::abs(bpX) > cfg.deadZone) {
        deadX = bpX - std::copysign(cfg.deadZone, bpX);
    }
    float deadY = 0.0f;
    if (std::abs(bpY) > cfg.deadZone) {
        deadY = bpY - std::copysign(cfg.deadZone, bpY);
    }

    float rawShiftX = (deadX / cfg.referenceAccel) * cfg.sensitivity;
    float rawShiftY = (deadY / cfg.referenceAccel) * cfg.sensitivity;

    // ── 5. Detectors update ──
    const float accelEnergy = linAccel.length();
    const float gyroMag = lastGyro_.length();

    walkingDet_.update(dt, accelEnergy);
    rawShiftX *= walkingDet_.shiftGain();
    rawShiftY *= walkingDet_.shiftGain();

    const bool hasPressure = (liveSensorMask_ & (1u << MASK_BIT_PRESSURE)) != 0;
    stationaryDet_.update(accelEnergy, gyroMag, speedEst_.speed(), speedEst_.valid(),
                          frame_.climbRate, hasPressure, dt);

    if (stationaryDet_.stationary()) {
        speedEst_.onStationary();
    }

    // ── 6. Speed estimator update ──
    speedEst_.update(viewAccel.y, dt);

    // ── 7. Dot motion model update ──
    dotMotion_.update(
        rawShiftX, rawShiftY,
        cfg.accelWeight(),
        cfg.flowWeight() * walkingDet_.flowMult(),
        cfg.flowGain,
        cfg.flowFloorMps(),
        cfg.flowCapUnits,
        speedEst_.speed(),
        speedEst_.valid(),
        frame_.confidence,
        cfg.springStiffness,
        cfg.springDamping,
        dt
    );

    // ── 8. Populate output frame ──
    frame_.shiftX        = dotMotion_.shiftX();
    frame_.shiftY        = dotMotion_.shiftY();
    frame_.flowRate      = dotMotion_.flowRate();
    frame_.speedMps      = speedEst_.speed();
    frame_.stationarySec = stationaryDet_.stationarySec();

    // ── 9. Status flags ──
    frame_.flags |= (1 << FLAG_SENSORS_RUNNING);

    if (gravityEst_.settled()) {
        frame_.flags |= (1 << FLAG_GRAVITY_SETTLED);
    } else {
        frame_.flags &= ~(1 << FLAG_GRAVITY_SETTLED);
    }

    if (speedEst_.valid()) {
        frame_.flags |= (1 << FLAG_SPEED_VALID);
    } else {
        frame_.flags &= ~(1 << FLAG_SPEED_VALID);
    }

    if (walkingDet_.walking()) {
        frame_.flags |= (1 << FLAG_WALKING);
    } else {
        frame_.flags &= ~(1 << FLAG_WALKING);
    }

    if (stationaryDet_.stationary()) {
        frame_.flags |= (1 << FLAG_STATIONARY);
    } else {
        frame_.flags &= ~(1 << FLAG_STATIONARY);
    }

    // ── Write frame ──
    frame_.sensorMask     = static_cast<int32_t>(liveSensorMask_);
    frame_.droppedSamples = droppedSamples_;
    frame_.timestampNanos = tsNs;

    sharedFrame_.write(frame_);
}

} // namespace motionclue
