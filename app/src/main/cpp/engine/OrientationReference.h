#pragma once
#include "Vec3.h"

namespace motionclue {

// Slow drift reference from game rotation vector (pipeline stage 3).
// Phase C1 stub: returns constant gravity direction.
class OrientationReference {
public:
    void update(const float* gameRotQuat, float dt);
    Vec3 gravityReference() const;
    void reset();
};

} // namespace motionclue
