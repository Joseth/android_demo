package com.joseth.demo.audio;

import android.Manifest;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.joseth.demo.CheckPermissionsActivity;
import com.joseth.demo.R;
import com.joseth.demo.common.FileUtils;
import com.joseth.demo.common.Mlog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MockAudioCall extends CheckPermissionsActivity {
    private static final String TAG = "MockAudioCall";

    private static final int RECORDING_RATE = 8000; // can go up to 44K, if needed
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int PCM_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final String PLAY_FILE_NAME = "audioplay.pcm";
    private static final String RECORD_FILE_NAME = "audiorecord.pcm";

    private Button mBtnToggle;

    private Object mStopLocker = new Object();
    protected volatile boolean mRequestStop = true;
    private AudioRecorderTask mRecordTask;
    private File mRecordFile;
    private AudioTrackTask mTrackTask;
    private File mPlayFile;

    protected String[] needPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    protected String[] getNeedPermissions() {
        return needPermissions;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock_audio_call);

        mBtnToggle = findViewById(R.id.btn_toggle);
        mBtnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (mStopLocker) {
                    if (mRequestStop) {
                        start();
                    } else {
                        stop();
                    }
                    mBtnToggle.setText(mRequestStop ? R.string.start : R.string.stop);
                }
            }
        });

        File outDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        if (outDir.exists())
            outDir.mkdirs();
        mPlayFile = new File(outDir, PLAY_FILE_NAME);
        mRecordFile = new File(outDir, RECORD_FILE_NAME);

        mRecordTask = new AudioRecorderTask(mRecordFile);
        mTrackTask = new AudioTrackTask(mPlayFile);
    }

    private void start() {
        Mlog.d(TAG, "start");

        synchronized (mStopLocker) {
            mRequestStop = false;
        }

        if (mRecordFile.exists())
            mRecordFile.renameTo(mPlayFile);

        mRecordTask.startTask();
        mTrackTask.startTask();
    }

    private void stop() {
        Mlog.d(TAG, "stop");

        synchronized (mStopLocker) {
            if (mRequestStop) {
                return;
            }
            mRequestStop = true;    // for rejecting newer frame
        }

        mTrackTask.stopTask();
        mRecordTask.stopTask();
    }

    class AudioTrackTask extends Thread {
        private static final String TAG = "AudioTrackTask";
        private final File mPlayFile;

        AudioTrackTask(final File playFile) {
            mPlayFile = playFile;
        }

        int startTask() {
            Mlog.d(TAG, "startTask");
            start();

            return 0;
        }

        void stopTask() {
            Mlog.d(TAG, "stopTask");
            interrupt();
        }

        private AudioTrack createAudioTrack() {
            AudioTrack audioTrack;
            final int intSize = AudioTrack.getMinBufferSize(RECORDING_RATE, CHANNELS_OUT, PCM_FORMAT);

            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    RECORDING_RATE,
                    CHANNELS_OUT,
                    PCM_FORMAT,
                    intSize,
                    AudioTrack.MODE_STREAM);

            return audioTrack;
        }

        private void releaseAudioTrack(AudioTrack audioTrack) {
            if (audioTrack != null)
                audioTrack.release();
        }

        private boolean doPlay(AudioTrack audioTrack) throws IOException {
            int cnt = 0;
            final int intSize = AudioTrack.getMinBufferSize(RECORDING_RATE, CHANNELS_OUT, PCM_FORMAT);
            byte[] buffer = new byte[intSize * 2];
            FileInputStream in = null;
            BufferedInputStream bis = null;
            int read;

            in = new FileInputStream(mPlayFile);
            bis = new BufferedInputStream(in);

            Mlog.d(TAG, "play...");
            audioTrack.play();
            try {
                while (!interrupted()) {
                    synchronized (mStopLocker) {
                        if (mRequestStop)
                            break;
                    }

                    if ((read = bis.read(buffer, 0, buffer.length)) > 0) {
                        audioTrack.write(buffer, 0, read);
                        Mlog.v(TAG, "read " + read);
                    }
                }
            } finally {
                audioTrack.stop();

                FileUtils.closeStream(in);
                FileUtils.closeStream(bis);
            }

            Mlog.d(TAG, "stop: cnt = " + cnt);
            return cnt > 0;
        }

        @Override
        public void run() {
            AudioTrack audioTrack = createAudioTrack();
            boolean success = false;

            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            if (audioTrack != null) {
                try {
                    success = doPlay(audioTrack);
                } catch (Exception e) {
                    Log.e(TAG, "doPlay exception: " + e.toString());
                } finally {
                    releaseAudioTrack(audioTrack);
                }
            }

            Mlog.d(TAG, "success = " + success);
        }
    }

    private class AudioRecorderTask extends Thread {
        static final String TAG = "AudioRecorderTask";
        private File mOutFile;

        AudioRecorderTask(final File outFile) {
            super("AudioRecorderTask");

            mOutFile = outFile;
        }

        int startTask() {
            Mlog.d(TAG, "startTask");
            start();

            return 0;
        }

        void stopTask() {
            Mlog.d(TAG, "stopTask");
            interrupt();
        }

        private final int[] AUDIO_SOURCES = new int[]{
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.CAMCORDER,
                MediaRecorder.AudioSource.DEFAULT,
        };

        private AudioRecord createAudioRecord() {
            final int minBufferSize = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, PCM_FORMAT);
            AudioRecord audioRecord = null;
            for (final int src : AUDIO_SOURCES) {
                try {
                    audioRecord = new AudioRecord(src,
                            RECORDING_RATE, CHANNEL_IN, PCM_FORMAT, minBufferSize * 3);
                    if (audioRecord != null) {
                        Mlog.d(TAG, src + " minBufferSize = " + minBufferSize);
                        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                            audioRecord.release();
                            audioRecord = null;
                        }
                    }
                } catch (final Exception e) {
                    audioRecord = null;
                }
                if (audioRecord != null) {
                    Mlog.d(TAG, "create success: AudioSource = " + audioRecord.getAudioSource());
                    break;
                }
            }

            return audioRecord;
        }

        private void releaseAudioRecord(AudioRecord audioRecord) {
            if (audioRecord != null)
                audioRecord.release();
        }

        private boolean doRecord(AudioRecord audioRecord) throws IOException {
            int cnt = 0;
            final int minBufferSize = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, PCM_FORMAT);
            byte []buf = new byte[minBufferSize];
            int readBytes;
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;

            Mlog.d(TAG, "recording");
            audioRecord.startRecording();
            try {
                fos = new FileOutputStream(mOutFile);
                bos = new BufferedOutputStream(fos);

                while (!interrupted()) {
                    synchronized (mStopLocker) {
                        if (mRequestStop)
                            break;
                    }

                    readBytes = audioRecord.read(buf, 0, buf.length);
                    if (readBytes > 0) {
                        bos.write(buf, 0, readBytes);
                        cnt++;
                        Mlog.v(TAG, "read: " + readBytes);
                    }
                }
            } finally {
                audioRecord.stop();
                FileUtils.closeStream(bos);
                FileUtils.closeStream(fos);
            }

            return cnt > 0;
        }

        @Override
        public void run() {
            boolean success = false;
            AudioRecord audioRecord = createAudioRecord();

            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            if (audioRecord != null) {
                try {
                    success = doRecord(audioRecord);
                } catch (final Exception e) {
                    Log.e(TAG, "doRecord exception", e);
                } finally {
                    releaseAudioRecord(audioRecord);
                }
            }
            Mlog.v(TAG, "AudioThread:success = " + success);
        }
    }
}
