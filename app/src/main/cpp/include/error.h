//
// Created by lishengjun on 19-5-9.
//

#ifndef MYAPPLICATION_ER_H
#define MYAPPLICATION_ER_H


const int32_t UVC_SUCCESS = 0;
/** Input/output error */
const int32_t UVC_ERROR_IO = -1;
/** Invalid parameter */
const int32_t UVC_ERROR_INVALID_PARAM = -2;
/** Access denied */
const int32_t UVC_ERROR_ACCESS = -3;
/** No such device */
const int32_t UVC_ERROR_NO_DEVICE = -4;
/** Entity not found */
const int32_t UVC_ERROR_NOT_FOUND = -5;
/** Resource busy */
const int32_t UVC_ERROR_BUSY = -6;
/** Operation timed out */
const int32_t UVC_ERROR_TIMEOUT = -7;
/** Overflow */
const int32_t UVC_ERROR_OVERFLOW = -8;
/** Pipe error */
const int32_t UVC_ERROR_PIPE = -9;
/** System call interrupted */
const int32_t UVC_ERROR_INTERRUPTED = -10;
/** Insufficient memory */
const int32_t UVC_ERROR_NO_MEM = -11;
/** Operation not supported */
const int32_t UVC_ERROR_NOT_SUPPORTED = -12;
/** Device is not UVC-compliant */
const int32_t UVC_ERROR_INVALID_DEVICE = -50;
/** Mode not supported */
const int32_t UVC_ERROR_INVALID_MODE = -51;
/** Resource has a callback (can't use polling and async) */
const int32_t UVC_ERROR_CALLBACK_EXISTS = -52;
/** Undefined error */
const int32_t UVC_ERROR_OTHER = -99;

#endif //MYAPPLICATION_ER_H
