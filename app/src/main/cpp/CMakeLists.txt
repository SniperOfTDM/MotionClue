cmake_minimum_required(VERSION 3.22.1)
project(motionclue_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(motionclue_native SHARED
    jni_bridge.cpp
    engine/MotionEngine.cpp
    engine/SensorInventory.cpp
    engine/SensorSource.cpp
    engine/SharedFrame.cpp
    engine/GravityEstimator.cpp
    engine/OrientationReference.cpp
    engine/WalkingDetector.cpp
    engine/StationaryDetector.cpp
    engine/EnvSensors.cpp
    engine/SpeedEstimator.cpp
    engine/DotMotionModel.cpp
)

target_include_directories(motionclue_native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_options(motionclue_native PRIVATE -O2 -Wall -Wextra -Wpedantic)

# 16 KB page-size compatibility (required for Android 15+)
target_link_options(motionclue_native PRIVATE "-Wl,-z,max-page-size=16384")

target_link_libraries(motionclue_native
    android
    log
)
