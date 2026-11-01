#pragma once
#include "Vec3.h"

namespace motionclue {

// Complementary filter for gravity estimation (pipeline stage 2).
// Phase C1 stub: returns constant down vector.
class GravityEstimator {
public:
    void update(const Vec3& accel, const Vec3& gyro, float dt);
    Vec3 gravity() const;
    Vec3 linearAccel(const Vec3& accel) const;
    bool settled() const;
    void reset();
};

} // namespace motionclue
