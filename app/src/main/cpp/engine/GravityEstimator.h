#pragma once
#include "Vec3.h"

namespace motionclue {

// Complementary filter for gravity estimation (pipeline stage 2).
// Gyro predicts, accelerometer corrects. Gated on accel magnitude and gyro rate.
class GravityEstimator {
public:
    void update(const Vec3& accel, const Vec3& gyro, float dt);
    Vec3 gravity() const;
    Vec3 linearAccel(const Vec3& accel) const;
    bool settled() const;
    void reset();

private:
    Vec3 gravity_{0.0f, 0.0f, -9.80665f};
    float settleAccumS_ = 0.0f;
};

} // namespace motionclue
