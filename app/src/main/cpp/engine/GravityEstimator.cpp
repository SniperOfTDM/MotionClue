#include "GravityEstimator.h"
#include "Config.h"

namespace motionclue {

void GravityEstimator::update(const Vec3&, const Vec3&, float) {}
Vec3 GravityEstimator::gravity() const { return {0.0f, 0.0f, -STANDARD_GRAVITY}; }
Vec3 GravityEstimator::linearAccel(const Vec3& accel) const { return accel - gravity(); }
bool GravityEstimator::settled() const { return false; }
void GravityEstimator::reset() {}

} // namespace motionclue
