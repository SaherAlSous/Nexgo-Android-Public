package com.nexgo.camerascannersample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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

    @Override
    public void onInitResult(int resultCode) {
        switch (resultCode) {
            case SdkResult.Success:
                Log.d(TAG, "Scanner Init Success");
                Log.d(TAG, "Calling startScan(..)");
                scanner.startScan(60, MainActivity.this);
                break;
            default:
                Log.e(TAG, "Scanner Failed to Init: " + resultCode);
                break;
        }
    }

    @Override
    public void onScannerResult(int resultCode, String s) {
        switch (resultCode) {
            case SdkResult.Success:
                Log.d(TAG, "Got Code: " + s);
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
}
