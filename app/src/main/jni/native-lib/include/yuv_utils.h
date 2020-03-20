//
// Created by lishengjun on 19-5-9.
//

#ifndef MYAPPLICATION_YUV_UTILS_H
#define MYAPPLICATION_YUV_UTILS_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdio.h> // FILE
#include <sys/types.h>

#define PIXEL_NV21              1.5f
#define PIXEL_RGB565            2
#define PIXEL_UYVY              2
#define PIXEL_YUYV              2
#define PIXEL_RGB               3
#define PIXEL_BGR               3
#define PIXEL_RGBX              4



/** YUYV/YUV2/YUV422: YUV encoding with one luminance value per pixel and
 * one UV (chrominance) pair for every two pixels.
 */
const int32_t UVC_FRAME_FORMAT_NV21 = 1;
const int32_t UVC_FRAME_FORMAT_YUYV = 2;
const int32_t UVC_FRAME_FORMAT_UYVY = 3;
/** 16-bits RGB */
const int32_t UVC_FRAME_FORMAT_RGB565 = 4;    // RGB565
/** 24-bit RGB */
const int32_t UVC_FRAME_FORMAT_RGB = 5;        // RGB888
const int32_t UVC_FRAME_FORMAT_BGR = 6;        // BGR888
/* 32-bits RGB */
const int32_t UVC_FRAME_FORMAT_RGBX = 7;        // RGBX8888
/** Motion-JPEG (or JPEG) encoded images */
const int32_t UVC_FRAME_FORMAT_MJPEG = 8;
const int32_t UVC_FRAME_FORMAT_GRAY8 = 9;
const int32_t UVC_FRAME_FORMAT_BY8 = 10;


typedef struct frame {
    /** Image data for this frame */
    void *data;
    size_t data_bytes;

    /** Width of image in pixels */
    uint32_t width;

    /** Height of image in pixels */
    uint32_t height;

    /** Pixel data format */
    int32_t frame_format;

    /** Number of bytes per horizontal line (undefined for compressed format) */
    size_t step;

    /** Frame number (may skip, but is strictly monotonically increasing) */
    uint32_t sequence;
} frame_t;

int32_t get_pixel_size(const int format);

int32_t uvc_nv212rgbx(frame_t *in, frame_t *out);
int32_t uvc_yuyv2rgbx(frame_t *in, frame_t *out);		// XXX
int32_t uvc_uyvy2rgbx(frame_t *in, frame_t *out);		// XXX
int32_t uvc_rgb2rgbx(frame_t *in, frame_t *out);		// XXX
int32_t uvc_any2rgbx(frame_t *in, frame_t *out);		// XXX

frame_t *allocate_frame(size_t data_bytes);
void free_frame(frame_t *frame);

#ifdef __cplusplus
}
#endif

#endif //MYAPPLICATION_YUV_UTILS_H
