package com.nexgo.emvsample;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.nexgo.common.ByteUtils;
import com.nexgo.common.TlvUtils;
import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.SdkResult;
import com.nexgo.oaf.apiv3.device.led.LEDDriver;
import com.nexgo.oaf.apiv3.device.led.LightModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.AlgorithmModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.OnPinPadInputListener;
import com.nexgo.oaf.apiv3.device.pinpad.PinAlgorithmModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.PinPad;
import com.nexgo.oaf.apiv3.device.pinpad.PinPadKeyCode;
import com.nexgo.oaf.apiv3.device.reader.CardInfoEntity;
import com.nexgo.oaf.apiv3.device.reader.CardReader;
import com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum;
import com.nexgo.oaf.apiv3.device.reader.OnCardInfoListener;
import com.nexgo.oaf.apiv3.emv.AidEntity;
import com.nexgo.oaf.apiv3.emv.CandidateAppInfoEntity;
import com.nexgo.oaf.apiv3.emv.CapkEntity;
import com.nexgo.oaf.apiv3.emv.EmvChannelTypeEnum;
import com.nexgo.oaf.apiv3.emv.EmvDataSourceEnum;
import com.nexgo.oaf.apiv3.emv.EmvHandler;
import com.nexgo.oaf.apiv3.emv.EmvOnlineResultEntity;
import com.nexgo.oaf.apiv3.emv.EmvProcessResultEntity;
import com.nexgo.oaf.apiv3.emv.EmvTransDataEntity;
import com.nexgo.oaf.apiv3.emv.EmvTransFlowEnum;
import com.nexgo.oaf.apiv3.emv.OnEmvProcessListener;
import com.nexgo.oaf.apiv3.emv.PromptEnum;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnCardInfoListener, OnEmvProcessListener, OnPinPadInputListener {

    static final String TAG = "MainEMVAct";     //The String that is output as the 'TAG' in logcat
    final boolean       emvDebugOutput = false; //Enable/disable verbose emv kernel output

    private Button startEmvTestButton;          //Button we will use to start the EMV process from the UI

    private DeviceEngine deviceEngine;          //DeviceEngine used for retrieving the emvHandler and other device device objects (ex. CardReader)
    private EmvHandler   emvHandler;            //The EmvHandler is used for managing the emv process through a transaction on the terminal

    private PinPad       sdkPinPad;             //The PinPad object is used for retrieving the PIN from the user
    private int          inputPinResult = -1;   //The pinpad return value; used to determine PIN process successful/failure, etc.
    private String       pinBlockArray;
    private String       pinKsn;

    private EmvUtils     emvUtils;              //The helper class used during the emv process (i.e. tools for parsing AID, CAPK, etc.)
    private String       transAmount = "5000";  //Transaction amount, 2 digits offset (i.e. 50.00 --> 5000)
    private ProgressDialog progressDialog;      //ProgressDialog to prevent user from starting another readCard(..) process until the current one is finished

    private List<String> mSelAppList;           //The List<String> of the AIDs on the card; null until populated in onSelAPp

    private boolean  mAutoSelectCommonDebit = false;     //If true, function will auto-select first 'common debit' AID found on this list if available
    private String[] mCommonDebitList = new String[]{   //Array of AIDs representing the Common Debit AID for most common/popular card brands
            "A0000000980840",           //VI COMMON DEBIT
            "A0000001524010",           //DSC COMMON DEBIT
            "A000000333010108",         //UNIONPAY COMMON DEBIT
            "A0000006200620",           //DNA COMMON DEBIT
            "A0000000042203"            //MC COMMON DEBIT
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize the deviceEngine object for use in the app
        deviceEngine = APIProxy.getDeviceEngine(MainActivity.this);

        //Initialize the emvHandler object used to handle emv specific actions
        emvHandler   = deviceEngine.getEmvHandler("app1");

        //Set the EMV debug log whether we want to see EMV kernel output when processing
        emvHandler.emvDebugLog(emvDebugOutput);

        //Initialize the pinPad object used to handle the PIN entry
        sdkPinPad    = deviceEngine.getPinPad();

        //Initialize the emvUtils helper class
        emvUtils     = new EmvUtils(MainActivity.this);

        //Init the EMV AIDs - emvUtils reads them from inbas_aid.json in assets folder
        initEmvAid();

        //Init the EMV CAPKs - emvUtils reads them from inbas_capk.json in assets folder
        initEmvCapk();

        //Init progress dialog to show something on screen when doing EMV process
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Processing...");

        setLEDs("A");

        //Create + init button to start the EMV process
        startEmvTestButton = (Button) findViewById(R.id.goButton);
        startEmvTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Start the EMV test process to begin reading the card
                startEMVReadTest();
            }
        });

    }

    /**
     *  startEMVReadTest()
     *
     *  This function is called when the Start EMV Test button is pressed. Inside, it initializes the card reader,
     *   SlotTypes (allowed capture methods i.e. swipe, NFC, etc.), and begins the process for listening for a
     *   card event (i.e. ICC/EMV card insert)
     *
     */
    private void startEMVReadTest()
    {

        Log.d(TAG + "_startEMVTest", "entered startEMVReadTest()");
        setLEDs("aY");

        //Init the cardReader object by retrieving an instance from the deviceEngine
        CardReader cardReader = deviceEngine.getCardReader();

        //Create HashSet of types of capture methods we are listening for
        HashSet<CardSlotTypeEnum> slotTypes = new HashSet<>();

        //For EMV sample, we only want internal EMV, which is ICC1. Add ICC1 to HashSet of capture methods to listen for
        //Other capture methods like Swipe will be ignored
        slotTypes.add(CardSlotTypeEnum.ICC1);

        //Call the searchCard method (HashSet of capture types, listen timeout in seconds, which onCardInfoListener callback to use)
        cardReader.searchCard(slotTypes, 60, this);

        //Show toast to user to insert the EMV card
        Toast.makeText(getApplicationContext(), "Please Insert Card.", Toast.LENGTH_SHORT).show();

        //Shows a ProgressDialog to inform user we're processing, and to prevent them from clicking button again
        progressDialog.show();
    }

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

    /**
     * Helper function.
     *
     * Pad the ammount in the correct format
     *
     * @param amountStr The amount string. No decimal places should be present, and two digits should be reserved for 'cents' (i.e. $50.00 == 5000)
     * @param size the final size/length of the amount string; function will pad with correct number of digits to meet this parameter.
     * @param padChar the character to 'pad' with. (i.e. '0' will pad the digits with 0's (00000005)
     * @return the correctly formatted/padded transaction amont string.
     */
    private String leftPad(String amountStr, int size, char padChar) {
        Log.d(TAG, "Enter leftPad()");
        Log.d(TAG, "Initial Amount Str[" + amountStr + "] : final string size[" + size + "] : padChar[" + padChar + "]");

        StringBuilder padded = new StringBuilder(amountStr == null ? "" : amountStr);
        while (padded.length() < size) {
            padded.insert(0, padChar);
        }
        Log.d(TAG, "Final padded string: " + padded.toString());
        return padded.toString();
    }

    /**
     * Callback method.
     *
     * This function gets called after the PINPAD is dismissed (i.e. cancelled, entered PIN, etc.)
     *
     * @param retCode SdkResult from the PINPAD input (Success, Cancel, Nopin, other error, etc.)
     * @param data PINBLOCK byte array (encrypted PIN)
     */
    @Override
    public void onInputResult(int retCode, byte[] data) {
        Log.d(TAG + "_OnInpResult", "Enter onInputResult() : retCode" + retCode);
        Log.d(TAG + "_OnInpResult", "Encrypted Pin Block (EPB) = " + ByteUtils.byteArray2HexStringWithSpace(data));

        boolean isPinBypass = false;

        //Check the return code and handle accordingly
        switch (retCode)
        {
            //PinPad process exited success
            case SdkResult.Success:
                Log.d(TAG, "PinPad Result Success");
                //Data should not but null if didn't cancel out of process and wasn't using PIN bypass
                if (data != null)
                {
                    pinBlockArray = ByteUtils.byteArray2HexString(data).toUpperCase();                              //Set the pinBlockArray (String) to the return value 'data' (PIN output) for sending to host
                    pinKsn        = ByteUtils.byteArray2HexString(sdkPinPad.dukptCurrentKsn(0)).toUpperCase();   //Save the pinKsn in case needed to send to host

                    //Incremenent the KSN counter
                    sdkPinPad.dukptKsnIncrease(0);
                }
                break;
            //PinPad exited with no PIN -> PIN Bypassed
            case SdkResult.PinPad_No_Pin_Input:
                Log.d(TAG, "PinPad Result NO_PIN_INPUT -> Bypass");
                isPinBypass = true;
                break;
            //PinPad process exited not success, but because user clicked CANCEL button on PinPad
            case SdkResult.PinPad_Input_Cancel:
                Log.w(TAG, "PinPad Result USER CANCELLED");
                break;
            default:
                //SdkResult.PinPad* contains the various PinPad error codes you can check if you want
                Log.e(TAG, "PinPad Result NOT success. retCode = " + retCode);
        }

        //(var1 = whether valid input / success, var2 = pin bypass)
        emvHandler.onSetPinInputResponse(retCode == SdkResult.Success || retCode == SdkResult.PinPad_No_Pin_Input, isPinBypass);

    }

    /**
     * This callback is called by the onPinpadInputListener(..) function each time the user enters a
     *  digit of their PIN.
     *
     *  You will not get the return value of the DIGIT - but can either check if CLEAR button is pressed or whether a new valid digit was pressed.
     *  This allows you to do two things if you are showing a PIN screen UI
     *      1 - If a digit is pressed (i.e. 1,2,3...) then PinPadKeyCode.KEYCODE_STAR is returned in this function. In this case, you can add a * character to your PIN UI display for the user
     *      2 - If the CLEAR button is pressed on the PinPad (i.e. restart the PIN entry), then PinPadCode.KEYCODE_STAR is returned in this function. In this case, you can clear the PIN UI display for the user
     *
     *  Essentially, this callback allows you to know when to either append *'s to your PIN UI entry, or when to clear the PIN UI entry.
     *
     * @param keyCode - the raw key of the button they've pressed
     */
    @Override
    public void onSendKey(final byte keyCode) {
        Log.d(TAG, "Enter onSendKey() : keycode byte = " + keyCode);

        switch (keyCode) {
            case PinPadKeyCode.KEYCODE_CLEAR:
                //mHandler.post(() -> showPin(PinCode.CLEAR));
                break;
            case PinPadKeyCode.KEYCODE_STAR:
                //mHandler.post(() -> showPin(PinCode.STAR));
                break;
            default:
                break;
        }

    }

    /**
     * Callback function.
     *
     * Once searchCard(..) is called, this callback is called by the EMV kernel once one of the defined
     *  capture methods detects a card (i.e. SWIPE / NFC / ICC).
     *
     *  If there are no errors, you should next manually call the emvProcess(..) from the initialized EmvHandler
     *  object. After the EmvHandler.emvProcess(..) has been started, the EMV kernel will use the onEmvProcessListener(..)
     *  to callback the various methods required for the EMV process.
     *
     *
     * @param retCode the return code of the onCardInfo(..) action, denoting the result. Codes are from SdkResult.*
     * @param cardInfo the card object containing the reference to the card detected by the reader.
     */
    @Override
    public void onCardInfo(int retCode, CardInfoEntity cardInfo) {
        Log.d(TAG, "Enter onCardInfo() : retCode" + retCode);

        //Check if return code is success, and the returned cardInfo object is not null
        if (retCode == SdkResult.Success && cardInfo != null) {

            //success and cardInfo is not null

            //Build the EmvTransDataEntity object that will be used as input when calling to start
            // the emvProcess
            EmvTransDataEntity emvTransDataEntity = new EmvTransDataEntity();
            emvTransDataEntity.setB9C((byte) 0x00);
            emvTransDataEntity.setTransAmt(leftPad(transAmount, 12, '0'));
            emvTransDataEntity.setTermId("00000001");        //Set the terminalid
            emvTransDataEntity.setMerId("000000000000001");  //Set the merchantid
            emvTransDataEntity.setTransDate(new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new Date())); //set the trans date
            emvTransDataEntity.setTransTime(new SimpleDateFormat("hhmmss", Locale.getDefault()).format(new Date())); //set the trans time
            emvTransDataEntity.setTraceNo("00000001");       //set the traceno


            //If the card detected is from NFC (RF) - then the flow needs to be defined as such.
            if (cardInfo.getCardExistslot() == CardSlotTypeEnum.RF) {
                emvTransDataEntity.setProcType(EmvTransFlowEnum.QPASS);
                emvTransDataEntity.setChannelType(EmvChannelTypeEnum.FROM_PICC);

            } else {
                //Else, if the card detected is not NFC, then flow needs to be normal ICC (EMV)
                emvTransDataEntity.setProcType(EmvTransFlowEnum.FULL);
                emvTransDataEntity.setChannelType(EmvChannelTypeEnum.FROM_ICC);
            }

            //Init the EMV terminal configuration
            initEmvTerminalConfig();

            //Make the call to begin the actual EMV process
            emvHandler.emvProcess(emvTransDataEntity, this);

        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Insert Card", Toast.LENGTH_SHORT).show();
                }
            });
        }


    }

    /**
     * initEmvTermConfig ,include Terminal Capabilities, country code, currency code...
     */
    public void initEmvTerminalConfig(){

        //contact terminal Capability
        emvHandler.setTlv(ByteUtils.hexString2ByteArray("9F33"), ByteUtils.hexString2ByteArray("E0F8C8")); //Terminal Capabilities

        //currency code
        emvHandler.initTermConfig(ByteUtils.hexString2ByteArray("9f1a0208405f2a0208409f3c020840"));

         }



    /**
     * When the MSR detects a 'bad' swipe (i.e. angled swipe, or swipe where magnetic track data was read
     *  in a corrupt manner) - it will call this method.
     *
     *  By default, the reader will continue to listen at this point for another swipe. You can manually call stopSearch(..)
     *  to stop listening for a card swipe if you would like, and/or can show a message to the user like "Please Swipe Again"
     *  to inform them the previous swipe was invalid.
     */
    @Override
    public void onSwipeIncorrect() {
        Log.e(TAG + "_BadSwipe", "Bad EMV Swipe");

        //Show message to user warning them the swipe was bad.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Swipe Error, Please Swipe Again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * This method is only called when the reader detects multiple cards from the NFC reader. For example, if a user taps their wallet
     *  and they have multiple Contactless cards - the terminal will not know how to handle that event by itself.
     *
     * You
     */
    @Override
    public void onMultipleCards() {
        Log.e(TAG + "_MultCard", "Multiple cards detected...");

        //Show message to user warning that they need to try again by only tapping ONE card at a time.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Multiple Cards Detected. Please Tap Only One Card.", Toast.LENGTH_LONG).show();
            }
        });

    }

    /**
     * Callback function.
     *
     * This function is called by the EMV process to 'set' the transaction amount.
     *
     * You need to call emvHandler.onSetRequestAmountResponse(..) in order to confirm the amount. Afterwards,
     *  the EMV process will continue on to the next step.
     */
    @Override
    public void onRequestAmount() {
        Log.d(TAG, "Enter onRequestAmount()");

        //Set the amount in the emvHandler object
        emvHandler.onSetRequestAmountResponse(leftPad(transAmount, 12, '0'));
    }

    /**
     * Callback Function.
     *
     * This function is called automatically by the emv process once a card is inserted. It is used to display the
     *  available AIDs (i.e. 'Apps') on the payment card, and to select which AID should be used for the EMV process.
     *
     * In this sample app, we iterate through all the avilable AID on card, and show an AlertDialog/ListView to allow
     *  the user to select the AID. Once selected, we use the emvHandler.setOnSelAppResponse(..) to continue on in the EMV
     *  process using the selected AID.
     *
     * @param appNameList the list of app names available on the card.
     * @param appInfoList the List<CandidateAppInfoEntity> of objects representing the apps available on the card.
     * @param isFirstSelect whether the terminal should automatically select the first available AID on the card.
     */
    @Override
    public void onSelApp(List<String> appNameList, List<CandidateAppInfoEntity> appInfoList, boolean isFirstSelect) {
        Log.d(TAG, "Enter onSelApp()");

        mSelAppList = appNameList;

        Log.d(TAG, "isFirstSelect : " + isFirstSelect);

        Log.d(TAG, "appNameList:");
        for (int i=0; i<appNameList.size(); i++){
            Log.d(TAG, "appNameList[" + i + "] " + appNameList.get(i));
        }

        Log.d(TAG, "appInfoList:");
        for (int i=0; i<appInfoList.size(); i++){
            CandidateAppInfoEntity appInfo = appInfoList.get(i);
            Log.d(TAG, "appInfoList[" + i + "] AID  HEX   : " + ByteUtils.byteArray2HexString(appInfo.getAid()));
            Log.d(TAG, "appInfoList[" + i + "] AID  ASCII : " + EmvUtils.HexToASCII(ByteUtils.byteArray2HexString(appInfo.getAid())));
            Log.d(TAG, "appInfoList[" + i + "] Priority   : " + appInfo.getPriority());
            Log.d(TAG, "appInfoList[" + i + "] Label      : " + ByteUtils.byteArray2HexString(appInfo.getAppLabel()));
            Log.d(TAG, "appInfoList[" + i + "] Pref Name  : " + ByteUtils.byteArray2HexString(appInfo.getPreferName()));

            /* If enabled, auto-select any matching AID for common debit. */
            if (mAutoSelectCommonDebit) {
                for (int a = 0; a < mCommonDebitList.length; a++) {
                    if (ByteUtils.asciiByteArray2String(appInfo.getAid()).toUpperCase().compareTo(mCommonDebitList[a]) == 0) {
                        Log.w(TAG, "Auto-selected Common Debit AID : [" + mCommonDebitList[a] + "]");
                        emvHandler.onSetSelAppResponse(i + 1);
                        return;
                    }
                }
            }

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
                for (int i = 0; i < mSelAppList.size(); i++) {
                    Map<String, String> map = new HashMap<>();
                    map.put("appIdx", (i + 1) + "");
                    map.put("appName", mSelAppList.get(i));
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

    }

    @Override
    public void onConfirmEcSwitch() {
        Log.d(TAG, "Enter onConfirmECSwitch()");
        emvHandler.onSetConfirmEcSwitchResponse(true);
    }

    /**
     * Callback function.
     *
     * This function is called by the EMV process to confirm the card number.
     *
     * To move forward to the next step in the EMV process, you must call emvHandler.onSetConfirmCardNoResponse(true).
     *
     * For production purposes you should not store the number or show it on the screen, as it may make application non-compliant with PCI.
     *  Rather, it should be used to confirm the number is correct PAN format, or not otherwise malformed.
     *
     * @param cardInfoEntity the cardInfo object used in the callback to check the card number.
     */
    @Override
    public void onConfirmCardNo(final CardInfoEntity cardInfoEntity) {
        Log.d(TAG, "Enter onConfirmCardNo()");
        Log.d(TAG, "CardNumber from API : " + cardInfoEntity.getCardNo());
        Log.d(TAG, "getPAN() : Parsed PAN = " + getPAN());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "CardNo: " + cardInfoEntity.getCardNo(), Toast.LENGTH_SHORT).show();
            }
        });


        //confirm card number so goes to next step
        emvHandler.onSetConfirmCardNoResponse(true);
    }

    /**
     * Callback Function.
     *
     * This function is called by the EMV process when a card requiring PIN is used. This function should
     *  call another to show a PINPAD screen and retrieve the PIN from the user.
     *
     * The reason we need to define isOnlinePin is because the PIN process is different for ONLINE vs OFFLINE PIN.
     *
     * @param isOnlinePin where or not the PIN should be ONLINE or OFFLINE
     * @param leftTimes the number of tries enter the correct PIN
     */
    @Override
    public void onCardHolderInputPin(boolean isOnlinePin, int leftTimes) {
        Log.d(TAG, "Enter onCardHolderInputPIN()");

        startInputPin(true);


        //todo figure out what the params do...?
        //emvHandler.onSetPinInputResponse(true, false);
    }

    /**
     *
     * @param promptEnum
     */
    @Override
    public void onPrompt(PromptEnum promptEnum) {
        Log.d(TAG, "Enter onPrompt()");
        emvHandler.onSetPromptResponse(true);

    }

    /**
     * Gets a String representation of the captured card number from the tags
     *
     * Note: Can only be called when the card number is available to the Emv kernel, usually at onConfirmCardNo(..) and/or onOnlineProc(..)
     * @return String representation of the card number without the 'D' or other raw control characters
     */
    private String getPAN() {
        byte[] value = emvHandler.getTlv(new byte[]{0x5a}, EmvDataSourceEnum.FROM_KERNEL);
        if (value == null) {
            value = emvHandler.getTlv(new byte[]{0x57}, EmvDataSourceEnum.FROM_KERNEL);
            String tk2 = ByteUtils.byteArray2HexString(value).toUpperCase();
            tk2 = tk2.substring(0, tk2.indexOf('D'));
            value = ByteUtils.hexString2ByteArray(tk2);
        }
        String pan = null;
        if (value != null) {
            pan = ByteUtils.byteArray2HexString(value).toUpperCase();
            if (pan.endsWith("F")) {
                pan = pan.substring(0, pan.length() - 1);
            }
        }
        return pan;
    }

    /**
     * Get the AID being used by EMV process from the kernel.
     * @return String of AID being used by EMV process.
     */
    public String getProcessingAID(){
        Log.d(TAG, "getProcessingAID()");
        byte[] value = emvHandler.getTlv(new byte[]{0x54}, EmvDataSourceEnum.FROM_KERNEL);
        if (value == null)
            value = emvHandler.getTlv(new byte[]{0x4F}, EmvDataSourceEnum.FROM_KERNEL);
        if (value == null)
            value = emvHandler.getTlv(new byte[]{0x9, 0xF, 0x0, 0x7}, EmvDataSourceEnum.FROM_KERNEL);
        return ByteUtils.byteArray2HexString(value).toUpperCase();
    }

    /**
     *  Start the Input PIN process. This can be implemented in your own way.
     *
     *  It should accomplish:
     *   1 - Checking that the terminal is injected with DUKPT key (otherwise PIN entry cannot be done)
     *   2 - Setting the acceptable lengths for PIN entry to an int[] array (pinLen[] array below)
     *   3 - Set the PinPad algorithm to DUKPT
     *   4 - Convert the String value of the card PAN to a byte[] for passing into PinPad
     *   5 - Call the pinpad process based on if it should be ONLINE or OFFLINE PIN (determined by boolean in the constructor)
     *
     *  After the function calls the .sdkPinPad.inputOnlinePin(..) or .sdkPinPad.inputOfflinePin(..) functions, when the
     *
     * @param isOnlinePin if the PIN should be ONLINE (true) or OFFLINE (false)
     */
    public void startInputPin(final boolean isOnlinePin)
    {
        Log.d(TAG, "Enter startInputPin() : isOnlinePin = " + isOnlinePin);

        int INJECTED_PIN_SLOT = 0;  //slot '0' is where the normal PIN keys are injected, and should be used unless programming for P2PE

        //Check if there is a key injected into the terminal (it is required for PIN entry)
        if (sdkPinPad.dukptCurrentKsn(INJECTED_PIN_SLOT) == null) {
            //There is no key injected; cannot continue - show some error to user and break out
            Log.e(TAG, "startInputPin() : cannot continue, No key is injected!");
            emvHandler.emvProcessCancel();  //Stop the EMV process, cannot proceed to enter PIN without the injected key
            return;
        }

            //There is a key injected, continue on..
            Log.d(TAG, "startInputPIN() : key injected, moving on to pin entry");

        /**
         *  Create and array with the valid PIN entry lengths; will pass to PINPAD shortly as an argument
         *
         *  Note: length '0' would be for PIN bypass scenario
         */
            int[] pinLen = new int[]{0, 4, 5, 6, 7, 8, 9, 10, 11, 12};

            Log.d(TAG, "Acceptable PIN entry lengths (from pinLen array): ");
            for (int i=0; i<pinLen.length; i++)
                Log.d(TAG, "  Acceptable Length[" + i + "] = " + pinLen[i]);

            //Set the PINPAD algorithm mode - we want to use DUKPT
            sdkPinPad.setAlgorithmMode(AlgorithmModeEnum.DUKPT);

            /**
             *  Handle the PIN based on if it is online or offline pin
             *
             *  Online and Offline PIN have different flows / requirements
             */
            if (isOnlinePin)
            {
                //If ONLINE PIN...
                //Create byte array of the card number - we will pass it into the inputOnlinePin function
                byte[] cardNumBytes = getPAN().getBytes();
                inputPinResult = sdkPinPad.inputOnlinePin(pinLen,                           //array of acceptable PIN lengths (i.e. most are 4 digits, but can be other lengths)
                                                        60,                              //Pinpad timeout in seconds
                                                           cardNumBytes,                    //The card number's PAN in byte[] format
                                                           INJECTED_PIN_SLOT,               //The injection/key slot to use to encrypt the PIN
                                                           PinAlgorithmModeEnum.ISO9564FMT1,//The algorithm to use for encrypting PIN
                                                          this);        //The onPinPadInputListener object callback (implemented in MainAcitivity, so 'this')
                Log.d(TAG, "isOnlinePin result = " + inputPinResult);

            }
            else
            {
                //if OFFLINE PIN...
                //Create byte array of the card number - we will pass it into the inputOfflinePin function
                byte[] cardNumBytes = getPAN().getBytes();
                inputPinResult = sdkPinPad.inputOfflinePin(pinLen,                          //array of acceptable PIN lengths (i.e. most are 4 digits, but can be other lengths)
                                                        60,                              //Pinpad timeout in seconds
                                        this);                          //The onPinPadInputListener object callback (implemented in MainAcitivity, so 'this')
                Log.d(TAG, "isOfflinePin result = " + inputPinResult);
            }

            //If PIN input is not successful, process input PIN failed
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
    }


    /**
     * Callback Function.
     *
     * It is used to prompt the cashier, etc. to verify the ID of the customer matches the payment card.
     *
     * Since this is not usually done in practice, you can set onSetCertVerifyResponse(true);
     *
     * @param s
     * @param s1
     */
    @Override
    public void onCertVerify(String s, String s1) {
        Log.d(TAG, "Enter onCertVerify()");
        Log.d(TAG, "s: " + s);
        Log.d(TAG, "s1: " + s);
        emvHandler.onSetCertVerifyResponse(true);

    }

    /**
     * Callback function.
     *
     * This function is called when the EMV card is removed from the reader.
     */
    @Override
    public void onRemoveCard() {
        Log.d(TAG, "Enter onRemoveCard()");

    }

    /**
     *This function is where the application should 'process' the payment with the processor, and then set the result
     *  and response tags accordingly.
     *
     *  At the end of the function, you should call emvHandler.onSetOnlineProcResponse(..) with the processing result from the payment
     *   processor.
     */
    @Override
    public void onOnlineProc() {
        Log.d(TAG, "Enter onOnlineProc()");
        Log.d(TAG, "AID : [" + getProcessingAID() + "]");
        setLEDs("aB");

        /**
         *  Process the tags / transaction with the host for the authorization result.
         *
         *      For instance, you can retrieve the PinBlock String we saved from InputPin process, etc. and send to host here.
         *
         *  Save the host response, and parse the respective tags.
         *
         */
        Log.w(TAG, "Pretending to do something online...");


        //Build the EMVOnlineResultEntity object used to store the transaction result from the processor.
        EmvOnlineResultEntity emvOnlineResult = new EmvOnlineResultEntity();

        //If the transaction is APPROVED, set the 'AuthCode' to the authorization code returned in the host response (i.e. 'OK9790'), and the 'RejCode' to '00'
        //emvOnlineResult.setAuthCode("OK9790");
        emvOnlineResult.setRejCode("00");

        //If the transaction is DECLINED, don't set the 'AuthCode', and do set the 'RejCode' to '05' unless processor has other rejection code....
        //emvOnlineResult.setRejCode("05");

        //fill with the server response 55 field EMV data to do second auth
        //Fill setRecvField55 to (TAG91 + TAG71 + TAG72) in TLV format ... emvOnlineResult.setRecvField55(ByteUtils.hexString2ByteArray(emvDataSb.toString()));
//        String sampleTag = "910A01020304050607080000" + "720E9F180400010203860580CA9F3600";
//        emvOnlineResult.setRecvField55(ByteUtils.hexString2ByteArray(sampleTag));
        emvOnlineResult.setRecvField55(null);

        //call onSetOnlineProcResponse(..) with the online processing result to have EMV process move to the next step.
        emvHandler.onSetOnlineProcResponse(SdkResult.Success, emvOnlineResult);
    }

    @Override
    public void onReadCardAgain() {
        Log.d(TAG, "Enter onReadCardAgain()");

    }

    @Override
    public void onAfterFinalSelectedApp() {
        Log.d(TAG, "Enter onAfterFinalSelectedApp()");


        emvHandler.onSetAfterFinalSelectedAppResponse(true);


    }

    /**
     *Callback Function.
     *
     * This is the last step called during the EMV process. It is called when either the process is successfully
     *  completed, if was cancelled, or if there was some other error. You can retrieve the result of the process
     *  by retrieving the retCode object from the constructor and using a switch statement to check the meaning.
     *  The result codes are based off the NEXGO SDK, and can use SdkResult.* to find the meaning.
     *
     * The EmvProcessResultEntity returned can also be used to check characteristics of the processed transaction.
     *
     * @param retCode the return code denoting the end result of the EMV process/transaction.
     * @param emvProcessResultEntity the EmvProcessResultEntity containing various information about the processed transaction.
     */
    @Override
    public void onFinish(int retCode, EmvProcessResultEntity emvProcessResultEntity) {
        Log.d(TAG, "Enter onFinish() -> returnCode: " + retCode);

        //Dismiss the ProgressDialog since the process has finished
        progressDialog.dismiss();

        DecimalFormat df = new DecimalFormat("0.00##");
        String formattedAmount = df.format(Double.parseDouble(transAmount));

        //Parse the retCode to see what the result was
        switch (retCode) {
            case SdkResult.Emv_Success_Arpc_Fail:
            case SdkResult.Success:
                //online approve
                Log.d(TAG, "SUCCESS");

                //Show a dialog message to user letting them know trans was successful
                showResultDialog("ONLINE APPROVED", "EMV Transaction was successful:\n\n" +
                        "AID:\t" + getProcessingAID() + "\n" +
                        "PAN:\t" + getPAN() + "\n" +
                        "Amt:\t$" + formattedAmount, MainActivity.this);
                setLEDs("aGB");
                break;

            case SdkResult.Emv_Qpboc_Offline:// EMV Contactless: Offline Approval
            case SdkResult.Emv_Offline_Accept://EMV Contact: Offline Approval
                //offline approve
                Log.d(TAG, "OFFLINE APPROVE");

                //Show a dialog message to user letting them know trans was successful

                showResultDialog("OFFLINE APPROVED", "EMV Transaction was successful:\n\n" +
                        "AID:\t\t" + getProcessingAID() + "\n" +
                        "PAN:\t" + getPAN() + "\n" +
                        "Amt:\t$" + formattedAmount, MainActivity.this);
                setLEDs("aGB");
                break;

            case SdkResult.Emv_Qpboc_Online://EMV Contactless: Online Process for union pay
                //union pay online contactless--application should go online

                break;

            case SdkResult.Emv_Candidatelist_Empty:// Application have no aid list No
            case SdkResult.Emv_FallBack://  FallBack ,chip card reset failed
                //fallback process
                Log.w(TAG, "NO AID or CHIP RESET / FALLBACK FAILED");

                //Show a dialog message to user letting them know trans result. Fallback isn't implemented in this EMV sample - but would be to try the MSR
                showResultDialog("DECLINED", "No AID available, or chip reset failed. Fallback failed.", MainActivity.this);
                setLEDs("aR");
                break;

            case SdkResult.Emv_Arpc_Fail: //
            case SdkResult.Emv_Script_Fail:
            case SdkResult.Emv_Declined:
                //emv decline ,if it is in second gac, application should decide if it is need reversal the transaction
                Log.w(TAG, "DECLINED");

                //Show a dialog message to user letting them know trans result
                showResultDialog("DECLINED", "Transaction was Declined. Reversal may be required.", MainActivity.this);
                break;

            case SdkResult.Emv_Cancel:// Transaction Cancel
                //user cancel
                Log.w(TAG, "TRANS EMV CANCELLED");

                //Show a dialog message to user letting them know trans result
                showResultDialog("DECLINED", "EMV cancelled transaction.", MainActivity.this);
                break;

            case SdkResult.Emv_Offline_Declined: //
                //offline decline
                Log.w(TAG, "OFFLINE DECLINED");
                //Show a dialog message to user letting them know trans result
                showResultDialog("DECLINED", "Transaction Offline Declined.", MainActivity.this);
                break;

            case SdkResult.Emv_Card_Block: //Card Block
                //card is blocked
                Log.e(TAG, "CARD BLOCKED");
                //Show a dialog message to user letting them know trans result
                showResultDialog("DECLINED", "Card is blocked.", MainActivity.this);
                break;

            case SdkResult.Emv_App_Block: // Application Block
                //card application block
                Log.e(TAG, "CARD APP BLOCKED");

                //Show a dialog message to user letting them know trans result
                showResultDialog("DECLINED", "Application on card is locked.", MainActivity.this);
                break;

            case SdkResult.Emv_App_Ineffect:
                //card not active
                break;

            case SdkResult.Emv_App_Expired:
                //card Expired
                Log.w(TAG, "CARD IS EXPIRED");

                //Show a dialog message to user letting them know trans result
                showResultDialog("DECLINED", "Transaction was declined; Card is expired.", MainActivity.this);
                setLEDs("aR");
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

                //Show a dialog message to user letting them know trans result
                showResultDialog("DECLINED", "Transaction was terminated.", MainActivity.this);
                setLEDs("aR");
                break;

            default:
                //other error
                Log.e(TAG, "GENERAL ERROR");

                //Show a dialog message to user letting them know trans result
                showResultDialog("DECLINED", "General EMV Error Occurred.", MainActivity.this);
                setLEDs("aR");
                break;
        }

    }

    /**
     * This function is used to display text in an 'AlertDialog' displayed to the user.
     * <p>
     *
     * @param body - The text to be displayed in the AlertDialog.
     */
    public static void showRetryDialog(String title, String body, final Context context) {
        new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //yes

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing

                    }
                })
                .setTitle(title)
                .setMessage(body)
                .show();
    }

    /**
     * This function is used to display text in an 'AlertDialog' displayed to the user.
     * <p>
     *
     * @param body - The text to be displayed in the AlertDialog.
     */
    public static void showResultDialog(String title, String body, final Context context) {
        new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {




                    }
                })
                .setCancelable(false)
                .setTitle(title)
                .setMessage(body)
                .show();
    }

    /**
     * Sets the LEDs on the device appropriately.
     * lowercase = off / uppercase = on
     * Valid values = rRbBgGyY
     *   r -> red
     *   b -> blue
     *   g -> green
     *   y -> yellow
     *   a -> all
     * @param ledString
     */
    public void setLEDs(String ledString){
        LEDDriver ledDriver = deviceEngine.getLEDDriver();
        for (int i=0; i<ledString.length(); i++){
            char c = ledString.charAt(i);
            switch (c){
                case 'r':
                    ledDriver.setLed(LightModeEnum.RED, false);
                    break;
                case 'R':
                    ledDriver.setLed(LightModeEnum.RED, true);
                    break;
                case 'b':
                    ledDriver.setLed(LightModeEnum.BLUE, false);
                    break;
                case 'B':
                    ledDriver.setLed(LightModeEnum.BLUE, true);
                    break;
                case 'g':
                    ledDriver.setLed(LightModeEnum.GREEN, false);
                    break;
                case 'G':
                    ledDriver.setLed(LightModeEnum.GREEN, true);
                    break;
                case 'y':
                    ledDriver.setLed(LightModeEnum.YELLOW, false);
                    break;
                case 'Y':
                    ledDriver.setLed(LightModeEnum.YELLOW, true);
                    break;
                case 'a':
                    ledDriver.setLed(LightModeEnum.YELLOW, false);
                    ledDriver.setLed(LightModeEnum.BLUE, false);
                    ledDriver.setLed(LightModeEnum.GREEN, false);
                    ledDriver.setLed(LightModeEnum.RED, false);
                    break;
                case 'A':
                    ledDriver.setLed(LightModeEnum.YELLOW, true);
                    ledDriver.setLed(LightModeEnum.BLUE, true);
                    ledDriver.setLed(LightModeEnum.GREEN, true);
                    ledDriver.setLed(LightModeEnum.RED, true);
                    break;
            }
        }

    }

}
