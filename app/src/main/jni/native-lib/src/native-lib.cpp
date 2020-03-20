#include <jni.h>
#include <string>
#include <android/native_window_jni.h>
#include <utils.h>

#include "SurfaceRender.h"
#include "utils.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_joseth_demo_camera_CameraPreview_stringFromJNI(JNIEnv *env, jobject instance,
                                                         jobject fileDescriptor) {

    // TODO
    std::string hello = "Hello from C++";

    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_joseth_demo_camera_CameraPreview_nativeRenderInit(JNIEnv *env, jclass type,
                                                            jobject surface, jint width,
                                                            jint height) {
    ANativeWindow *window = surface ? ANativeWindow_fromSurface(env, surface) : NULL;

    if (window == NULL)
        return -1;

    SurfaceRender *render = new SurfaceRender(window, width, height);

    return reinterpret_cast<jlong>(render);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_joseth_demo_camera_CameraPreview_nativeRenderRelease(JNIEnv *env, jclass type,
                                                               jlong handle) {

    SurfaceRender *render = reinterpret_cast<SurfaceRender *>(handle);
    if (LIKELY(render)) {
        SAFE_DELETE(render);
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_joseth_demo_camera_CameraPreview_nativeRender(JNIEnv *env, jclass type, jlong handle,
                                                        jbyteArray yuv_, jint yuvSize, jint format) {
    int ret = -1;
    jbyte *yuv = env->GetByteArrayElements(yuv_, NULL);


    SurfaceRender *render = reinterpret_cast<SurfaceRender *>(handle);
    if (LIKELY(render)) {
        ret = render->render(yuv, yuvSize, format);
    }

    env->ReleaseByteArrayElements(yuv_, yuv, 0);

    return ret;
}