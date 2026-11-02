#include "OrientationReference.h"
#include "Config.h"
#include <cmath>

namespace motionclue {

void OrientationReference::update(const float* gameRotQuat, float /*dt*/) {
    if (!gameRotQuat) return;
    // Extract gravity direction from rotation quaternion.
    // Game rotation vector is [x, y, z, w] with w possibly at index 3.
    // Gravity in world frame is [0, 0, -g]. Rotate into device frame.
    float qx = gameRotQuat[0], qy = gameRotQuat[1], qz = gameRotQuat[2];
    float qw = (std::abs(qx*qx + qy*qy + qz*qz) < 1.0f)
             ? std::sqrt(1.0f - qx*qx - qy*qy - qz*qz)
             : 0.0f;

    // Rotate world-down [0,0,-g] by conjugate of quaternion to get device-frame gravity
    // R^T * [0,0,-g] where R is the rotation matrix from the quaternion
    float gx = -STANDARD_GRAVITY * 2.0f * (qx * qz - qw * qy);
    float gy = -STANDARD_GRAVITY * 2.0f * (qy * qz + qw * qx);
    float gz = -STANDARD_GRAVITY * (1.0f - 2.0f * (qx * qx + qy * qy));

    gravRef_ = {gx, gy, gz};
    hasRotation_ = true;
}

Vec3 OrientationReference::gravityReference() const {
    return gravRef_;
}

Vec3 OrientationReference::toViewFrame(const Vec3& deviceAccel, int displayRotation) {
    // Map device-frame linear acceleration to screen coordinates.
    // Device axes (default portrait): X=right, Y=up, Z=out-of-screen.
    // Screen axes: X=screen-right (lateral), Y=screen-up (longitudinal).
    float sx, sy;
    switch (displayRotation) {
        case 1: // ROTATION_90: landscape, device rotated CCW
            sx =  deviceAccel.y;
            sy = -deviceAccel.x;
            break;
        case 2: // ROTATION_180: upside-down portrait
            sx = -deviceAccel.x;
            sy = -deviceAccel.y;
            break;
        case 3: // ROTATION_270: landscape, device rotated CW
            sx = -deviceAccel.y;
            sy =  deviceAccel.x;
            break;
        default: // ROTATION_0: natural portrait
            sx = deviceAccel.x;
            sy = deviceAccel.y;
            break;
    }
    return {sx, sy, deviceAccel.z};
}

void OrientationReference::reset() {
    gravRef_ = {0.0f, 0.0f, -STANDARD_GRAVITY};
    hasRotation_ = false;
}

} // namespace motionclue
