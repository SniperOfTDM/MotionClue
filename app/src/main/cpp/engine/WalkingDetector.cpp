#include "WalkingDetector.h"
#include "Config.h"

namespace motionclue {

void WalkingDetector::onStepEvent(int64_t timestampNs) {
    // Push into ring buffer
    stepRing_[ringHead_] = timestampNs;
    ringHead_ = (ringHead_ + 1) % MAX_STEPS;
    if (ringCount_ < MAX_STEPS) ringCount_++;
}

void WalkingDetector::update(float dt, float /*accelEnergy*/) {
    if (dt <= 0.0f) return;

    // Count recent steps within the sliding window
    if (ringCount_ > 0) {
        int64_t latestNs = stepRing_[(ringHead_ - 1 + MAX_STEPS) % MAX_STEPS];
        int64_t windowNs = static_cast<int64_t>(WALK_STEP_WINDOW_S * 1e9f);

        int recentCount = 0;
        for (int i = 0; i < ringCount_; i++) {
            int idx = (ringHead_ - 1 - i + MAX_STEPS * 2) % MAX_STEPS;
            if (latestNs - stepRing_[idx] <= windowNs) {
                recentCount++;
            } else {
                break;  // ring buffer is ordered
            }
        }

        if (recentCount >= WALK_MIN_STEPS) {
            walking_ = true;
            holdTimerS_ = WALK_HOLD_TIME_S;
        }
    }

    // Decay hold timer
    if (walking_ && holdTimerS_ > 0.0f) {
        holdTimerS_ -= dt;
        if (holdTimerS_ <= 0.0f) {
            walking_ = false;
            holdTimerS_ = 0.0f;
        }
    }
}

bool WalkingDetector::walking() const { return walking_; }

float WalkingDetector::shiftGain() const {
    return walking_ ? WALK_SHIFT_GAIN : 1.0f;
}

float WalkingDetector::flowMult() const {
    return walking_ ? 0.0f : 1.0f;
}

void WalkingDetector::reset() {
    for (int i = 0; i < MAX_STEPS; i++) stepRing_[i] = 0;
    ringHead_ = 0;
    ringCount_ = 0;
    holdTimerS_ = 0.0f;
    walking_ = false;
}

} // namespace motionclue
