#include "GravityEstimator.h"
#include "Config.h"
#include <cmath>

namespace motionclue {

void GravityEstimator::update(const Vec3& accel, const Vec3& gyro, float dt) {
    if (dt <= 0.0f || dt > 0.5f) return;

    // Predict: rotate gravity estimate by angular velocity
    // g_pred = g - cross(omega, g) * dt  (small-angle approximation)
    Vec3 gyroCross = gyro.cross(gravity_);
    Vec3 predicted = gravity_ - gyroCross * dt;

    // Correction: blend toward accelerometer direction
    float accelMag = accel.length();
    float magErr = std::abs(accelMag - STANDARD_GRAVITY);

    // Gate correction: reduce trust when device is accelerating or rotating fast
    float gate = 1.0f;
    if (magErr > GRAVITY_ACCEL_GATE) {
        gate = 0.0f;
    } else {
        gate = 1.0f - (magErr / GRAVITY_ACCEL_GATE);
    }

    float gyroMag = gyro.length();
    if (gyroMag > GRAVITY_GYRO_GATE) {
        gate *= std::max(0.0f, 1.0f - (gyroMag - GRAVITY_GYRO_GATE));
    }

    // Complementary filter alpha
    float alpha = GRAVITY_TAU_S / (GRAVITY_TAU_S + dt);
    float correction = (1.0f - alpha) * gate;

    // Blend predicted with accelerometer-corrected
    Vec3 accelDir = accel.normalized() * STANDARD_GRAVITY;
    gravity_ = predicted * (1.0f - correction) + accelDir * correction;

    // Re-normalize to maintain gravity magnitude
    float gLen = gravity_.length();
    if (gLen > 1e-6f) {
        gravity_ = gravity_ * (STANDARD_GRAVITY / gLen);
    }

    // Track settle time: calm conditions = converging
    if (magErr < GRAVITY_ACCEL_GATE * 0.5f && gyroMag < GRAVITY_GYRO_GATE * 0.5f) {
        settleAccumS_ += dt;
    } else {
        settleAccumS_ = std::max(0.0f, settleAccumS_ - dt * 0.5f);
    }
}

Vec3 GravityEstimator::gravity() const {
    return gravity_;
}

Vec3 GravityEstimator::linearAccel(const Vec3& accel) const {
    return accel - gravity_;
}

bool GravityEstimator::settled() const {
    return settleAccumS_ >= GRAVITY_SETTLE_TIME_S;
}

void GravityEstimator::reset() {
    gravity_ = {0.0f, 0.0f, -STANDARD_GRAVITY};
    settleAccumS_ = 0.0f;
}

} // namespace motionclue
