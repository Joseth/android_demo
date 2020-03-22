package com.joseth.demo.audio;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

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
import java.util.List;

public class MockAudioCall extends CheckPermissionsActivity {
    private static final String TAG = "MockAudioCall";

    private static final int STREAM_TYPE = 6; /* STREAM_BLUETOOTH_SCO */
    private static final int RECORDING_RATE = 8000; // can go up to 44K, if needed
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int PCM_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final String PLAY_FILE_NAME = "audioplay.pcm";
    private static final String RECORD_FILE_NAME = "audiorecord.pcm";

    private Button mBtnToggle;
    private Switch mSwitchBtState;
    private Switch mSwitchScoOn;
    private Switch mSwitchVoiceDialer;
    private Spinner mSpinnerMode;

    private BluetoothHeadsetInfo mBtHeadsetInfo;

    private Object mStopLocker = new Object();
    protected volatile boolean mRequestStop = true;
    private AudioRecorderTask mRecordTask;
    private File mRecordFile;
    private AudioTrackTask mTrackTask;
    private File mPlayFile;

    private AudioManager mAudioManager;
    private boolean mForceScoOn;

    private static final String[] mModeStrings = {
            "NORMAL", "RINGTONE", "IN_CALL", "IN_COMMUNICATION"
    };
    private int mCurrentMode;

    protected String[] needPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH
    };

    protected String[] getNeedPermissions() {
        return needPermissions;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock_audio_call);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

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

        setVolumeControlStream(STREAM_TYPE);

        mSwitchBtState = findViewById(R.id.bt_switch);
        mSwitchScoOn = findViewById(R.id.switch_force_sco_on);
        mSwitchScoOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtHeadsetInfo.setForceScoOn(mSwitchScoOn.isChecked());
            }
        });
        mSwitchVoiceDialer = findViewById(R.id.switch_voice_dialer);
        mSwitchVoiceDialer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mBtHeadsetInfo.setVoiceDialOn(mSwitchVoiceDialer.isChecked());
            }
        });

        mBtHeadsetInfo = new BluetoothHeadsetInfo();
        mBtHeadsetInfo.init();

        mSpinnerMode = findViewById(R.id.spinner_mode);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, mModeStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerMode.setAdapter(adapter);
        mSpinnerMode.setOnItemSelectedListener(mModeChanged);
        mCurrentMode = mAudioManager.getMode();
        mSpinnerMode.setSelection(mCurrentMode);

        File outDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        if (outDir.exists())
            outDir.mkdirs();
        mPlayFile = new File(outDir, PLAY_FILE_NAME);
        mRecordFile = new File(outDir, RECORD_FILE_NAME);

        mRecordTask = new AudioRecorderTask(mRecordFile);
        mTrackTask = new AudioTrackTask(mPlayFile);
    }

    @Override
    protected void onDestroy() {
        stop();
        mBtHeadsetInfo.release();

        super.onDestroy();
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

    private AdapterView.OnItemSelectedListener mModeChanged = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (mCurrentMode != position) {
                mCurrentMode = position;
                mAudioManager.setMode(mCurrentMode);

                Mlog.d(TAG, "setMode: " + parent.getSelectedItem());
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

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

            audioTrack = new AudioTrack(STREAM_TYPE,
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

    private class BluetoothHeadsetInfo extends BroadcastReceiver {
        private BluetoothHeadset mBluetoothHeadset;
        private BluetoothDevice mBluetoothHeadsetDevice;
        private boolean mVoiceDialerOn;

        BluetoothHeadsetInfo() {

        }

        private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener =
                new BluetoothProfile.ServiceListener() {
                    @Override
                    public void onServiceConnected(int profile, BluetoothProfile proxy) {
                        mBluetoothHeadset = (BluetoothHeadset) proxy;
                        List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();
                        if (deviceList.size() > 0) {
                            mBluetoothHeadsetDevice = deviceList.get(0);
                        } else {
                            mBluetoothHeadsetDevice = null;
                        }
                    }
                    @Override
                    public void onServiceDisconnected(int profile) {
                        if (mBluetoothHeadset != null) {
                            List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
                            if (devices.size() == 0) {
                                mBluetoothHeadsetDevice = null;
                            }
                            mBluetoothHeadset = null;
                        }
                    }
                };

        void init() {
            mBluetoothHeadsetDevice = null;

            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btAdapter != null) {
                btAdapter.getProfileProxy(MockAudioCall.this, mBluetoothProfileServiceListener,
                        BluetoothProfile.HEADSET);
            }

            IntentFilter intentFilter =
                    new IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
            intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
            registerReceiver(this, intentFilter);

        }

        void release() {
            if (mBluetoothHeadset != null) {
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                if (btAdapter != null) {
                    btAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
                }
            }
        }

        void setVoiceDialOn(boolean on) {
            if (mVoiceDialerOn == on)
                return;

            if (mBluetoothHeadset != null && mBluetoothHeadsetDevice != null) {
                if (on) {
                    mBluetoothHeadset.startVoiceRecognition(mBluetoothHeadsetDevice);
                } else {
                    mBluetoothHeadset.stopVoiceRecognition(mBluetoothHeadsetDevice);
                }
                mVoiceDialerOn = on;
            }
        }

        void setForceScoOn(boolean scoOn) {
            if (mForceScoOn == scoOn)
                return;

            if (scoOn) {
                mAudioManager.startBluetoothSco();
            } else {
                mAudioManager.stopBluetoothSco();
            }
            mForceScoOn = scoOn;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
                Mlog.d(TAG, "ACTION_AUDIO_STATE_CHANGED: " +prevState + " => "+ state);
            } else if (action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
                int currentState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                int prevState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE, -1);
                Mlog.d(TAG, "ACTION_SCO_AUDIO_STATE_UPDATED, state: " + prevState + " => " + currentState);

                if (mForceScoOn && currentState == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    mForceScoOn = false;
                    setForceScoOn(false);
                    Mlog.d(TAG, "stopBluetoothSco");
                }
            }
        }
    }
}
