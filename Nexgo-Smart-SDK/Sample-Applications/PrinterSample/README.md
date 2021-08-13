# PrinterSample
> A sample application for printing receipts on the NEXGO terminals with the SmartSDK.
> 
> The .aar library is already imported/configured inside the project. You can build/install this project as-is to a NEXGO N-series payment terminal from Android Studio directly, or by building an APK and installing it onto the terminal manually. 

## Table of Contents
* [Importing the Sample Project](#importing-the-sample-project)
* [Sample Application Flow in Detail](#sample-application-flow-in-detail)
* [Printing Text](#printing-text)
* [Printing Image](#printing-image)
* [Printing QR Code](#printing-qr-code)
* [Printing Barcode](#printing-barcode)
* [Miscellaneous](#miscellaneous)


## Importing the Sample Project
To download and use the project:
  1. Download the project by downloading the zip directly, or by using the git clone command
  2. Open Android Studio on your computer
  3. Select New > Import Project...
  4. Select the project from the Import dialog window that appears


## Sample Application Flow in Detail

Steps:
1. Get an instance of the `DeviceEngine` object, which is used to get instances of system devices such as the Printer.
```java
   deviceEngine = APIProxy.getDeviceEngine(MainActivity.this);
```
2. Get an instance of the `Printer` object, which we will use to do the receipt printing.
```java
   printer = deviceEngine.getPrinter();
```
3. Inititiate the `printer` object.
```java
   //Initialize the printer
   printer.initPrinter();
```
4. Append content to the printer queue
```java
   //Add sample text on top of the receipt
   printer.appendPrnStr("Panda Karate", 54, AlignEnum.CENTER, true);
```
5. Call `startPrint(..)` to start the print job.
```java
   //Start the print job
   printer.startPrint(true, MainActivity.this);
```

> Note: You must implement the `onPrintListener` in your project. It will be called after the print job is completed if successful, or after attempting to start if there was some issue.

## Printing Text
After initializing the `printer` object, call the `appendPrnStr(..)` function to append text to the printer queue. 

For example, let's add some sample text to print at the top of the receipt:
```java
   //Add sample text on top of receipt
   printer.appendPrnStr("Panda Karate", 54, AlignEnum.CENTER, true);
   printer.appendPrnStr("2251 Wax On Ave", 30, AlignEnum.CENTER, false);
   printer.appendPrnStr("Laguna Hills, CA", 30, AlignEnum.CENTER, false);
```

When you call the `startPrint(..)` function, the text will print along with the other items in the printer queue. 

> Note: You can change the TypeFace (i.e. font) used for printing text with the `setTypeface(..)` function. See the section in Miscellaneous for more information. 

## Printing Image
After initializing the `printer` object, call the `appendImage(..)` function to append an image to the printer queue. 

For example, let's add a Bitmap image to the printer queue:
```java
   Bitmap receiptLogo = //bitmap image
   printer.appendImage(receiptLogo,
                    AlignEnum.CENTER);
```

When you call the `startPrint(..)` function, the image will print along with the other items in the printer queue. 


## Printing QR Code
After initializing the `printer` object, call the `appendQRcode(..)` function to append a QR code to the printer queue. 

For example, let's append a QR Code with the text "Sample QR Code for the PrintSample Application!" to the printer queue:
```java
   //Add QR Code after Panda Logo
   printer.appendQRcode("Sample QR Code for the PrintSample Application!",
            384,
            7,
            3,
            AlignEnum.CENTER);
```

When you call the `startPrint(..)` function, the QR Code will print along with the other items in the printer queue. 


## Printing Barcode
After initializing the `printer` object, call the `appendBarcode(..)` function to append a Barcode to the printer queue. 

For example, let's append a Barcode with value "1234567890" to the printer queue:
```java
   //Add barcode after QR Code (text, height total, spacing, barcode text height)
   printer.appendBarcode("1234567890", 
				100, 
				0, 
				10, 
				BarcodeFormatEnum.CODE_128, 
				AlignEnum.CENTER);
```

When you call the `startPrint(..)` function, the Barcode will print along with the other items in the printer queue. 


## Miscellaneous

### Using External Fonts (i.e. TypeFace not included in system)
By default, you can set the FONT used for printing text using the `printer.setTypeface(Typeface typeface)` function. 

For example, you can use a default font like SANS SERIF by setting the typeface as below:
```java 
  printer.setTypeface(Typeface.SANS_SERIF);

```

However, you will find the default available font list is limited:
```java
  Typeface.DEFAULT
  Typeface.DEFAULT_BOLD
  Typeface.MONOSPACE
  Typeface.SANS_SERIF
  Typeface.SERIF
```

You can use an external font using the steps below:
1. Download the font you want to use
2. Package the font .ttf file into the Android Project's "assets" folder
3. Create a `Typeface` object representing the external font from the font file
4. Set the printer to use the new `Typeface` object
```java
  //Let's use an external font from the file in our 'assets' directory
  AssetManager am = getApplicationContext().getAssets();
  Typeface platNomor = Typeface.createFromAsset(am, "PlatNomor.ttf");
  printer.setTypeface(platNomor);
```

### onPrintListener
When utilizing the printer, you must implement the `onPrintListener` and its `onPrintResult(..)` callback method. You can listen for the result of a print job by handling the result inside the `onPrintResult(..)` method.

In addition to confirming the printer job finished successfully, you can also check if it failed due to running out of paper or some other reason. The easiest way to do so is to check the `resultCode` returned in the `onPrintResult(..)` method using a switch statement:
```java
   @Override
   public void onPrintResult(int resultCode) {
       switch (resultCode){
           case SdkResult.Success:
               Log.d(TAG, "Printer job finished successfully!");
               break;
           case SdkResult.Printer_Print_Fail:
               Log.e(TAG, "Printer Failed: " + resultCode);
               break;
           case SdkResult.Printer_Busy:
               Log.e(TAG, "Printer is Busy: " + resultCode);
               break;
           case SdkResult.Printer_PaperLack:
               Log.e(TAG, "Printer is out of paper: " + resultCode);
               break;
           case SdkResult.Printer_Fault:
               Log.e(TAG, "Printer fault: " + resultCode);
               break;
           case SdkResult.Printer_TooHot:
               Log.e(TAG, "Printer temperature is too hot: " + resultCode);
               break;
           case SdkResult.Printer_UnFinished:
               Log.w(TAG, "Printer job is unfinished: " + resultCode);
               break;
           case SdkResult.Printer_Other_Error:
               Log.e(TAG, "Printer Other_Error: " + resultCode);
               break;
           default:
               Log.e(TAG, "Generic Fail Error: " + resultCode);
               break;
       }
   }
```
