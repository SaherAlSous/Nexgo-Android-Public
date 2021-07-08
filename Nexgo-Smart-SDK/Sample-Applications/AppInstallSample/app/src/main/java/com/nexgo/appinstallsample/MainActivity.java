
package com.nexgo.appinstallsample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.OnAppOperatListener;
import com.nexgo.oaf.apiv3.SdkResult;

import java.io.File;

/*
 * @author hayden morris 2021
 *
 * Sample application for showing how to install an APK from a file using the SmartSDK.
 * Note: RELEASE MODE devices still require a Nexgo signature to install APKs with this method.
 *
 * Use case would be if somebody wanted to download an APK over internet or USB, and then install it
 *  onto the device. For example a custom app store, or custom deployment application.
 *
 * You need to include the permissions in the MANIFEST for reading from the filesystem!!
 */

public class MainActivity extends AppCompatActivity implements OnAppOperatListener {

    private final String TAG = "MainActivity";
    private DeviceEngine deviceEngine;
    private String apkPath = "/sdcard/NexInformation.apk"; //The path of the APK we want to install (change accordingly)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceEngine = APIProxy.getDeviceEngine(MainActivity.this);

        //Check the file, and if ok - then init the install process
        File apkFile = new File(apkPath);
        if (!apkFile.exists())
            Log.e(TAG, "APK file does not exist @ " + apkFile.getAbsolutePath());
        else {
            if (apkFile.isFile() && apkFile.canRead()) {
                Log.w(TAG, "Found file @ " + apkFile.getAbsolutePath() + "; attempting installation...");

                //Call the SDK to install the application at the path specified, and use this class as callback
                deviceEngine.installApp(apkFile.getAbsolutePath(),
                        this);
            } else {
                Log.e(TAG, "Path is not a file, or it cannot be read.");
            }
        }
    }


    /**
     * After the installation process has been attempted, the SDK will call this method to notify the
     * application of the result of the installation attempt (if it failed or was successful)
     *
     * @param result - the result of the installation process (i.e. SdkResult.Success is what we want)
     */
    @Override
    public void onOperatResult(int result) {
        switch (result) {
            case SdkResult.Success:
                Log.d(TAG, "Installation successful.");
                break;
            default:
                Log.e(TAG, "Installation failed. Check log.");
                break;
        }
    }
}