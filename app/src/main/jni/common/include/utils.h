//
// Created by joseth on 20-2-20.
//
#ifndef HYTERNADEMO_UTILS_H
#define HYTERNADEMO_UTILS_H
#ifdef __cplusplus
extern "C" {
#endif

#if defined(__GNUC__)
// the macro for branch prediction optimaization for gcc(-O2/-O3 required)
#define		CONDITION(cond)				((__builtin_expect((cond)!=0, 0)))
#define		LIKELY(x)					((__builtin_expect(!!(x), 1)))	// x is likely true
#define		UNLIKELY(x)					((__builtin_expect(!!(x), 0)))	// x is likely false
#else
#define		CONDITION(cond)				((cond))
#define		LIKELY(x)					((x))
#define		UNLIKELY(x)					((x))
#endif

#define		SAFE_FREE(p)				{ if (p) { free((p)); (p) = NULL; } }
#define		SAFE_DELETE(p)				{ if (p) { delete (p); (p) = NULL; } }

#define  MIN(a, b)  (((a) > (b)) ? (b) : (a))
#define  MAX(a, b)  (((a) > (b)) ? (a) : (b))

int get_bits_per_pixel(int format);

#ifdef __cplusplus
}
#endif
#endif //HYTERNADEMO_UTILS_H
