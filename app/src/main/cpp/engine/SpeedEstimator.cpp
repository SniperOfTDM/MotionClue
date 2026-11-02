#include "SpeedEstimator.h"
#include "Config.h"
#include <cmath>
#include <algorithm>

namespace motionclue {

void SpeedEstimator::update(float /*forwardAccel*/, float dt) {
    // Advance internal clock for staleness checks
    if (dt > 0.0f && dt < 0.5f) {
        currentNs_ += static_cast<int64_t>(dt * 1e9f);
    }

    // Check GPS staleness
    if (hasValidFix_ && lastFixNs_ > 0 && currentNs_ > 0) {
        float ageS = static_cast<float>(currentNs_ - lastFixNs_) * 1e-9f;
        if (ageS > GPS_DEAD_S) {
            hasValidFix_ = false;
            speed_ = -1.0f;
        } else if (ageS > GPS_STALE_S) {
            // Fade confidence but keep last speed
        }
    }
}

void SpeedEstimator::onFix(float speedMps, float accuracyMps, int64_t timestampNs) {
    if (speedMps < 0.0f) return;

    speed_ = speedMps;
    accuracy_ = accuracyMps;
    lastFixNs_ = timestampNs;
    currentNs_ = timestampNs;
    hasValidFix_ = true;
}

float SpeedEstimator::speed() const {
    return hasValidFix_ ? speed_ : -1.0f;
}

bool SpeedEstimator::valid() const {
    return hasValidFix_;
}

void SpeedEstimator::onStationary() {
    speed_ = 0.0f;
}

void SpeedEstimator::reset() {
    speed_ = -1.0f;
    accuracy_ = -1.0f;
    lastFixNs_ = 0;
    currentNs_ = 0;
    hasValidFix_ = false;
}

} // namespace motionclue
