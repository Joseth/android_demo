package com.joseth.demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.List;

public class CheckPermissionsActivity extends AppCompatActivity {

    protected String[] needPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private static final int PERMISSON_REQUESTCODE = 0;

    private boolean isNeedCheck = true;

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.M) {
            if (isNeedCheck) {
                checkPermissions(needPermissions);
            }
        } else {
            onAllPermissionGranted();
        }
    }

    private void checkPermissions(String[] permissions) {
        try {
            List<String> deniedList = findDeniedPermissions(permissions);

            if (null != deniedList && deniedList.size() > 0) {
                String[] array = deniedList.toArray(new String[deniedList.size()]);

                requestPermissions(array, PERMISSON_REQUESTCODE);
            } else {
                onAllPermissionGranted();
            }
        } catch (Throwable e) {
        }
    }

    private List<String> findDeniedPermissions(String[] permissions) {
        List<String> deniedList = new ArrayList<String>();

        try {
            for (String perm : permissions) {
                if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED
                        || shouldShowRequestPermissionRationale(perm)) {
                    deniedList.add(perm);
                }
            }
        } catch (Throwable e) {

        }

        return deniedList;
    }

    private boolean verifyPermissions(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] paramArrayOfInt) {
        if (requestCode == PERMISSON_REQUESTCODE) {
            boolean allGranted = verifyPermissions(paramArrayOfInt);

            if (!allGranted) {
                showMissingPermissionDialog();
            } else {
                onAllPermissionGranted();
            }
            isNeedCheck = false;
        }
    }

    protected void onAllPermissionGranted() {
         // Nothing to do
    }

    private void showMissingPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.notifyTitle);
        builder.setMessage(R.string.notifyMsg);

        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

        builder.setPositiveButton(R.string.setting,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startAppSettings();
                    }
                });

        builder.setCancelable(false);

        builder.show();
    }

    /**
     * 启动应用的设置
     *
     * @since 2.5.0
     */
    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
