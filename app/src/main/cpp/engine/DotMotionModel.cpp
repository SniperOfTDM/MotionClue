#include "DotMotionModel.h"

namespace motionclue {

void DotMotionModel::update(float, float, float, float, float, float, float,
                            float, bool, float, float, float, float) {}
float DotMotionModel::shiftX() const { return shiftX_; }
float DotMotionModel::shiftY() const { return shiftY_; }
float DotMotionModel::flowRate() const { return flowRate_; }
void DotMotionModel::reset() { shiftX_ = shiftY_ = flowRate_ = 0.0f; }

} // namespace motionclue
