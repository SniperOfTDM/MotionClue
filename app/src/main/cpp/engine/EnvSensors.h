#pragma once

namespace motionclue {

// Environmental sensor processing (pipeline stage 9).
// Phase C1: stores raw values only.
class EnvSensors {
public:
    void onLight(float lux);
    void onProximity(float cm);
    void onPressure(float hpa, float dt);
    void onRotationVector(const float* rv, int len);

    float lux() const;
    float proximityCm() const;
    bool near() const;
    float pressureHpa() const;
    float climbRate() const;
    float headingRad() const;

    void reset();

private:
    float lux_ = -1.0f;
    float proximityCm_ = -1.0f;
    float pressureHpa_ = -1.0f;
    float smoothedPressure_ = -1.0f;
    float climbRate_ = 0.0f;
    float headingRad_ = -1.0f;
};

} // namespace motionclue
