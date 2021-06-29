# EMVSample
> An EMV sample application that is able to read an EMV card for payment processing, including using PIN entry. This sample project only handles EMV ICC transactions, and MSR and NFC transactions are not handled as they are out of scope.
> 
> The .aar library is already imported/configured inside the project. You can build/install this project as-is to a NEXGO N-series payment terminal from Android Studio directly, or by building an APK and installing it onto the terminal manually. 

## Table of Contents
* [Importing the Sample Project](#importing-the-sample-project)
* [EMV Flow](#emv-flow)
* [Miscellaneous](#miscellaneous)


## Importing the Sample Project
To download and use the project:
  1. Download the project by downloading the zip directly, or by using the git clone command
  2. Open Android Studio on your computer
  3. Select New > Import Project...
  4. Select the project from the Import dialog window that appears


## EMV Flow
The low level EMV process is handled by the secure K21 processor inside the NEXGO terminal. The SmartSDK provides APIs to configure and initiate the EMV process. In addition, the API has a number of callback methods that are called by the EMV engine when certain inputs or events are required to be handled by the calling application. After the EMV process finishes, a callback to the onFinish(..) method will denote the result of the EMV process.

For basic EMV applications, you will need to have the following items:
1. DeviceEngine object, used to retrieve instances of the emvHandler and other system resources from the terminal.
2. EmvHandler object, used to manage the EMV process and set EMV parameters during the EMV communication with the card. 
3. CardReader object, used to open the readers on the terminal (i.e. MSR, EMV, NFC) and to listen for card events to begin the EMV process. 
4. If using PIN entry, then also the PinPad object, used to securely show the PinPad UI, retrieve the PIN from the user, and to encrypt the PIN to send to the card/processor for processing. 

For transactions/cards that do not require PIN entry, the basic flow is as follows:
* `onCardInfo(..)`
* `onSelApp(..)`
* `onPrompt(..)`
* `onConfirmCardNo(..)`
* `onProcessOnline(..)`
* `onFinish(..)`

## Sample Application Flow in Detail

Steps:
1. Get an instance of the DeviceEngine object, which is used to get instances of system devices such as the EmvHandler, PinPad, and others.
```java
  //Initialize the deviceEngine object for use in the app
  deviceEngine = APIProxy.getDeviceEngine(MainActivity.this);
```
2. Using the deviceEngine, initialize the emvHandler.
```java
  //Initialize the emvHandler object used to handle emv specific actions
  emvHandler   = deviceEngine.getEmvHandler("app1");
```
3. Using the deviceEngine, initialize the sdkPinPad (PinPad) which is used for handling PIN entry on the device.
```java
  //Initialize the pinPad object used to handle the PIN entry
  sdkPinPad    = deviceEngine.getPinPad();
```
4. Initialize the emvUtils (EmvUtils) helper which is used in various places to help handle tedious tasks.
```java
  //Initialize the emvUtils helper class
  emvUtils     = new EmvUtils(MainActivity.this);
```
5. Initialize the EMV AID lists which the application will use to determine which AIDs are supported, and what features those AIDs contain (such as Online PIN, transaction limits, etc.). 
```java
  //Init the EMV AIDs - emvUtils reads them from inbas_aid.json in assets folder
  initEmvAid();
```
  The initEmvAid() function removes the current AIDs stored in the emvHandler object, and then reads in the list of AIDs (if valid) from the assets folder and stores them in the emvHandler using the .setAidParaList(List<AidEntity>) function:
```java
   /**
     * Initialize the AIDs by reading from the entity list in assets, and using emvUtils to parse
     *  them into the AidEntity array.
     */
    private void initEmvAid() {
        //Remove any+all current AIDs
        emvHandler.delAllAid();
        List<AidEntity> aidEntityList = emvUtils.getAidList();
        if (aidEntityList == null) {
            Log.d(TAG + "_initAid", "initAID failed");
            return;
        }
        //Set the available AID list to the parsed values
        emvHandler.setAidParaList(aidEntityList);
    }
```

> You can implement your own functionality for reading in and handling the AIDs. In the method used in this sample application, the AID list is read in from "inbas_aid.json" file in the assets folder. If for example, you wanted to download an AID list from a remote source for each tranactions, you would need to create your own implementation to do so. 

6. Initialize the EMV CAPK lists into the application from the assets folder:
```java
  //Init the EMV CAPKs - emvUtils reads them from inbas_capk.json in assets folder
  initEmvCapk();
```
The initEmvCapk() functions deletes all the Capks currently stored in the emvHandler object, and then reads in the list of CAPKs (if valid) from the assets folder and stores them in the emvHandler using the .setCAPKList(List<CapkEntity>) function:
```java
   /**
    * Initialize the CAPKs by reading from the entity list in assts, and using the emvUtils to parse
    *  them into the CapkEntity array.
    */
   private void initEmvCapk() {
       //Remove any+all current CAPKs
       emvHandler.delAllCapk();
       List<CapkEntity> capkEntityList = emvUtils.getCapkList();
       if (capkEntityList == null) {
           Log.d(TAG + "_initCapk", "initCAPK failed");
           return;
       }
       //Set the available CAPK list to the parsed values
       emvHandler.setCAPKList(capkEntityList);
   }
```

> You can implement your own functionality for reading in and handling the CAPKs. In the method used in this sample application, the AID list is read in from "inbas_capk.json" file in the assets folder. 
  
7. Implement your own functionality for triggering the EMV process to start. For example, in this Sample - we have a button that when pressed, calls the `startEMVReadTest()` function which begins listening with the CardReader for an EMV card to be inserted.

8. When the user clicks the button, it triggers the `startEMVReadTest()` function. The function does the following:
   * Creates an instance of the CardReader class using the deviceEngine object. The CardReader is responsible for connecting to the various card reader devices on the terminal (i.e. EMV reader, MSR reader, etc.) to detect a card swipe / insert.
   * Creates a HashSet of the `CardSlotEnum` (which "slots" or capture devices, such as MSR and/or ICC) we want to capture
   * Adds the ICC1 `(EMV insert)` slot type to the HashSet
   * Calls the `cardReader.searchCard(..)` function, which will cause the terminal to begin listening for card events on the predetermined slot types. When a card is detected, a callback is made to the functions implemented for OnCardInfoListener. 
   > In this Sample, we implement the OnCardInfoListener directly in the MainActivity.
   
9. After a user inserts the EMV card, the `onCardInfo(int retCode, CardInfoEntity cardInfo)` callback function will be triggered. 
   * We need to check the retCode parameter to determine the result of the card insert (can we read successfully, was there error?)
   * If the card was read successfully, then we can begin the EMV process
   
   Check if the retCode was successful or not. We should also assure the cardInfo returned was not null
   ```java
   if (retCode == SdkResult.Success && cardInfo != null) {
      ...
	  }
   ```
   If not successful, we should prompt the user to insert the card again.
   
After confirming the result was successful at the beginning of `onCardInfo`, we need to build the `EMVTransDataEntity` object that will contain various transaction information to use when we start the EMV process. 
   ```java
      //Build the EmvTransDataEntity object that will be used as input when calling to start
      // the emvProcess
      EmvTransDataEntity transData = new EmvTransDataEntity();
      transData.setB9C((byte) 0x00);
      transData.setTransAmt(leftPad(transAmount, 12, '0'));
      transData.setTermId("00000001");        //Set the terminalid
      transData.setMerId("000000000000001");  //Set the merchantid
      transData.setTransDate(new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new Date())); //set the trans date
      transData.setTransTime(new SimpleDateFormat("hhmmss", Locale.getDefault()).format(new Date())); //set the trans time
      transData.setTraceNo("00000001");       //set the traceno
   ```
> The 'transAmt' field needs to be correctly formatted. We used the helper function `leftPad(..)` in this project to accomplish this.
   
   
After we have configured the transData object, we must set the EMV process type, and EMV channel type. This can be different based on card type and capture mode. Check if the card is EMV CTLS (i.e. NFC) or EMV ICC - and set the configuration accordingly:
```java
       //If the card detected is from NFC (RF) - then the flow needs to be defined as such.
       if (cardInfo.getCardExistslot() == CardSlotTypeEnum.RF) {
           transData.setProcType(EmvTransFlowEnum.QPASS);
           transData.setChannelType(EmvChannelTypeEnum.FROM_PICC);
       } else {
           //Else, if the card detected is not NFC, then flow needs to be normal ICC (EMV)
           transData.setProcType(EmvTransFlowEnum.FULL);
           transData.setChannelType(EmvChannelTypeEnum.FROM_ICC);
       }
```
After setting the ProcType and ChannelType parameters, we need to set the **Terminal Configuration** using the emvHandler object before finally starting the EMV process:
```java
   //9f1a - country code
   //5f2a - currency code
   //9f3c - repeat currency code
   //TLV (9f1a + 5f2a + 9f3c)
   emvHandler.initTermConfig(ByteUtils.hexString2ByteArray("9f1a0208405f2a0208409f3c020840"));
```

Finally, we will start the EMV process by calling `(emvHandler.emvProcess(EmvTransDataEntity data, onEmvProcessListener listener))`:
```java
            //Make the call to begin the actual EMV process
            emvHandler.emvProcess(transData, this);
```

At this point, the EMV process has been triggered - and the terminal will make callbacks to each required function to move through the EMV process. 

10. Typically, the after starting the EMV process, if there are no issues (i.e. no apps on the EMV card) - the Emv Process will call the `onSelApp(..)` callback. In this function, we must read the list of candidate apps on the card, and select one to use for the rest of the Emv Process/Transaction. If you set the boolean **isFirstSelect** to `true`, the function will automatically use the first available AID on the candidate list. 

```java
   //Iterate through the available AIDs on the card and print them to the logcat
   int k = 0;
   for (k = 0; k < appInfoList.size(); k++) {
       Log.d(TAG +"_onSelApp", "appInfoList " + k + ByteUtils.byteArray2HexString(appInfoList.get(k).getAid()));
       Log.d(TAG +"_onSelApp", "appInfoList " + k + ByteUtils.byteArray2HexString(appInfoList.get(k).getAppLabel()));
       Log.d(TAG +"_onSelApp", "appInfoList " + k + appInfoList.get(k).getPriority());
   }

   //Show listView with available AIDs, allow user to choose which 'App' (AID) to use, and then proceed accordingly.
   runOnUiThread(new Runnable() {
       @Override
       public void run() {
           //Build the user prompt for selecting the AID to use from the inserted EMV card
           View dv = getLayoutInflater().inflate(R.layout.dialog_app_list, null);
           final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setView(dv).create();
           ListView lv = (ListView) dv.findViewById(R.id.aidlistView);
           List<Map<String, String>> listItem = new ArrayList<>();
           for (int i = 0; i < appNameList.size(); i++) {
               Map<String, String> map = new HashMap<>();
               map.put("appIdx", (i + 1) + "");
               map.put("appName", appNameList.get(i));
               listItem.add(map);
           }
           SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(),
                   listItem,
                   R.layout.app_list_item,
                   new String[]{"appIdx", "appName"},
                   new int[]{R.id.tv_appIndex, R.id.tv_appName});
           lv.setAdapter(adapter);
           lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
               @Override
               public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                   emvHandler.onSetSelAppResponse(position + 1);
                   alertDialog.dismiss();
                   alertDialog.cancel();
                   Log.d(TAG, "Selected App: [" + position + "]");
               }
           });
           alertDialog.setCanceledOnTouchOutside(false);

           //After building the prompt, and setting the values and onClick actions - show the prompt to user
           alertDialog.show();
       }
      });
```

11. After setting the application to use in `onSelApp(..)`, the Emv Process will call the `onPrompt(..)` callback. For our purposes, we can immediately call the `.onSetPromptResponse(true)` to move to the next step in the Emv Process:
```java
   @Override
    public void onPrompt(PromptEnum promptEnum) {
        Log.d(TAG, "Enter onPrompt()");
        emvHandler.onSetPromptResponse(true);

    }
```

Once a item has been selected, the `emvHandler.onSetSelAppResponse(..)` function *must* be called to tell the Emv Process which AID was selected, and to trigger the next step in the EMV process. 
   
12. The Emv Process will then call the `onConfirmCardNo(final CardInfoEntity cardInfoEntity)` callback which contains the cardInfoEntity object that can be used to extract the card number of EMV card. 
> You should not output the raw PAN to a log, or store it in plaintext. If you have some method that is checking BIN range of card numbers, you could use this method to check if the BIN is supported - and if not exit the EMV process. 
> This is the first time in the EMV process where the raw PAN will be available. Previous calls to get the card number will return blank/null values. 

To continue on in the Emv Process, we must call the `emvHandler.onSetConfirmCardNoResponse(true)` function. 
```java
   //confirm card number so goes to next step
   emvHandler.onSetConfirmCardNoResponse(true);
```

13. At this point, the Emv Process can go one of two ways:
* If card card requires PIN entry, we must enter the PIN into the terminal. 
  * In this case, the Emv Process will trigger the `onCardHolderInputPin(...)` callback which will initiate and handle the process of receiving the PIN from the user. 
* If the card does not require PIN entry, we can begin processing the card online immediately. 
  * In this case, the Emv Process will trigger the `onOnlineProc()` callback. 
  
14. (Skip to step 15 if the card does not require PIN entry) If the card requires PIN entry, then we must handle receiving the PIN from the user. In this case, the Emv Process will automatically call the `onCardHolderInputPin(..)` callback. 

In this sample, I have handled this section with my own helper method `startInputPin(boolean isOnlinePin)`:
```java
   @Override
   public void onCardHolderInputPin(boolean isOnlinePin, int leftTimes) {
       Log.d(TAG, "Enter onCardHolderInputPIN()");

       startInputPin(true);
   }
```

Let's take a look at the `startInputPin(..)` method...

First, in order to use PIN - we need to ensure that a PIN key is injected into the terminal. This key will be used to encrypt the PIN when sending to the processor online. Usually, in our terminals - the PIN key is injected into 'slot 0':
```java
   //slot '0' is where the normal PIN keys are injected, and should be used unless programming for P2PE
   int INJECTED_PIN_SLOT = 0;  
```

We need to check that there *is* a PIN key injected before proceeding. If there is **not** a key injected, we must cancel the emv transaction and the Emv Process using the `emvHandler.ProcessCancel()` function.
```java
   //Check if there is a key injected into the terminal (it is required for PIN entry)
   if (sdkPinPad.dukptCurrentKsn(INJECTED_PIN_SLOT) == null) {
       //There is no key injected; cannot continue - show some error to user and break out
       Log.e(TAG, "startInputPin() : cannot continue, No key is injected!");
       emvHandler.emvProcessCancel();  //Stop the EMV process, cannot proceed to enter PIN without the injected key
       return;
   }
```

If our check for the PIN key was successful, we can move on.

We need to create a int[] array that contains all the valid PIN lengths we will accept. Normally, PIN is 4 digits, however PIN Bypass would mean PIN length 0 - and some banks allow PINs of various lengths.
```java
   /**
    *  Create and array with the valid PIN entry lengths; will pass to PINPAD shortly as an argument
    *
    *  Note: length '0' would be for PIN bypass scenario
    */
    int[] pinLen = new int[]{0, 4, 5, 6, 7, 8, 9, 10, 11, 12};
```

Next, we need to set the PinPad to the correct `AlgorithmModeEnum` with the `sdkPinPad.setAlgorithmMode(..)` function:
```java
   //Set the PINPAD algorithm mode - we want to use DUKPT
   sdkPinPad.setAlgorithmMode(AlgorithmModeEnum.DUKPT);
```

Finally, we need to prompt show the PinPad to prompt the user for the PIN. Depending on if **isOnlinePin** was true or false in the method constructor - we need to handle differently. Online PIN entry is different than Offline PIN entry. 

First, for **ONLINE** PIN entry, we would do the following:
Create a byte[] array cardNumBytes that will be the byte[] representation of the card's PAN. 
```java
   //Create byte array of the card number - we will pass it into the inputOnlinePin function
   byte[] cardNumBytes = getPAN().getBytes();
```

Next, we will call the `sdkPinPad.inputOnlinePin(..)` method and save the result code to and int, in this case `inputPinResult`:
```java
   inputPinResult = sdkPinPad.inputOnlinePin(pinLen,           //array of acceptable PIN lengths
                              60,                              //Pinpad timeout in seconds
                              cardNumBytes,                    //The card number's PAN in byte[] format
                              INJECTED_PIN_SLOT,               //The injection/key slot to use to encrypt the PIN
                              PinAlgorithmModeEnum.ISO9564FMT1,//The algorithm to use for encrypting PIN
                              this);                           //The onPinPadInputListener listener, 'this'
```

Alternatively, for **OFFLINE** PIN entry, we would do the following:
Create a byte[] array cardNumBytes that will be the byte[] representation of the card's PAN. 
```java
   //Create byte array of the card number - we will pass it into the inputOnlinePin function
   byte[] cardNumBytes = getPAN().getBytes();
```
Next, we will call the `sdkPinPad.inputOfflinePin(..)` method and save the result code to and int, in this case `inputPinResult`:
```java
  inputPinResult = sdkPinPad.inputOfflinePin(pinLen,           //array of acceptable PIN lengths
                              60,                              //Pinpad timeout in seconds
                              this);                           //The onPinPadInputListener listener, 'this'
```

For both *ONLINE* and *OFFLINE* PIN, we should check afterwards to check for failures. If successful, it the Process will move on automatically, however we should handle failure case and show some message to the user. 
```java
   //If PIN input is not success, process input PIN failed
   if (inputPinResult != SdkResult.Success)
   {
       Log.e(TAG, "Process finished input pin failed!");
       runOnUiThread(new Runnable() {
       @Override
       public void run() {
           Toast.makeText(getApplicationContext(), "PIN Input Process Failed", Toast.LENGTH_SHORT).show();
           }
       });
   }
```

After the PIN has been entered (i.e. PINPAD was dismissed by ENTER, CANCEL, etc.) - the `onInputResult(..)` callback function will be triggered. This callback will return the encrypted PIN block in a byte[] array. We should set a variable of the PIN block as a Hex String in uppercase, and a variable of the PIN KSN as a Hex String in Uppercase. Then increment the KSN counter using the `sdkPinPad.dukptKsnIncrease(0)` function.
```java
   //Set the pinBlockArray (String) to the return value 'data' (PIN output) for sending to host
   pinBlockArray = ByteUtils.byteArray2HexString(data).toUpperCase();                              
   
   //Save the pinKsn in case needed to send to host
   pinKsn        = ByteUtils.byteArray2HexString(sdkPinPad.dukptCurrentKsn(0)).toUpperCase();   

   //Incremenent the KSN counter
   sdkPinPad.dukptKsnIncrease(0);
```

Finally, to move to the next step in the EMV process, call the `emvHandler.onSetPinInputResponse(..)` function:
```java
   //(var1 = whether valid input / success, var2 = pin bypass)
   emvHandler.onSetPinInputResponse(retCode == SdkResult.Success || retCode == SdkResult.PinPad_No_Pin_Input, isPinBypass);
```

15. At this point, we have done the main interaction with the user and the card. We now will proceed to actually sending the transaction online to the processor for authorization. The Emv Process will automatically trigger the `onOnlineProc()` function. 

Create an `EmvOnlineResultEntity` used to store transaction result and possibly do second gen AC on card later on
```java
   //Build the EMVOnlineResultEntity object used to store the transaction result from the processor.
   EmvOnlineResultEntity emvOnlineResult = new EmvOnlineResultEntity();
```		

Process the transaction online, sending whatever information is requested by the processor. You should have access to all the tags, card information, etc. still in this function. 

After the transaction is processed, handle the authorization result. 

If it was **APPROVED**, we need to set the AuthCode:
```java
   //Set the 'AuthCode' to the authorization code returned in the host response (i.e. 'OK9790'), and the 'RejCode' to '00'
 
   emvOnlineResult.setAuthCode("OK9790");
   emvOnlineResult.setRejCode("00");
```

If it was **Declined**, we need to set the rej code:
```java
   //Don't set the 'AuthCode', and do set the 'RejCode' to '05' unless processor has other rejection code....
        //emvOnlineResult.setRejCode("05");
```

Then, we need to provide the field 55 response to the Emv Process to do second auth if required:
```java
   //fill with the server response 55 field EMV data to do second auth
   //Fill setRecvField55 to (TAG91 + TAG71 + TAG72) in TLV format ...
   emvOnlineResult.setRecvField55(ByteUtils.hexString2ByteArray(TAGS));
```

Finally, to complete the EMV Process, we need to call the `emvHandler.onSetOnlineProcResponse(..)` function. 
```java
   emvHandler.onSetOnlineProcResponse(SdkResult.Success, emvOnlineResult);
```

16. After the EMV Process completes, the `onFinish` callback method will be triggered. From it, you can check the result and handle accordingly.
```java
  //Parse the retCode to see what the result was
        switch (retCode) {
            case SdkResult.Emv_Success_Arpc_Fail:
            case SdkResult.Success:
                //online approve
                Log.d(TAG, "SUCCESS");
                break;

            case SdkResult.Emv_Qpboc_Offline:// EMV Contactless: Offline Approval
            case SdkResult.Emv_Offline_Accept://EMV Contact: Offline Approval
                //offline approve
                Log.d(TAG, "OFFLINE APPROVE");
                break;

            case SdkResult.Emv_Qpboc_Online://EMV Contactless: Online Process for union pay
                //union pay online contactless--application should go online

                break;

            case SdkResult.Emv_Candidatelist_Empty:// Application have no aid list No
            case SdkResult.Emv_FallBack://  FallBack ,chip card reset failed
                //fallback process
                Log.w(TAG, "NO AID or CHIP RESET / FALLBACK FAILED");
                break;

            case SdkResult.Emv_Arpc_Fail: //
            case SdkResult.Emv_Script_Fail:
            case SdkResult.Emv_Declined:
                //emv decline, if it is in second gac, application should decide if it is need reversal the transaction
                Log.w(TAG, "DECLINED");
                break;

            case SdkResult.Emv_Cancel:// Transaction Cancel
                //user cancel
                Log.w(TAG, "TRANS EMV CANCELLED");
                break;

            case SdkResult.Emv_Offline_Declined: //
                //offline decline
                Log.w(TAG, "OFFLINE DECLINED");
                break;

            case SdkResult.Emv_Card_Block: //Card Block
                //card is blocked
                Log.e(TAG, "CARD BLOCKED");

                break;

            case SdkResult.Emv_App_Block: // Application Block
                //card application block
                Log.e(TAG, "CARD APP BLOCKED");
                break;

            case SdkResult.Emv_App_Ineffect:
                //card not active
                break;

            case SdkResult.Emv_App_Expired:
                //card Expired
                Log.w(TAG, "CARD IS EXPIRED");
                break;

            case SdkResult.Emv_Other_Interface:
                //try other entry mode, like contact or mag-stripe
                Log.w(TAG, "TRY OTHER CAPTURE INTERFACE");
                break;

            case SdkResult.Emv_Plz_See_Phone:
                //see phone flow
                //prompt a dialog to user to check phone-->search contactless card(another card) -->start emvprocess
                Log.w(TAG, "CHECK PHONE");
                break;

            case SdkResult.Emv_Terminate:
                //transaction terminate
                Log.w(TAG, "TRANS TERMINATED");
                break;

            default:
                //other error
                Log.e(TAG, "GENERAL ERROR");
                break;
        }
```


## Miscellaneous

### PIN Entry Considerations
If you are familiar with the payment industry, you will note that some cards require PIN, and some do not. This is dependent on the AIDs supported by both the terminal and the EMV card. 

* Most US Credit Cards do not require PIN entry, and can be processed using a signature only. Most US Debit Cards, contain AIDs that allow it to either process using a PIN, or process using a signature - depending on the transaction type and AID selected during the EMV process. Essentially, most US based payment cards can be processed without a PIN DUKPT key injected, and without requiring PIN entry from the customer. 
* Most non-US based payment cards however do not contain the dual functionality, and a foreign Debit card will almost always require a PIN to be entered. Thus, if you do not inject the terminal and implement the PIN input handling, you will likely find yourself unable to process many foriegn payment cards. 

This project assumes the terminal is 'injected' with a DUKPT PIN key. If there is no 'key' injected into the terminal, and the card you are testing with requires it - then the EMV process will be cancelled inside the startInputPin(..) function:
```java
   int INJECTED_PIN_SLOT = 0;  //slot '0' is where the normal PIN keys are injected, and should be used unless programming for P2PE
 
   //Check if there is a key injected into the terminal (it is required for PIN entry)
   if (sdkPinPad.dukptCurrentKsn(INJECTED_PIN_SLOT) == null) {
       //There is no key injected; cannot continue - show some error to user and break out
       Log.e(TAG, "startInputPin() : cannot continue, No key is injected!");
       emvHandler.emvProcessCancel();  //Stop the EMV process, cannot proceed to enter PIN without the injected key
       return;
   }
``` 

### Enabling EMV kernal debug output
Separate from the normal application logcat output, there is an option to enable/disable EMV kernal log output. This log output provides a deeper look into what the EMV engine is doing under the hood, such as when reading tags, etc. 

You can set this additional logging enabled/disabled by using the .emvDebugLog(boolean enabled) function on an initialized EMV Handler object like below:
```java
  //Initialize the emvHandler object used to handle emv specific actions
  emvHandler   = deviceEngine.getEmvHandler("app1");

  //Set the EMV debug log whether we want to see EMV kernel output when processing
  emvHandler.emvDebugLog(false);
```
