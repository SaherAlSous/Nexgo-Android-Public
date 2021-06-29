# CameraScannerSample
> A sample application for reading a barcode using the Nexgo terminal's camera with the SmartSDK.
> 
> The .aar library is already imported/configured inside the project. You can build/install this project as-is to a NEXGO N-series payment terminal from Android Studio directly, or by building an APK and installing it onto the terminal manually. 

## Table of Contents
* [Importing the Sample Project](#importing-the-sample-project)
* [Sample Application Flow in Detail](#sample-application-flow-in-detail)
* [Miscellaneous](#miscellaneous)


## Importing the Sample Project
To download and use the project:
  1. Download the project by downloading the zip directly, or by using the git clone command
  2. Open Android Studio on your computer
  3. Select New > Import Project...
  4. Select the project from the Import dialog window that appears


## Sample Application Flow in Detail

Steps:
1. Get an instance of the `DeviceEngine` object, which is used to get instances of system devices such as the Scanner.
```java
  deviceEngine = APIProxy.getDeviceEngine(MainActivity.this);
```
2. Get an instance of the `Scanner` object, which we will use to do the barcode scanning.
```java
  scanner = deviceEngine.getScanner();
```
3. Configure the `ScannerCfgEntity` object that is passed to the `Scanner` object when calling the `initScanner(..)` function
```java
  //Configure the ScannerCfgEntity to pass to initScanner(..) function
  final ScannerCfgEntity scannerConfigEntity = new ScannerCfgEntity();
  scannerConfigEntity.setAutoFocus(true);
  scannerConfigEntity.setUsedFrontCcd(false);
  scannerConfigEntity.setBulkMode(false);
  scannerConfigEntity.setInterval(1000);
```

4. Call the `scanner.initScanner(ScannerCfgEntity scannerConfigEntity, onScannerListener listener)` function to initialize the Scanner object.
```java
  scanner.initScanner(scannerConfigEntity, MainActivity.this);
```

5. After calling the `initScanner(..)` function, the callback function `onInitResult(int resultCode)` will be called. From here, we can determine whether or not the scanner was initialized properly or not. If so, we can call the `startScan(..)` function to begin scanning for barcodes.
```java
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
```

6. After you call the 'startScanner(..)` function, the camera will appear and scan for barcodes. After the camera scanner exits (from getting barcode, error, or cancel) - the callback function `onScannerResult(int resultCode, String s)` will be called. From here, we can determine whether or not the scanner completed successfully. If so, we can get the barcode as a String - otherwise can handle the error. 
```java
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
```

## Miscellaneous

* Each time you use the scanner object, you should call the stopScan() function after getting a result in `onScannerResult(..)` - even if the result is an error. 
* Each time you begin a 'new' scan, you *must* go through the `initScanner(..)` function/flow again. 
