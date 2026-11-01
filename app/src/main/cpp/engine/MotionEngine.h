// app/src/main/cpp/engine/MotionEngine.h
#pragma once
#include "SharedFrame.h"
#include "SensorInventory.h"
#include "Config.h"
#include "GravityEstimator.h"
#include "OrientationReference.h"
#include "BandPass.h"
#include "WalkingDetector.h"
#include "StationaryDetector.h"
#include "EnvSensors.h"
#include "SpeedEstimator.h"
#include "DotMotionModel.h"
#include <android/sensor.h>
#include <mutex>
#include <memory>
#include <atomic>
#include <string>

namespace motionclue {

class SensorSource;  // forward — defined in SensorSource.h

class MotionEngine {
public:
    MotionEngine();
    ~MotionEngine();

    bool init(uint8_t* buffer, size_t capacity);
    bool start();
    void stop();

    void setFix(float speedMps, float speedAccMps,
                float bearingDeg, float bearingAccDeg,
                int64_t elapsedRealtimeNanos);
    void setConfig(const float* params, int length);
    void resetCalibration();
    std::string describeSensors() const;

    // Called by SensorSource on the sensor thread.
    // No allocation, no JNI, no blocking calls, no per-sample logging.
    void onSensorEvent(const ASensorEvent& event);

private:
    void processAccelEvent(const ASensorEvent& event);

    ASensorManager*                sensorManager_ = nullptr;
    SensorInventory                inventory_;
    std::unique_ptr<SensorSource>  sensorSource_;
    SharedFrame                    sharedFrame_;

    // Config (mutex-protected for cross-thread)
    mutable std::mutex configMutex_;
    RuntimeConfig      config_;

    // Fix data (mutex-protected)
    std::mutex fixMutex_;
    struct FixData {
        float   speedMps     = -1.0f;
        float   speedAccMps  = -1.0f;
        float   bearingDeg   = -1.0f;
        float   bearingAccDeg = -1.0f;
        int64_t timestamp    = 0;
    };
    FixData latestFix_;

    // Sensor-thread-only state
    FrameData frame_;
    uint32_t  liveSensorMask_     = 0;
    int64_t   rateWindowStartNs_  = 0;
    int       rateWindowCount_    = 0;
    int64_t   lastAccelTimestampNs_ = 0;
    int32_t   droppedSamples_     = 0;

    // Reset handoff (control thread -> sensor thread)
    std::atomic<bool> resetPending_{false};

    // Processing stages (stubs in C1)
    GravityEstimator    gravityEst_;
    OrientationReference orientRef_;
    BandPass            bandPassX_, bandPassY_, bandPassZ_;
    WalkingDetector     walkingDet_;
    StationaryDetector  stationaryDet_;
    EnvSensors          envSensors_;
    SpeedEstimator      speedEst_;
    DotMotionModel      dotMotion_;

    std::atomic<bool>   started_{false};
};

} // namespace motionclue
