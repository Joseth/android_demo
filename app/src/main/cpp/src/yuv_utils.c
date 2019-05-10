//
// Created by lishengjun on 19-5-9.
//
#define __ANDROID_DEBUG__
#define LOG_TAG     "yuvUtils"
#include <log.h>

#include <malloc.h>
#include "yuv_utils.h"
#include "error.h"
#include "utils.h"

#define USE_STRIDE 1

#define PIXEL2_RGB565        PIXEL_RGB565 * 2
#define PIXEL2_UYVY            PIXEL_UYVY * 2
#define PIXEL2_YUYV            PIXEL_YUYV * 2
#define PIXEL2_RGB            PIXEL_RGB * 2
#define PIXEL2_BGR            PIXEL_BGR * 2
#define PIXEL2_RGBX            PIXEL_RGBX * 2

#define PIXEL4_RGB565        PIXEL_RGB565 * 4
#define PIXEL4_UYVY            PIXEL_UYVY * 4
#define PIXEL4_YUYV            PIXEL_YUYV * 4
#define PIXEL4_RGB            PIXEL_RGB * 4
#define PIXEL4_BGR            PIXEL_BGR * 4
#define PIXEL4_RGBX            PIXEL_RGBX * 4

#define PIXEL8_RGB565        PIXEL_RGB565 * 8
#define PIXEL8_UYVY            PIXEL_UYVY * 8
#define PIXEL8_YUYV            PIXEL_YUYV * 8
#define PIXEL8_RGB            PIXEL_RGB * 8
#define PIXEL8_BGR            PIXEL_BGR * 8
#define PIXEL8_RGBX            PIXEL_RGBX * 8

#define PIXEL16_RGB565        PIXEL_RGB565 * 16
#define PIXEL16_UYVY        PIXEL_UYVY * 16
#define PIXEL16_YUYV        PIXEL_YUYV * 16
#define PIXEL16_RGB            PIXEL_RGB * 16
#define PIXEL16_BGR            PIXEL_BGR * 16
#define PIXEL16_RGBX        PIXEL_RGBX * 16

static inline unsigned char sat(int i) {
    return (unsigned char) (i >= 255 ? 255 : (i < 0 ? 0 : i));
}

int32_t get_pixel_size(const int format) {
    switch (format) {
        case UVC_FRAME_FORMAT_YUYV:
            return PIXEL_YUYV;
        case UVC_FRAME_FORMAT_UYVY:
            return PIXEL_UYVY;
        case UVC_FRAME_FORMAT_RGB:
            return PIXEL_RGB;
        case UVC_FRAME_FORMAT_RGBX:
            return PIXEL_RGBX;
        default:
            return 0;
    }
}

#define IYUYV2RGBX_2(pyuv, prgbx, ax, bx) { \
        const int d1 = (pyuv)[ax+1]; \
        const int d3 = (pyuv)[ax+3]; \
        const int r = (22987 * (d3/*(pyuv)[ax+3]*/ - 128)) >> 14; \
        const int g = (-5636 * (d1/*(pyuv)[ax+1]*/ - 128) - 11698 * (d3/*(pyuv)[ax+3]*/ - 128)) >> 14; \
        const int b = (29049 * (d1/*(pyuv)[ax+1]*/ - 128)) >> 14; \
        const int y0 = (pyuv)[ax+0]; \
        (prgbx)[bx+0] = sat(y0 + r); \
        (prgbx)[bx+1] = sat(y0 + g); \
        (prgbx)[bx+2] = sat(y0 + b); \
        (prgbx)[bx+3] = 0xff; \
        const int y2 = (pyuv)[ax+2]; \
        (prgbx)[bx+4] = sat(y2 + r); \
        (prgbx)[bx+5] = sat(y2 + g); \
        (prgbx)[bx+6] = sat(y2 + b); \
        (prgbx)[bx+7] = 0xff; \
}

#define IYUYV2RGBX_4(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_2(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_2(pyuv, prgbx, ax + PIXEL2_YUYV, bx + PIXEL2_RGBX);

#define IYUYV2RGBX_8(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_4(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_4(pyuv, prgbx, ax + PIXEL4_YUYV, bx + PIXEL4_RGBX);

#define IYUYV2RGBX_16(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_8(pyuv, prgbx, ax, bx) \
    IYUYV2RGBX_8(pyuv, prgbx, ax + PIXEL8_YUYV, bx + PIXEL8_RGBX);

int32_t uvc_ensure_frame_size(frame_t *frame, size_t need_bytes) {
    if (UNLIKELY(!frame->data || frame->data_bytes < need_bytes))
        return UVC_ERROR_NO_MEM;
    return UVC_SUCCESS;
}

int32_t uvc_any2rgbx(frame_t *in, frame_t *out) {
    LOGV("uvc_any2rgbx: format = %d, out = %d", in->frame_format, out->data);

    switch (in->frame_format) {
        case UVC_FRAME_FORMAT_NV21:
            return uvc_nv212rgbx(in, out);
        case UVC_FRAME_FORMAT_YUYV:
            return uvc_yuyv2rgbx(in, out);
        case UVC_FRAME_FORMAT_UYVY:
            return uvc_uyvy2rgbx(in, out);
        case UVC_FRAME_FORMAT_RGB:
            return uvc_rgb2rgbx(in, out);
        default:
            return UVC_ERROR_NOT_SUPPORTED;
    }
}

// BT.601 YUV to RGB reference
//  R = (Y - 16) * 1.164              - V * -1.596
//  G = (Y - 16) * 1.164 - U *  0.391 - V *  0.813
//  B = (Y - 16) * 1.164 - U * -2.018

// Y contribution to R,G,B.  Scale and bias.
#define YG 18997  /* round(1.164 * 64 * 256 * 256 / 257) */
#define YGB -1160 /* 1.164 * 64 * -16 + 64 / 2 */

// U and V contributions to R,G,B.
#define UB -128 /* max(-128, round(-2.018 * 64)) */
#define UG 25   /* round(0.391 * 64) */
#define VG 52   /* round(0.813 * 64) */
#define VR -102 /* round(-1.596 * 64) */

// Bias values to subtract 16 from Y and 128 from U and V.
#define BB (UB * 128 + YGB)
#define BG (UG * 128 + VG * 128 + YGB)
#define BR (VR * 128 + YGB)

int32_t clamp0(int32_t v) { return ((-(v) >> 31) & (v)); }

int32_t clamp255(int32_t v) { return (((255 - (v)) >> 31) | (v)) & 255; }

uint32_t Clamp(int32_t val) {
    int v = clamp0(val);
    return (uint32_t) (clamp255(v));
}

void YuvPixel(uint8_t y, uint8_t u, uint8_t v, uint8_t *b, uint8_t *g, uint8_t *r) {
    uint32_t y1 = (uint32_t) (y * 0x0101 * YG) >> 16;
    *b = Clamp((int32_t) (-(u * UB) + y1 + BB) >> 6);
    *g = Clamp((int32_t) (-(v * VG + u * UG) + y1 + BG) >> 6);
    *r = Clamp((int32_t) (-(v * VR) + y1 + BR) >> 6);
}

void NV21ToRGBXRow_C(const uint8_t *src_y, const uint8_t *src_vu, uint8_t *rgb_buf, int width) {
    int x;

    for (x = 0; x < width - 1; x += 2) {
//        LOGV("NV21ToRGBXRow_C: x = %d, src_y = %d, src_vu = %d, rgb_buf = %d", x, src_y, src_vu, rgb_buf);

        YuvPixel(src_y[0], src_vu[1], src_vu[0], rgb_buf + 2, rgb_buf + 1, rgb_buf + 0);
        rgb_buf[3] = 255;

        YuvPixel(src_y[1], src_vu[1], src_vu[0], rgb_buf + 6, rgb_buf + 5, rgb_buf + 4);
        rgb_buf[7] = 255;

        src_y += 2;
        src_vu += 2;
        rgb_buf += 8;
    }

//    LOGV("NV21ToRGBXRow_C: width = %d", width);
    if (width & 1) {
        YuvPixel(src_y[0], src_vu[1], src_vu[0], rgb_buf + 2, rgb_buf + 1, rgb_buf + 0);
        rgb_buf[3] = 255;
    }
}

int NV21ToRGBX(const uint8_t *src_y, int src_stride_y, const uint8_t *src_uv, int src_stride_uv,
               uint8_t *dst_argb, int dst_stride_argb, int width, int height) {
    int y;

//    LOGD("NV21ToRGBX: START");

    if (!src_y || !src_uv || !dst_argb || width <= 0 || height == 0) { return -1; }

    for (y = 0; y < height; ++y) {
//        LOGD("NV21ToRGBX: y = %d", y);

        NV21ToRGBXRow_C(src_y, src_uv, dst_argb, width);
        dst_argb += dst_stride_argb;
        src_y += src_stride_y;
        if (y & 1) {
            src_uv += src_stride_uv;
        }
    }
    return 0;
}

int32_t uvc_nv212rgbx(frame_t *in, frame_t *out) {
    int src_width = in->width; // 原图的宽度
    int src_height = in->height; // 原图的高度
    const uint8_t* src = in->data; // 原图的地址
    uint8_t* crop_argb = out->data; // RGBA的地址
    int aligned_src_width = (src_width + 1) & ~1;
    const uint8_t* src_uv = src + aligned_src_width * src_height;
    int argb_stride = src_width * PIXEL_RGBX; //  should be width * 4 or width must be 1 right

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;

    return NV21ToRGBX(src, src_width, src_uv, aligned_src_width,
            crop_argb, argb_stride, src_width, src_height);
}

int32_t uvc_yuyv2rgbx(frame_t *in, frame_t *out) {
    if (UNLIKELY(in->frame_format != UVC_FRAME_FORMAT_YUYV))
        return UVC_ERROR_INVALID_PARAM;

    if (UNLIKELY(uvc_ensure_frame_size(out, in->width * in->height * PIXEL_RGBX) < 0))
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;

    uint8_t *pyuv = in->data;
    const uint8_t *pyuv_end = pyuv + in->data_bytes - PIXEL8_YUYV;
    uint8_t *prgbx = out->data;
    const uint8_t *prgbx_end = prgbx + out->data_bytes - PIXEL8_RGBX;

    // YUYV => RGBX8888
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            pyuv = in->data + in->step * h;
            prgbx = out->data + out->step * h;
            for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) && (w < ww);) {
                IYUYV2RGBX_8(pyuv, prgbx, 0, 0);

                prgbx += PIXEL8_RGBX;
                pyuv += PIXEL8_YUYV;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end);) {
            IYUYV2RGBX_8(pyuv, prgbx, 0, 0);

            prgbx += PIXEL8_RGBX;
            pyuv += PIXEL8_YUYV;
        }
    }
#else
    for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) ;) {
        IYUYV2RGBX_8(pyuv, prgbx, 0, 0);

        prgbx += PIXEL8_RGBX;
        pyuv += PIXEL8_YUYV;
    }
#endif
    return UVC_SUCCESS;
}

#define IUYVY2RGBX_2(pyuv, prgbx, ax, bx) { \
        const int d0 = (pyuv)[ax+0]; \
        const int d2 = (pyuv)[ax+2]; \
        const int r = (22987 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
        const int g = (-5636 * (d0/*(pyuv)[ax+0]*/ - 128) - 11698 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
        const int b = (29049 * (d0/*(pyuv)[ax+0]*/ - 128)) >> 14; \
        const int y1 = (pyuv)[ax+1]; \
        (prgbx)[bx+0] = sat(y1 + r); \
        (prgbx)[bx+1] = sat(y1 + g); \
        (prgbx)[bx+2] = sat(y1 + b); \
        (prgbx)[bx+3] = 0xff; \
        const int y3 = (pyuv)[ax+3]; \
        (prgbx)[bx+4] = sat(y3 + r); \
        (prgbx)[bx+5] = sat(y3 + g); \
        (prgbx)[bx+6] = sat(y3 + b); \
        (prgbx)[bx+7] = 0xff; \
    }
#define IUYVY2RGBX_16(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_8(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_8(pyuv, prgbx, ax + PIXEL8_UYVY, bx + PIXEL8_RGBX)
#define IUYVY2RGBX_8(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_4(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_4(pyuv, prgbx, ax + PIXEL4_UYVY, bx + PIXEL4_RGBX)
#define IUYVY2RGBX_4(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_2(pyuv, prgbx, ax, bx) \
    IUYVY2RGBX_2(pyuv, prgbx, ax + PIXEL2_UYVY, bx + PIXEL2_RGBX)

int32_t uvc_uyvy2rgbx(frame_t *in, frame_t *out) {
    if (UNLIKELY(in->frame_format != UVC_FRAME_FORMAT_UYVY))
        return UVC_ERROR_INVALID_PARAM;

    if (UNLIKELY(uvc_ensure_frame_size(out, in->width * in->height * PIXEL_RGBX) < 0))
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;
    out->sequence = in->sequence;

    uint8_t *pyuv = in->data;
    const uint8_t *pyuv_end = pyuv + in->data_bytes - PIXEL8_UYVY;
    uint8_t *prgbx = out->data;
    const uint8_t *prgbx_end = prgbx + out->data_bytes - PIXEL8_RGBX;

    // UYVY => RGBX8888
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            pyuv = in->data + in->step * h;
            prgbx = out->data + out->step * h;
            for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) && (w < ww);) {
                IUYVY2RGBX_8(pyuv, prgbx, 0, 0);

                prgbx += PIXEL8_RGBX;
                pyuv += PIXEL8_UYVY;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end);) {
            IUYVY2RGBX_8(pyuv, prgbx, 0, 0);

            prgbx += PIXEL8_RGBX;
            pyuv += PIXEL8_UYVY;
        }
    }
#else
    for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) ;) {
        IUYVY2RGBX_8(pyuv, prgbx, 0, 0);

        prgbx += PIXEL8_RGBX;
        pyuv += PIXEL8_UYVY;
    }
#endif
    return UVC_SUCCESS;
}


#define RGB2RGBX_2(prgb, prgbx, ax, bx) { \
        (prgbx)[bx+0] = (prgb)[ax+0]; \
        (prgbx)[bx+1] = (prgb)[ax+1]; \
        (prgbx)[bx+2] = (prgb)[ax+2]; \
        (prgbx)[bx+3] = 0xff; \
        (prgbx)[bx+4] = (prgb)[ax+3]; \
        (prgbx)[bx+5] = (prgb)[ax+4]; \
        (prgbx)[bx+6] = (prgb)[ax+5]; \
        (prgbx)[bx+7] = 0xff; \
    }
#define RGB2RGBX_16(prgb, prgbx, ax, bx) \
    RGB2RGBX_8(prgb, prgbx, ax, bx) \
    RGB2RGBX_8(prgb, prgbx, ax + PIXEL8_RGB, bx +PIXEL8_RGBX);
#define RGB2RGBX_8(prgb, prgbx, ax, bx) \
    RGB2RGBX_4(prgb, prgbx, ax, bx) \
    RGB2RGBX_4(prgb, prgbx, ax + PIXEL4_RGB, bx + PIXEL4_RGBX);
#define RGB2RGBX_4(prgb, prgbx, ax, bx) \
    RGB2RGBX_2(prgb, prgbx, ax, bx) \
    RGB2RGBX_2(prgb, prgbx, ax + PIXEL2_RGB, bx + PIXEL2_RGBX);


int32_t uvc_rgb2rgbx(frame_t *in, frame_t *out) {
    if (UNLIKELY(in->frame_format != UVC_FRAME_FORMAT_RGB))
        return UVC_ERROR_INVALID_PARAM;

    if (UNLIKELY(uvc_ensure_frame_size(out, in->width * in->height * PIXEL_RGBX) < 0))
        return UVC_ERROR_NO_MEM;

    out->width = in->width;
    out->height = in->height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;
    out->sequence = in->sequence;

    uint8_t *prgb = in->data;
    const uint8_t *prgb_end = prgb + in->data_bytes - PIXEL8_RGB;
    uint8_t *prgbx = out->data;
    const uint8_t *prgbx_end = prgbx + out->data_bytes - PIXEL8_RGBX;

    // RGB888 to RGBX8888
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
        const int hh = in->height < out->height ? in->height : out->height;
        const int ww = in->width < out->width ? in->width : out->width;
        int h, w;
        for (h = 0; h < hh; h++) {
            w = 0;
            prgb = in->data + in->step * h;
            prgbx = out->data + out->step * h;
            for (; (prgbx <= prgbx_end) && (prgb <= prgb_end) && (w < ww);) {
                RGB2RGBX_8(prgb, prgbx, 0, 0);

                prgb += PIXEL8_RGB;
                prgbx += PIXEL8_RGBX;
                w += 8;
            }
        }
    } else {
        // compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
        for (; (prgbx <= prgbx_end) && (prgb <= prgb_end);) {
            RGB2RGBX_8(prgb, prgbx, 0, 0);

            prgb += PIXEL8_RGB;
            prgbx += PIXEL8_RGBX;
        }
    }
#else
    for (; (prgbx <= prgbx_end) && (prgb <= prgb_end) ;) {
        RGB2RGBX_8(prgb, prgbx, 0, 0);

        prgb += PIXEL8_RGB;
        prgbx += PIXEL8_RGBX;
    }
#endif
    return UVC_SUCCESS;
}

frame_t *allocate_frame(size_t data_bytes) {
    LOGV("allocate_frame: data_bytes = %d", data_bytes);

    frame_t *frame = malloc(sizeof(frame_t));

    if (UNLIKELY(!frame))
        return NULL;

#ifndef __ANDROID__
    // XXX in many case, it is not neccesary to clear because all fields are set before use
    // therefore we remove this to improve performace, but be care not to forget to set fields before use
    memset(frame, 0, sizeof(*frame));	// bzero(frame, sizeof(*frame)); // bzero is deprecated
#endif

    if (LIKELY(data_bytes > 0)) {

        frame->data_bytes = data_bytes;
        frame->data = malloc(data_bytes);

        if (UNLIKELY(!frame->data)) {
            free(frame);
            return NULL;
        }

        LOGV("data_bytes = %d, dataPtr = %d", data_bytes, frame->data);
    }

    return frame;
}

/** @brief Free a frame structure
 * @ingroup frame
 *
 * @param frame Frame to destroy
 */
void free_frame(frame_t *frame) {
    if (frame->data_bytes > 0)
        free(frame->data);

    free(frame);
}
