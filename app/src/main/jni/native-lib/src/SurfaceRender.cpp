//
// Created by lishengjun on 19-5-9.
//
#define __ANDROID_DEBUG__
#define LOG_TAG     "SurfaceRender"
#include <mc_log.h>

#include <assert.h>
#include <string.h>

#include "SurfaceRender.h"
#include "utils.h"
#include "yuv_utils.h"
#include "error.h"

SurfaceRender::SurfaceRender(ANativeWindow *window, const int32_t width, const int32_t height) {
    mPreviewWindow = window;
    mWidth = width;
    mHeight = height;
    mRgbxFrame = NULL;

    if (LIKELY(window)) {
        ANativeWindow_setBuffersGeometry(mPreviewWindow, width, height, WINDOW_FORMAT_RGBA_8888);
    }
}

SurfaceRender::~SurfaceRender() {
    ALOGD("~SurfaceRender");
    if (mPreviewWindow != NULL) {
        ANativeWindow_release(mPreviewWindow);
        mPreviewWindow = NULL;
        ALOGD("release window");
    }

    if (mRgbxFrame != NULL) {
        free_frame(mRgbxFrame);
        mRgbxFrame = NULL;
        ALOGD("free mRgbxFrame");
    }
}

static void copyFrame(const uint8_t *src, uint8_t *dest, const int width, int height, const int stride_src, const int stride_dest) {
    const int h8 = height % 8;
    for (int i = 0; i < h8; i++) {
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
    }
    for (int i = 0; i < height; i += 8) {
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
    }
}

int copyToSurface(const frame_t *frame, ANativeWindow **window) {
    ALOGD("copyToSurface start: width = %d, height = %d, rgbxSize = %d", frame->width, frame->height, frame->data_bytes);

    int result = 0;
    if (LIKELY(*window)) {
        ANativeWindow_Buffer buffer;
        if (LIKELY(ANativeWindow_lock(*window, &buffer, NULL) == 0)) {
            // source = frame data
            const uint8_t *src = (uint8_t *)frame->data;
            const int src_w = frame->width * PIXEL_RGBX;
            const int src_step = frame->width * PIXEL_RGBX;
            // destination = Surface(ANativeWindow)
            uint8_t *dest = (uint8_t *)buffer.bits;
            const int dest_w = buffer.width * PIXEL_RGBX;
            const int dest_step = buffer.stride * PIXEL_RGBX;
            // use lower transfer bytes
            const int w = src_w < dest_w ? src_w : dest_w;
            // use lower height
            const int h = frame->height < buffer.height ? frame->height : buffer.height;
            // transfer from frame data to the Surface
            copyFrame(src, dest, w, h, src_step, dest_step);
            ANativeWindow_unlockAndPost(*window);
        } else {
            result = -1;
        }
    } else {
        result = -1;
    }

    ALOGV("copyToSurface: result = %d", result);

    return result; //RETURN(result, int);
}

int SurfaceRender::render(const int8_t *yuv, const int32_t yuvSize, const int32_t format) {
    int32_t ret;

    ALOGV("render: size = %d, format = %d, width = %d, height = %d", yuvSize, format, mWidth, mHeight);

    if (mPreviewWindow == NULL)
        return -1;

    if (mRgbxFrame == NULL)
        mRgbxFrame = allocate_frame(mWidth * mHeight * PIXEL_RGBX);
    if (mRgbxFrame == NULL)
        return UVC_ERROR_NO_MEM;

    frame_t origFrame;
    origFrame.data = (void *) yuv;
    origFrame.data_bytes = yuvSize;
    origFrame.frame_format = format;
    origFrame.width = mWidth;
    origFrame.height = mHeight;

    switch (format) {
        case UVC_FRAME_FORMAT_YUYV:
            origFrame.step = origFrame.width * 2;
            break;
        case UVC_FRAME_FORMAT_MJPEG:
            origFrame.step = 0;
            break;
        default:
            origFrame.step = 0;
            break;
    }

    ret = uvc_any2rgbx(&origFrame, mRgbxFrame);
    ALOGV("render: uvc_any2rgbx: ret = %d, mRgbxFrame = 0x%X", ret, mRgbxFrame->data);
    if (ret < 0) {
        return ret;
    }

    return copyToSurface(mRgbxFrame, &mPreviewWindow);
//    return ret;
}


