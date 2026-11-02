#pragma once
#include "Vec3.h"

namespace motionclue {

// Slow drift reference from game rotation vector (pipeline stage 3).
// Tracks device orientation for view-frame rotation.
class OrientationReference {
public:
    void update(const float* gameRotQuat, float dt);
    Vec3 gravityReference() const;
    void reset();

    // Rotate a device-frame vector into view-frame based on display rotation.
    // Returns Vec3 where x=screen-right, y=screen-up lateral/longitudinal components.
    static Vec3 toViewFrame(const Vec3& deviceAccel, int displayRotation);

private:
    Vec3 gravRef_{0.0f, 0.0f, -9.80665f};
    bool hasRotation_ = false;
};

} // namespace motionclue
