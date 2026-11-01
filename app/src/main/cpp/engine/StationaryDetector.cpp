#include "StationaryDetector.h"

namespace motionclue {

void StationaryDetector::update(float, float, float, bool, float, bool, float) {}
bool StationaryDetector::stationary() const { return false; }
float StationaryDetector::stationarySec() const { return 0.0f; }
void StationaryDetector::reset() {}

} // namespace motionclue
