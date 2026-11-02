#include "StationaryDetector.h"
#include "Config.h"
#include <cmath>

namespace motionclue {

void StationaryDetector::update(float accelEnergy, float gyroMag, float speedMps,
                                 bool speedValid, float climbRate, bool hasPressure,
                                 float dt) {
    if (dt <= 0.0f) return;

    // All conditions must be calm simultaneously
    bool accelCalm = accelEnergy < 0.3f;  // low linear accel energy
    bool gyroCalm  = gyroMag < 0.1f;      // low rotation rate
    bool speedLow  = !speedValid || speedMps < STAT_SPEED_THRESHOLD;
    bool climbLow  = !hasPressure || std::abs(climbRate) < STAT_CLIMB_THRESHOLD;

    if (accelCalm && gyroCalm && speedLow && climbLow) {
        accumS_ += dt;
    } else {
        // Decay faster than accumulate
        accumS_ = (accumS_ > dt * 2.0f) ? (accumS_ - dt * 2.0f) : 0.0f;
    }

    stationary_ = (accumS_ >= STAT_TIME_S);
}

bool StationaryDetector::stationary() const { return stationary_; }

float StationaryDetector::stationarySec() const { return accumS_; }

void StationaryDetector::reset() {
    accumS_ = 0.0f;
    stationary_ = false;
}

} // namespace motionclue
