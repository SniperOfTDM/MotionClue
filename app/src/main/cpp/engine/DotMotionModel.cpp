#include "DotMotionModel.h"
#include "Config.h"
#include <cmath>
#include <algorithm>

namespace motionclue {

void DotMotionModel::update(float rawShiftX, float rawShiftY,
                            float accelWeight, float flowWeight,
                            float flowGain, float flowFloorMps, float flowCapUnits,
                            float speedMps, bool speedValid, float /*confidence*/,
                            float springK, float springD, float dt) {
    if (dt <= 0.0f || dt > 0.1f) return;

    // ── Shift: spring-damped second-order system ──
    // Force = input acceleration (already normalized/scaled by caller)
    float forceX = rawShiftX * accelWeight;
    float forceY = rawShiftY * accelWeight;

    // Spring-damper: a = -k*x - d*v + F
    float ax = -springK * shiftX_ - springD * velX_ + forceX * springK;
    float ay = -springK * shiftY_ - springD * velY_ + forceY * springK;

    // Semi-implicit Euler integration
    velX_ += ax * dt;
    velY_ += ay * dt;
    shiftX_ += velX_ * dt;
    shiftY_ += velY_ * dt;

    // Clamp output to [-1, 1]
    shiftX_ = std::clamp(shiftX_, -1.0f, 1.0f);
    shiftY_ = std::clamp(shiftY_, -1.0f, 1.0f);

    // ── Flow: speed-proportional scrolling ──
    float targetFlow = 0.0f;
    if (speedValid && speedMps >= 0.0f && flowWeight > 0.0f) {
        float above = speedMps - flowFloorMps;
        if (above > 0.0f) {
            // Map speed above floor to flow units, capped
            float raw = above * flowGain;
            targetFlow = std::min(raw, flowCapUnits) * flowWeight;
        }
    }

    // Smooth flow transitions with exponential fade
    float fadeFactor = 1.0f - std::exp(-dt / FLOW_FADE_TAU_S);
    flowSmooth_ += (targetFlow - flowSmooth_) * fadeFactor;
    flowRate_ = flowSmooth_;
}

float DotMotionModel::shiftX() const { return shiftX_; }
float DotMotionModel::shiftY() const { return shiftY_; }
float DotMotionModel::flowRate() const { return flowRate_; }

void DotMotionModel::reset() {
    shiftX_ = shiftY_ = flowRate_ = 0.0f;
    velX_ = velY_ = 0.0f;
    flowSmooth_ = 0.0f;
}

} // namespace motionclue
