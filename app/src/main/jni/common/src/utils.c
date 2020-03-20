//
// Created by joseth on 20-2-20.
//

#include <linux/videodev2.h>

int get_bits_per_pixel(int format)
{
    switch (format) {
        case V4L2_PIX_FMT_NV21:
            return 12;
        case V4L2_PIX_FMT_YUYV:
            return 16;
        case V4L2_PIX_FMT_MJPEG:
            return 16;
    }
    return -1;
}
