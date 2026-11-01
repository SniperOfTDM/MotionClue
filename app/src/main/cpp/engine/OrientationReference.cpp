#include "OrientationReference.h"
#include "Config.h"

namespace motionclue {

void OrientationReference::update(const float*, float) {}
Vec3 OrientationReference::gravityReference() const { return {0.0f, 0.0f, -STANDARD_GRAVITY}; }
void OrientationReference::reset() {}

} // namespace motionclue
