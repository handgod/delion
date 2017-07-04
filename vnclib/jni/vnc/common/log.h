#include <android/log.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "VNC", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , "VNC", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO  , "VNC", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN  , "VNC", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "VNC", __VA_ARGS__)

