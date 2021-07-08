package com.nexgo.camerascannersample;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.SdkResult;
import com.nexgo.oaf.apiv3.device.scanner.OnScannerListener;
import com.nexgo.oaf.apiv3.device.scanner.Scanner;
import com.nexgo.oaf.apiv3.device.scanner.ScannerCfgEntity;

public class MainActivity extends AppCompatActivity implements OnScannerListener {

    public final String TAG = "MainActivity";
    private DeviceEngine deviceEngine;
    private Scanner scanner;
    private Button startScannerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize the SDK components
        deviceEngine = APIProxy.getDeviceEngine(MainActivity.this);
        scanner = deviceEngine.getScanner();

        //Initialize the UI button to start the scanning procedure
        startScannerButton = (Button) findViewById(R.id.startScannerButton);

        //Configure the ScannerCfgEntity to pass to initScanner(..) function
        final ScannerCfgEntity scannerConfigEntity = new ScannerCfgEntity();
        scannerConfigEntity.setAutoFocus(true);
        scannerConfigEntity.setUsedFrontCcd(false);
        scannerConfigEntity.setBulkMode(false);
        scannerConfigEntity.setInterval(1000);

        //Set the button action --> start scanning when user clicks the UI button
        startScannerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Calling initScanner(..)");
                scanner.initScanner(scannerConfigEntity, MainActivity.this);
            }
        });
    }

    /**
     * The scanner must be initialized before the scanner can be used - i.e. before startScan(..) can be called.
     *
     * After attempting to initialize the scanner, it will call this method to return the result. If successful,
     * you can begin the scanning. Otherwise, you should handle the error.
     *
     * @param resultCode - the result of the scanner init (i.e. SdkResult.*)
     */
    @Override
    public void onInitResult(int resultCode) {
        switch (resultCode) {
            case SdkResult.Success:
                Log.d(TAG, "Scanner Init Success");
                Log.d(TAG, "Calling startScan(..)");

                //Since the scanner was initialized successfuly, we can call the scanner.startScan(..) function to begin the 'scan'.
                scanner.startScan(60, MainActivity.this);
                break;
            default:
                Log.e(TAG, "Scanner Failed to Init: " + resultCode);
                break;
        }
    }

    /**
     * This callback method is called by the Scanner once it completes a scanning action, whether successful or not.
     *
     * You should code the required functionality into this method to handle the result accordingly.
     *
     * @param resultCode - the result of the scan (i.e. SdkResult.*)
     * @param s - The decoded string that was scanned by the scanner
     */
    @Override
    public void onScannerResult(int resultCode, final String s) {
        switch (resultCode) {
            case SdkResult.Success:
                Log.d(TAG, "Got Code: " + s);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAlertDialog("Scan Success",
                                "Barcode:  " + s,
                                MainActivity.this);
                    }
                });
                break;
            case SdkResult.Scanner_Customer_Exit:
                Log.d(TAG, "Requested Exit Scanner OK.");
                break;
            case SdkResult.Scanner_Other_Error:
                Log.e(TAG, "Got Scanner Error OtherError");
                break;
            default:
                Log.e(TAG, "Other general error.");
        }
        scanner.stopScan();
    }

    /**
     * This function is used to display text in an 'AlertDialog' displayed to the user.
     * <p>
     * In this sample application, it is used to display the decoded barcode from the scan.
     *
     * @param body - The text to be displayed in the AlertDialog.
     */
    public void showAlertDialog(String title, String body, final Context context) {
        new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setTitle(title)
                .setMessage(body)
                .show();
    }
}
