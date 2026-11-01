// app/src/main/cpp/jni_bridge.cpp
//
// JNI bridge for libmotionclue_native.so (contract v2).
// 8 registered methods on NativeMotionEngine.
// No exception may cross the JNI boundary.

#include <jni.h>
#include <android/log.h>
#include <new>
#include <string>
#include "engine/MotionEngine.h"

#define LOG_TAG "motionclue_jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace motionclue;

static inline MotionEngine* toEngine(jlong handle) {
    return reinterpret_cast<MotionEngine*>(handle);
}

// ── 1. nativeCreate ──
static jlong jni_nativeCreate(JNIEnv* env, jobject /*thiz*/, jobject buffer) {
    try {
        auto* ptr = static_cast<uint8_t*>(env->GetDirectBufferAddress(buffer));
        jlong cap = env->GetDirectBufferCapacity(buffer);
        if (!ptr || cap != 128) {
            LOGE("Invalid buffer: ptr=%p cap=%lld", (void*)ptr, (long long)cap);
            return 0;
        }
        auto* engine = new (std::nothrow) MotionEngine();
        if (!engine) {
            LOGE("MotionEngine alloc failed");
            return 0;
        }
        if (!engine->init(ptr, static_cast<size_t>(cap))) {
            LOGE("MotionEngine::init failed");
            delete engine;
            return 0;
        }
        LOGI("Engine created handle=%p", (void*)engine);
        return reinterpret_cast<jlong>(engine);
    } catch (...) {
        LOGE("Exception in nativeCreate");
        return 0;
    }
}

// ── 2. nativeStart ──
static jboolean jni_nativeStart(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    try {
        if (!handle) return JNI_FALSE;
        return toEngine(handle)->start() ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        LOGE("Exception in nativeStart");
        return JNI_FALSE;
    }
}

// ── 3. nativeStop ──
static void jni_nativeStop(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    try {
        if (handle) toEngine(handle)->stop();
    } catch (...) {
        LOGE("Exception in nativeStop");
    }
}

// ── 4. nativeSetFix ──
static void jni_nativeSetFix(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle,
                              jfloat speedMps, jfloat speedAccMps,
                              jfloat bearingDeg, jfloat bearingAccDeg,
                              jlong elapsedRealtimeNanos) {
    try {
        if (handle)
            toEngine(handle)->setFix(speedMps, speedAccMps,
                                     bearingDeg, bearingAccDeg,
                                     elapsedRealtimeNanos);
    } catch (...) {
        LOGE("Exception in nativeSetFix");
    }
}

// ── 5. nativeSetConfig ──
static void jni_nativeSetConfig(JNIEnv* env, jobject /*thiz*/, jlong handle,
                                 jfloatArray params) {
    try {
        if (!handle || !params) return;
        jint len = env->GetArrayLength(params);
        if (len < 12) return;
        jfloat* elems = env->GetFloatArrayElements(params, nullptr);
        if (!elems) return;
        toEngine(handle)->setConfig(elems, len);
        env->ReleaseFloatArrayElements(params, elems, JNI_ABORT);
    } catch (...) {
        LOGE("Exception in nativeSetConfig");
    }
}

// ── 6. nativeResetCalibration ──
static void jni_nativeResetCalibration(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    try {
        if (handle) toEngine(handle)->resetCalibration();
    } catch (...) {
        LOGE("Exception in nativeResetCalibration");
    }
}

// ── 7. nativeDescribeSensors ──
// May allocate (NewStringUTF). This is a control call, not a sensor callback.
static jstring jni_nativeDescribeSensors(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    try {
        if (!handle) return env->NewStringUTF("");
        std::string text = toEngine(handle)->describeSensors();
        jstring result = env->NewStringUTF(text.c_str());
        return result ? result : env->NewStringUTF("");
    } catch (...) {
        LOGE("Exception in nativeDescribeSensors");
        return env->NewStringUTF("");
    }
}

// ── 8. nativeDestroy ──
static void jni_nativeDestroy(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    try {
        if (!handle) return;
        auto* engine = toEngine(handle);
        LOGI("Engine destroy handle=%p", (void*)engine);
        delete engine;   // ~MotionEngine calls stop() then disable()
    } catch (...) {
        LOGE("Exception in nativeDestroy");
    }
}

// ── JNI_OnLoad: register all 8 methods ──

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("GetEnv failed");
        return -1;
    }

    jclass clazz = env->FindClass(
        "com/sniperoftdm/motionclueandro/bridge/NativeMotionEngine");
    if (!clazz) {
        LOGE("FindClass NativeMotionEngine failed");
        return -1;
    }

    // Contract v2 method table — 8 methods
    static const JNINativeMethod methods[] = {
        {"nativeCreate",           "(Ljava/nio/ByteBuffer;)J",
                                   reinterpret_cast<void*>(jni_nativeCreate)},
        {"nativeStart",            "(J)Z",
                                   reinterpret_cast<void*>(jni_nativeStart)},
        {"nativeStop",             "(J)V",
                                   reinterpret_cast<void*>(jni_nativeStop)},
        {"nativeSetFix",           "(JFFFFJ)V",
                                   reinterpret_cast<void*>(jni_nativeSetFix)},
        {"nativeSetConfig",        "(J[F)V",
                                   reinterpret_cast<void*>(jni_nativeSetConfig)},
        {"nativeResetCalibration", "(J)V",
                                   reinterpret_cast<void*>(jni_nativeResetCalibration)},
        {"nativeDescribeSensors",  "(J)Ljava/lang/String;",
                                   reinterpret_cast<void*>(jni_nativeDescribeSensors)},
        {"nativeDestroy",          "(J)V",
                                   reinterpret_cast<void*>(jni_nativeDestroy)},
    };

    if (env->RegisterNatives(clazz, methods,
                              sizeof(methods) / sizeof(methods[0])) < 0) {
        LOGE("RegisterNatives failed (Kotlin v2 migration pending?)");
        return -1;
    }

    LOGI("motionclue_native loaded (contract v2, 8 methods)");
    return JNI_VERSION_1_6;
}
