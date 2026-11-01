#include "WalkingDetector.h"

namespace motionclue {

void WalkingDetector::onStepEvent(int64_t) {}
void WalkingDetector::update(float, float) {}
bool WalkingDetector::walking() const { return false; }
float WalkingDetector::shiftGain() const { return 1.0f; }
float WalkingDetector::flowMult() const { return 1.0f; }
void WalkingDetector::reset() {}

} // namespace motionclue
