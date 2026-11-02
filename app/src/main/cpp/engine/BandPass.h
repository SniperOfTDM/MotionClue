#pragma once
#include <cmath>

namespace motionclue {

// Cascaded first-order highpass + lowpass = bandpass filter, one per axis.
// HP removes DC (gravity residuals below lowHz), LP removes vibration above highHz.
class BandPass {
public:
    BandPass() = default;

    void setCoefficients(float lowHz, float highHz, float sampleHz) {
        if (sampleHz <= 0.0f || lowHz <= 0.0f || highHz <= 0.0f) return;
        const float dt = 1.0f / sampleHz;

        // Highpass: RC time constant from low cutoff
        const float tauHP = 1.0f / (2.0f * PI * lowHz);
        hpAlpha_ = tauHP / (tauHP + dt);

        // Lowpass: RC time constant from high cutoff
        const float tauLP = 1.0f / (2.0f * PI * highHz);
        lpAlpha_ = dt / (tauLP + dt);

        configured_ = true;
    }

    float process(float input) {
        if (!configured_) return input;

        // First-order highpass
        const float hpOut = hpAlpha_ * (hpPrev_ + input - hpPrevIn_);
        hpPrevIn_ = input;
        hpPrev_ = hpOut;

        // First-order lowpass
        lpOut_ += lpAlpha_ * (hpOut - lpOut_);

        return lpOut_;
    }

    void reset() {
        hpPrev_ = 0.0f;
        hpPrevIn_ = 0.0f;
        lpOut_ = 0.0f;
    }

private:
    static constexpr float PI = 3.14159265358979323846f;
    float hpAlpha_ = 1.0f;
    float lpAlpha_ = 1.0f;
    float hpPrev_ = 0.0f;
    float hpPrevIn_ = 0.0f;
    float lpOut_ = 0.0f;
    bool configured_ = false;
};

} // namespace motionclue
