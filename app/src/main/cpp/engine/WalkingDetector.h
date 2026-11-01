#pragma once
#include <cstdint>

namespace motionclue {

// Walking detection from step detector or accel energy (pipeline stage 7).
// Phase C1 stub: always not walking.
class WalkingDetector {
public:
    void onStepEvent(int64_t timestampNs);
    void update(float dt, float accelEnergy);
    bool walking() const;
    float shiftGain() const;
    float flowMult() const;
    void reset();
};

} // namespace motionclue
