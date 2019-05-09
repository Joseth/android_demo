//
// Created by G7 on 2018/8/8.
//

#ifndef G7EYE_ADAS_LOG_H
#define G7EYE_ADAS_LOG_H

#ifdef __cplusplus
extern "C" {
#endif

#include <android/log.h>

#if defined(__ANDROID_DEBUG__)
#define LOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__))
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))
#else
#define LOGV(...)
#define LOGD(...)
#define LOGI(...)
#define LOGW(...)
#define LOGE(...)
#endif

#ifdef __cplusplus
}
#endif
#endif //G7EYE_ADAS_LOG_H
