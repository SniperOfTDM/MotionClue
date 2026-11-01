#pragma once

namespace motionclue {

// 2nd-order biquad band-pass filter, one per axis.
// Phase C1 stub: passes input through unchanged.
class BandPass {
public:
    BandPass() = default;
    void setCoefficients(float /*lowHz*/, float /*highHz*/, float /*sampleHz*/) {}
    float process(float input) { return input; }
    void reset() {}
};

} // namespace motionclue
