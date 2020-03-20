package com.joseth.demo.camera;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;

import com.joseth.demo.CheckPermissionsActivity;
import com.joseth.demo.HandlerThreadHandler;
import com.joseth.demo.R;
import com.joseth.demo.SingleThreadExecutor;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

public class CameraPreview extends CheckPermissionsActivity implements Handler.Callback, Camera.PreviewCallback {

    private static final String TAG = "MainActivity";

    private TextureView mTextureView1;
    private boolean mTextureView1Ready;
    private TextureView mTextureView2;
    private boolean mTextureView2Ready;
    private Surface mDisplaySurface;
    private long mRenderHandler = 0;

    static final int UVC_FRAME_FORMAT_NV21 = 1;

    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;
    private int mPreviewWidth = PREVIEW_WIDTH;
    private int mPreviewHeight = PREVIEW_HEIGHT;

    private static final int MSG_ID_TEXTUREVIEW_READY = 1;
    private static final int MSG_ID_TEXTUREVIEW_DESTROY = 2;

    private SingleThreadExecutor mRenderExecutor = new SingleThreadExecutor(TAG);
    private HandlerThreadHandler mWorkHandler = HandlerThreadHandler.createHandler(TAG, this);
    private Camera mCamera;
    private byte[][] mPreviewBuffers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView1 = (TextureView) findViewById(R.id.textureView);
        mTextureView1.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mTextureView1Ready = true;
                mWorkHandler.sendEmptyMessage(MSG_ID_TEXTUREVIEW_READY);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                mTextureView1Ready = false;
                mWorkHandler.sendEmptyMessage(MSG_ID_TEXTUREVIEW_DESTROY);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        mTextureView2 = (TextureView) findViewById(R.id.textureView2);
        mTextureView2.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mTextureView2Ready = true;
                mWorkHandler.sendEmptyMessage(MSG_ID_TEXTUREVIEW_READY);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                mTextureView2Ready = true;

                mWorkHandler.sendEmptyMessage(MSG_ID_TEXTUREVIEW_DESTROY);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        } );

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI(null));
    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_ID_TEXTUREVIEW_READY:
                if (mTextureView1Ready && mTextureView2Ready)
                    handleOpenCamera();
                break;
            case MSG_ID_TEXTUREVIEW_DESTROY:
                if (!mTextureView1Ready || !mTextureView2Ready)
                    handleCloseCamera();
                break;
        }
        return false;
    }

    private void handleOpenCamera() {
        if (mCamera != null)
            handleCloseCamera();

        mCamera = Camera.open(0);

        Camera.Parameters parameters = mCamera.getParameters();

        getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), mPreviewWidth, mPreviewHeight);
        parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
        parameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setParameters(parameters);

        int bufSize = mPreviewWidth * mPreviewHeight * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
        mPreviewBuffers = new byte[2][bufSize];

        for (byte[] buf : mPreviewBuffers) {
            mCamera.addCallbackBuffer(buf);
        }
        mCamera.setPreviewCallbackWithBuffer(this);

        try {
            mCamera.setPreviewTexture(mTextureView1.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "setPreviewTexture exception", e);
        }
        mCamera.startPreview();

        mDisplaySurface = new Surface(mTextureView2.getSurfaceTexture());
        mRenderHandler = nativeRenderInit(mDisplaySurface, mPreviewWidth, mPreviewHeight);
        Log.d(TAG, "mRenderHandler = " + mRenderHandler);
    }

    private void handleCloseCamera() {
        if (mCamera == null)
            return;

        Log.d(TAG, "handleCloseCamera: mRenderHandler = " + mRenderHandler);
        if (mRenderHandler != 0) {
            nativeRenderRelease(mRenderHandler);
            mRenderHandler = 0;
        }

        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        Log.d(TAG, "camera closed");
    }

    private Object mBufferSync = new Object();
    private boolean mReady = true;

    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        Log.d(TAG, "onPreviewFrame: len = " + data.length);

        synchronized (mBufferSync) {
            if (!mReady) {
                mCamera.addCallbackBuffer(data);
                return;
            }

            mReady = false;
        }

        mWorkHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRenderHandler != 0) {
                    int ret = nativeRender(mRenderHandler, data, data.length, UVC_FRAME_FORMAT_NV21);

                    Log.d(TAG, "nativeRender: ret = " + ret);
                }

                if (mCamera != null)
                    mCamera.addCallbackBuffer(data);

                synchronized (mBufferSync) {
                    mReady = true;
                }
            }
        });
    }

    public void getOptimalPreviewSize(List<Camera.Size> previewSizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : previewSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : previewSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        mPreviewWidth = optimalSize.width;
        mPreviewHeight = optimalSize.height;
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI(FileDescriptor fileDescriptor);

    /**
     * @return handle of the surface
     *
     * */
    public static final native long nativeRenderInit(Surface surface, int width, int height);

    public static final native void nativeRenderRelease(final long handle);

    public static native int nativeRender(final long handle, final byte[] yuv, final int yuvSize, final int format);
}
