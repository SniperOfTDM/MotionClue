#include "SpeedEstimator.h"

namespace motionclue {

void SpeedEstimator::update(float, float) {}
void SpeedEstimator::onFix(float, float, int64_t) {}
float SpeedEstimator::speed() const { return -1.0f; }
bool SpeedEstimator::valid() const { return false; }
void SpeedEstimator::onStationary() {}
void SpeedEstimator::reset() {}

} // namespace motionclue
