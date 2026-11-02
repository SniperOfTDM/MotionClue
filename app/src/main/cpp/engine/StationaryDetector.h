#pragma once

namespace motionclue {

// Stationary detection (pipeline stage 8).
// Requires low accel energy, low gyro, and low speed sustained for STAT_TIME_S.
class StationaryDetector {
public:
    void update(float accelEnergy, float gyroMag, float speedMps, bool speedValid,
                float climbRate, bool hasPressure, float dt);
    bool stationary() const;
    float stationarySec() const;
    void reset();

private:
    float accumS_ = 0.0f;
    bool stationary_ = false;
};

} // namespace motionclue
