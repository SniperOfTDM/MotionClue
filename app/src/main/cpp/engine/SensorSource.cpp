#include "SensorSource.h"
#include "MotionEngine.h"
#include "Config.h"
#include <android/log.h>

// BACKGROUND TEST: timestamp must keep advancing for 60 s while another
// app is in foreground and screen is on.  HyperOS may throttle background
// sensor delivery; verify with a Logcat timestamp trace.

#define LOG_TAG "SensorSource"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace motionclue {

SensorSource::SensorSource(ASensorManager* manager, const SensorInventory& inventory,
                           MotionEngine& engine)
    : manager_(manager), inventory_(inventory), engine_(engine) {}

SensorSource::~SensorSource() { stop(); }

bool SensorSource::start(int samplePeriodUs) {
    if (running_.load()) return true;
    samplePeriodUs_ = samplePeriodUs;
    stopFlag_.store(false);
    running_.store(true);
    thread_ = std::thread(&SensorSource::threadFunc, this);
    return true;
}

void SensorSource::stop() {
    if (!running_.exchange(false)) return;
    stopFlag_.store(true, std::memory_order_relaxed);
    ALooper* lp = looper_.load(std::memory_order_acquire);
    if (lp) ALooper_wake(lp);
    if (thread_.joinable()) thread_.join();
}

void SensorSource::threadFunc() {
    ALooper* lp = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    looper_.store(lp, std::memory_order_release);
    if (!lp) {
        LOGE("ALooper_prepare failed");
        running_.store(false);
        return;
    }

    queue_ = ASensorManager_createEventQueue(manager_, lp, SENSOR_LOOPER_ID, nullptr, nullptr);
    if (!queue_) {
        LOGE("createEventQueue failed");
        running_.store(false);
        return;
    }

    enableSensors();

    while (!stopFlag_.load(std::memory_order_relaxed)) {
        int ident = ALooper_pollOnce(POLL_TIMEOUT_MS, nullptr, nullptr, nullptr);
        if (ident == SENSOR_LOOPER_ID) {
            ASensorEvent events[16];
            int count;
            while ((count = ASensorEventQueue_getEvents(queue_, events, 16)) > 0) {
                for (int i = 0; i < count; i++) {
                    engine_.onSensorEvent(events[i]);
                }
            }
        }
    }

    disableSensors();
    ASensorManager_destroyEventQueue(manager_, queue_);
    queue_ = nullptr;
    looper_.store(nullptr, std::memory_order_relaxed);
}

void SensorSource::enableSensors() {
    auto enable = [this](const ASensor* s, int periodUs) {
        if (!s || !queue_) return;
        ASensorEventQueue_enableSensor(queue_, s);
        if (periodUs > 0) ASensorEventQueue_setEventRate(queue_, s, periodUs);
    };
    enable(inventory_.accelerometer,      samplePeriodUs_);
    enable(inventory_.gyroscope,           samplePeriodUs_);
    enable(inventory_.gravity,             samplePeriodUs_);
    enable(inventory_.linearAccel,         samplePeriodUs_);
    enable(inventory_.gameRotationVector,  SLOW_SENSOR_PERIOD_US);
    enable(inventory_.rotationVector,      SLOW_SENSOR_PERIOD_US);
    enable(inventory_.magnetometer,        SLOW_SENSOR_PERIOD_US);
    enable(inventory_.pressure,            SLOW_SENSOR_PERIOD_US);
    enable(inventory_.light,               SLOW_SENSOR_PERIOD_US);
    enable(inventory_.proximity,           0);
    enable(inventory_.stepDetector,        0);
}

void SensorSource::disableSensors() {
    auto disable = [this](const ASensor* s) {
        if (s && queue_) ASensorEventQueue_disableSensor(queue_, s);
    };
    disable(inventory_.accelerometer);
    disable(inventory_.gyroscope);
    disable(inventory_.magnetometer);
    disable(inventory_.gravity);
    disable(inventory_.linearAccel);
    disable(inventory_.rotationVector);
    disable(inventory_.gameRotationVector);
    disable(inventory_.pressure);
    disable(inventory_.light);
    disable(inventory_.proximity);
    disable(inventory_.stepDetector);
}

} // namespace motionclue
