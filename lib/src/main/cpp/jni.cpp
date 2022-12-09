#include <jni.h>
#include <string>
#include <android/log.h>
#include "localsocket.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "LocalSocket", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "LocalSocket", __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_skynewborn_android_localsocket_Api_sendMessage(
        JNIEnv* env,
        jobject /* this */, 
        jstring serviceId,
        jstring message) {
    std::string sid(env->GetStringUTFChars(serviceId, nullptr));
    std::string request(env->GetStringUTFChars(message, nullptr));
    LOGI("Request: %s - %s", sid.c_str(), request.c_str());
    LocalSocket localSocket(sid);
    auto ret = localSocket.process(request);
    if (ret != 0) {
        LOGE("Failed to send, code: %d", ret);
        return nullptr;
    }
    LOGI("Response: %s", localSocket.getResponse().c_str());
    return env->NewStringUTF(localSocket.getResponse().c_str());
}