LOCAL_PATH	:= $(call my-dir)
COMMON_DIR := $(call my-dir)/../common

LOCAL_SRC_FILES := \
        src/native-lib.cpp	\
        src/SurfaceRender.cpp	\
        src/yuv_utils.c

LOCAL_C_INCLUDES := \
    $(COMMON_DIR)/include   \
    $(LOCAL_PATH)/include

LOCAL_LDLIBS := -llog
LOCAL_LDLIBS += -landroid
LOCAL_MODULE:= native-lib
include $(BUILD_SHARED_LIBRARY)

