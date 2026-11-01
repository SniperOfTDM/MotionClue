#include "SharedFrame.h"
#include <atomic>

namespace motionclue {

bool SharedFrame::init(uint8_t* buffer, size_t capacity) {
    if (!buffer || capacity != BUFFER_SIZE) return false;
    buf_ = buffer;
    std::memset(buf_, 0, BUFFER_SIZE);
    int32_t version = BUFFER_VERSION;
    std::memcpy(buf_ + OFF_VERSION, &version, sizeof(version));
    sequence_ = 0;
    enabled_ = true;
    return true;
}

void SharedFrame::write(const FrameData& data) {
    if (!enabled_) return;
    // Seqlock: store sequence odd
    sequence_++;
    std::memcpy(buf_ + OFF_SEQUENCE, &sequence_, sizeof(sequence_));
    std::atomic_thread_fence(std::memory_order_release);
    // Write all data fields (slots 2-31)
    std::memcpy(buf_ + OFF_DATA, &data, sizeof(FrameData));
    std::atomic_thread_fence(std::memory_order_release);
    // Store sequence even
    sequence_++;
    std::memcpy(buf_ + OFF_SEQUENCE, &sequence_, sizeof(sequence_));
}

void SharedFrame::disable() {
    enabled_ = false;
}

} // namespace motionclue
