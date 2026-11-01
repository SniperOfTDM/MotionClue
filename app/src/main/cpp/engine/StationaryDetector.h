#pragma once

namespace motionclue {

// Stationary detection (pipeline stage 8).
// Phase C1 stub: always not stationary.
class StationaryDetector {
public:
    void update(float accelEnergy, float gyroMag, float speedMps, bool speedValid,
                float climbRate, bool hasPressure, float dt);
    bool stationary() const;
    float stationarySec() const;
    void reset();
};

} // namespace motionclue
