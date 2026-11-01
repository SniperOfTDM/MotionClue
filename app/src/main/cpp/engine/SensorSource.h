#pragma once
#include "SensorInventory.h"
#include <android/sensor.h>
#include <android/looper.h>
#include <thread>
#include <atomic>
#include <cstdint>

namespace motionclue {

class MotionEngine;

class SensorSource {
public:
    SensorSource(ASensorManager* manager, const SensorInventory& inventory, MotionEngine& engine);
    ~SensorSource();
    bool start(int samplePeriodUs);
    void stop();

private:
    void threadFunc();
    void enableSensors();
    void disableSensors();

    static constexpr int SENSOR_LOOPER_ID = 1;
    static constexpr int POLL_TIMEOUT_MS  = 100;

    ASensorManager*        manager_;
    const SensorInventory& inventory_;
    MotionEngine&          engine_;

    std::thread            thread_;
    std::atomic<bool>      running_{false};
    std::atomic<bool>      stopFlag_{false};
    std::atomic<ALooper*>  looper_{nullptr};
    ASensorEventQueue*     queue_ = nullptr;
    int                    samplePeriodUs_ = 10000;
};

} // namespace motionclue
