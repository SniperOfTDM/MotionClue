#pragma once
#include <cstdint>

namespace motionclue {

// Walking detection from step detector or accel energy (pipeline stage 7).
// Counts steps in a sliding window; holds walking state with timeout.
class WalkingDetector {
public:
    void onStepEvent(int64_t timestampNs);
    void update(float dt, float accelEnergy);
    bool walking() const;
    float shiftGain() const;
    float flowMult() const;
    void reset();

private:
    static constexpr int MAX_STEPS = 32;
    int64_t stepRing_[MAX_STEPS] = {};
    int ringHead_ = 0;
    int ringCount_ = 0;
    float holdTimerS_ = 0.0f;
    bool walking_ = false;
};

} // namespace motionclue
