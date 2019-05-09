//
// Created by lishengjun on 19-5-9.
//

#ifndef __SURFACE_RENDER_H__
#define __SURFACE_RENDER_H__

#include <android/native_window.h>
#include "yuv_utils.h"

class SurfaceRender {
public:
    SurfaceRender(ANativeWindow *window, const int32_t width, const int32_t height);
    ~SurfaceRender();

    int render(const int8_t yuv[], const int32_t yuvSize, const int32_t format);
private:
    ANativeWindow *mPreviewWindow;
    int32_t mWidth;
    int32_t mHeight;

    frame_t *mRgbxFrame;

};
#endif //__SURFACE_RENDER_H__
