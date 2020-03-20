//
// Created by joseth on 20-2-19.
//

#ifndef HYTERNADEMO_MC_LOG_H
#define HYTERNADEMO_MC_LOG_H

#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef LOG_TAG
#define LOG_TAG "MCLOG"
#endif

#ifdef LOG_NDEBUG
#define ALOGV(...)      ((void)0)
#define ALOGD(...)      ((void)0)
#define ALOGI(...)      ((void)0)
#define ALOGW(...)      ((void)0)
#define ALOGE(...)      ((void)0)
#else
#ifdef CONFIG_DEBUG
#define ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#else
#define ALOGV(...)      ((void)0)
#define ALOGD(...)      ((void)0)
#endif /* CONFIG_DEBUG */
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif /* LOG_NDEBUG */

#define ASSERT(cond, fmt, ...)                                \
  if (!(cond)) {                                              \
    __android_log_assert(#cond, LOG_TAG, fmt, ##__VA_ARGS__); \
  }

#define FUN_ENTER()     ALOGV("%s enter", __func__)
#define FUN_EXIT()      ALOGV("%s exit", __func__)

#ifdef __cplusplus
}
#endif
#endif //HYTERNADEMO_MC_LOG_H
