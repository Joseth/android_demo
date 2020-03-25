LOCAL_PATH	:= $(call my-dir)
COMMON_DIR := $(call my-dir)/../common

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
        src/uart.c	\
        src/uart_echo.c

LOCAL_C_INCLUDES := \
    $(COMMON_DIR)/include   \
    $(LOCAL_PATH)/include

LOCAL_LDLIBS := -llog
LOCAL_LDLIBS += -landroid
LOCAL_MODULE:= uart_echo
include $(BUILD_EXECUTABLE)

