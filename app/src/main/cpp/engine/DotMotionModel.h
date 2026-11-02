#pragma once

namespace motionclue {

// Spring-damper shift + flow rate (pipeline stage 12).
// Second-order critically damped spring driven by filtered lateral acceleration.
class DotMotionModel {
public:
    void update(float rawShiftX, float rawShiftY,
                float accelWeight, float flowWeight,
                float flowGain, float flowFloorMps, float flowCapUnits,
                float speedMps, bool speedValid, float confidence,
                float springK, float springD, float dt);
    float shiftX() const;
    float shiftY() const;
    float flowRate() const;
    void reset();

private:
    float shiftX_ = 0.0f, shiftY_ = 0.0f, flowRate_ = 0.0f;
    float velX_ = 0.0f, velY_ = 0.0f;
    float flowSmooth_ = 0.0f;
};

} // namespace motionclue
