#include "EnvSensors.h"

namespace motionclue {

void EnvSensors::onLight(float lux) { lux_ = lux; }
void EnvSensors::onProximity(float cm) { proximityCm_ = cm; }
void EnvSensors::onPressure(float hpa, float) { pressureHpa_ = hpa; }
void EnvSensors::onRotationVector(const float*, int) {}

float EnvSensors::lux() const { return lux_; }
float EnvSensors::proximityCm() const { return proximityCm_; }
bool EnvSensors::near() const { return proximityCm_ >= 0.0f && proximityCm_ < 1.0f; }
float EnvSensors::pressureHpa() const { return pressureHpa_; }
float EnvSensors::climbRate() const { return 0.0f; }
float EnvSensors::headingRad() const { return -1.0f; }

void EnvSensors::reset() {
    lux_ = -1.0f;
    proximityCm_ = -1.0f;
    pressureHpa_ = -1.0f;
}

} // namespace motionclue
