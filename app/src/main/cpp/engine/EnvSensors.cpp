#include "EnvSensors.h"
#include "Config.h"
#include <cmath>

namespace motionclue {

void EnvSensors::onLight(float lux) {
    lux_ = lux;
}

void EnvSensors::onProximity(float cm) {
    proximityCm_ = cm;
}

void EnvSensors::onPressure(float hpa, float dt) {
    pressureHpa_ = hpa;
    if (hpa <= 0.0f) return;

    if (smoothedPressure_ < 0.0f) {
        smoothedPressure_ = hpa;
        climbRate_ = 0.0f;
        return;
    }

    if (dt > 0.0f && dt < 1.0f) {
        // Barometric altitude delta: ~8.43 meters per 1 hPa near sea level
        // dH = -8.43 * (hpa - smoothedPressure)
        float prevSmooth = smoothedPressure_;
        float alpha = dt / (PRESSURE_SMOOTH_S + dt);
        smoothedPressure_ += alpha * (hpa - smoothedPressure_);

        float dP = smoothedPressure_ - prevSmooth;
        // Negative dP = ascending = positive climb rate
        climbRate_ = -8.43f * (dP / dt);
    }
}

void EnvSensors::onRotationVector(const float* rv, int len) {
    if (!rv || len < 3) return;
    // Calculate azimuth/heading from rotation vector
    // q = [x, y, z, (w)]
    float qx = rv[0], qy = rv[1], qz = rv[2];
    float qw = (len >= 4) ? rv[3] : 0.0f;
    if (len < 4) {
        float sum = qx*qx + qy*qy + qz*qz;
        qw = (sum < 1.0f) ? std::sqrt(1.0f - sum) : 0.0f;
    }

    // Yaw / heading angle
    float siny_cosp = 2.0f * (qw * qz + qx * qy);
    float cosy_cosp = 1.0f - 2.0f * (qy * qy + qz * qz);
    headingRad_ = std::atan2(siny_cosp, cosy_cosp);
}

float EnvSensors::lux() const { return lux_; }
float EnvSensors::proximityCm() const { return proximityCm_; }
bool EnvSensors::near() const { return proximityCm_ >= 0.0f && proximityCm_ < 1.0f; }
float EnvSensors::pressureHpa() const { return pressureHpa_; }
float EnvSensors::climbRate() const { return climbRate_; }
float EnvSensors::headingRad() const { return headingRad_; }

void EnvSensors::reset() {
    lux_ = -1.0f;
    proximityCm_ = -1.0f;
    pressureHpa_ = -1.0f;
    smoothedPressure_ = -1.0f;
    climbRate_ = 0.0f;
    headingRad_ = -1.0f;
}

} // namespace motionclue
