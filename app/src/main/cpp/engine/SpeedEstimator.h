#pragma once
#include <cstdint>

namespace motionclue {

// 1-D speed filter (pipeline stage 11).
// Phase C1 stub: always invalid.
class SpeedEstimator {
public:
    void update(float forwardAccel, float dt);
    void onFix(float speedMps, float accuracyMps, int64_t timestampNs);
    float speed() const;
    bool valid() const;
    void onStationary();
    void reset();
};

} // namespace motionclue
