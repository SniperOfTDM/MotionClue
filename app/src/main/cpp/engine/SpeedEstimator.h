#pragma once
#include <cstdint>

namespace motionclue {

// 1-D speed filter (pipeline stage 11).
// GPS-primary with staleness tracking and stationary override.
class SpeedEstimator {
public:
    void update(float forwardAccel, float dt);
    void onFix(float speedMps, float accuracyMps, int64_t timestampNs);
    float speed() const;
    bool valid() const;
    void onStationary();
    void reset();

private:
    float speed_ = -1.0f;
    float accuracy_ = -1.0f;
    int64_t lastFixNs_ = 0;
    int64_t currentNs_ = 0;
    bool hasValidFix_ = false;
};

} // namespace motionclue
