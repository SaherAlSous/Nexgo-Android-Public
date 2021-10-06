/**
 * @author Hayden Morris <hmorris@exadigm.com> 3/21/2019
 */

package com.nexgo.haydenm.intentcaller;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.device.printer.AlignEnum;
import com.nexgo.oaf.apiv3.device.printer.OnPrintListener;
import com.nexgo.oaf.apiv3.device.printer.Printer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity {

    //TAG string used for Logcat output
    private final static String TAG = "IntentCallerApp";

    // Buttons and text inputs from the layout
    private Button intent_sale_go_button;
    private EditText intent_sale_amount_input;
    private EditText intent_tip_amount_input;

    // JSON Objects that will be packed into an Intent & sent to the Integrator
    private JSONObject Input;
    private JSONObject Action;
    private JSONObject Payment;

    private String pSaleType;
    private boolean pReceipt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intent_sale_go_button       =   (Button)    findViewById(R.id.intent_sale_go_button);


        intent_sale_go_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("GoButtonClicked", "GO pressed...Building intent..");

                //Begin to build the CreditSale JSON message (returns true/false depending on result)
                if (buildCreditSaleJSONMessage())
                {
                    //CreditSale message params built successfully...

                    //Build the main JSON, and pack the credit JSON into the request
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.Integrator");   //The listener name of Integrator
                    intent.putExtra("Input", Input.toString());      //Pack the Input JSONObject into the intent

                    //check the activity is available to handle intent
                    if (isActivityAvailable(intent)) {
                        //Activity for target intent exists on device (i.e. Integrator) - can continue to call startActivityForResult(..)
                        Log.d(TAG, "Activity for target intent exists on the device.");
                        startActivityForResult(intent, 1); //todo list requestcodes
                    }
                    else {
                        //Activity for target intent does NOT exist on device - do not call startActivityForResult(..) else will crash
                        Log.e(TAG, "Error: Activity for target intent does not exist on the device.");
                        Toast.makeText(getApplicationContext(), "Activity for Intent not available!", Toast.LENGTH_LONG).show();
                    }
                }
                else
                {
                    //Failed to successfully build the CreditSale JSON request
                    Log.e("GoButtonClicked", "buildCreditSaleJSONMessage() failed!");
                }
            }
        });

        //Initialize the 2 EditText objects used to get the input AMOUNTs from the user
        intent_sale_amount_input    =   (EditText) findViewById(R.id.intent_sale_amount_input);
        intent_tip_amount_input     =   (EditText)  findViewById(R.id.intent_tip_amount_input);

    }

    /**
     * Builds the JSON Message that will be put in an Intent and sent to the Exadigm Integrator to
     * initiate a Credit Sale with our desired amount. If the JSON message is able to be built
     * without any issues, it will return true, otherwise false.
     * @return true if json message is able to be built correctly, otherwise returns false.
     */
    public boolean buildCreditSaleJSONMessage()
    {
        //Initialiate JSONObjects that will be packed into the Intent sent to Integrator.
        Input   = new JSONObject();
        Action  = new JSONObject();
        Payment = new JSONObject();

        //Action Parameters
        boolean pSignature        =  true; //Require on-screen signature
        boolean pManual           =  true; //Allow manual/keyed entry (as opposed to SWIPE/EMV/TAP)
        String pProcessor         =  "EVO";
        pReceipt                  =  true; //Print a receipt

        try {
            //Put the required additional parameters for a 'Credit Sale' into the Action JSONObject
            Action.put("signature", pSignature);
            Action.put("receipt", pReceipt);
            Action.put("manual", pManual);
            Action.put("processor", pProcessor);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [ACTION] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }

        //Payment Parameters
        pSaleType         =  "Sale"; //'Sale' = Credit Sale
        if (intent_sale_amount_input.getText().toString().equalsIgnoreCase(""))
        {
            Log.e("buildSaleJSONMessage()", "Amount input was blank..");
            return false;
        }
        //Get the Sale parameters (amounts $$) entered by the user
        String  pSaleAmount       =  intent_sale_amount_input.getText().toString();
        String  pTipAmount        =  intent_tip_amount_input.getText().toString();
        String  pCashbackAmount   =  "0.00";

        try {
            //Put the required payment fields for a 'Credit Sale' into the Payment JSONObject
            Payment.put("type", pSaleType);
            Payment.put("amount", pSaleAmount);
            Payment.put("tip_amount", pTipAmount);
            Payment.put("cash_back", pCashbackAmount);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [PAYMENT] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }

        //Input Parameters (sent with Intent)
        try {
            //Pack the various JSONObject (Action, Payment, Host) Strings into the Intent to send to Integrator app
            Input.put("action", Action);
            Input.put("payment", Payment);
            //Input.put("host", Host); //20190312 Hayden -- Remove sending the Host Params b/c no longer needed. It is handled by Integrator.
            Log.d(TAG,"Params:" + Input.toString());

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [INPUT] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * The onActivityResult(X,X,X) function is called after the Integrator Intent has run
     * and the result is returned to the IntentCaller application.
     *
     * This is the function where we can handle operations after an Integrator transaction has
     * finished, like getting the 'Signature' image, parsing a TransactionKey, or just for checking
     * the raw response from the Integrator (including the raw HOST RESPONSE)
     * @param requestCode
     * @param resultCode the resultCode returned from the activity called in startActivityForResult(..)
     * @param data the data returned from Integrator, containing results and other messages/objs
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {

                String response = data.getStringExtra("transdata");
                System.out.println("RESPONSE ====> " + response);

                // Create gson object
                Gson gson = new Gson();
                // Convert JSON string to Java objects
                ResponseParser pResponse = gson.fromJson(response, ResponseParser.class);
                System.out.println("pResponse Object => " + pResponse);

                // Retrieve Transaction ID
                String transID = pResponse.getPacketData().getTransactionID();
                System.out.println("TransactionID=" + transID);

                //Check if there is a signature in the response data and it is not null.
                if (data.hasExtra("signature") && data.getByteArrayExtra("signature") != null && new String(data.getByteArrayExtra("signature")).compareToIgnoreCase("") != 0)
                {
                    //Initialize the DeviceEngine object used to retrieve the printer component.
                    DeviceEngine deviceEngine = APIProxy.getDeviceEngine(MainActivity.this);

                    //Retrieve the Printer from the deviceEngine object.
                    Printer printer = deviceEngine.getPrinter();

                    //Init the printer
                    printer.initPrinter();

                    //Set the typeface
                    printer.setTypeface(Typeface.DEFAULT);

                    //Set the spacing
                    printer.setLetterSpacing(5);

                    //Create bitmap object from signature byte array
                    Bitmap signatureBitmap = BitmapFactory.decodeByteArray(data.getByteArrayExtra("signature"), 0, data.getByteArrayExtra("signature").length);

                    // Print customer signature to the receipt
                    // If pReceipt is set to false, do not use the code inside the if statement
                    if (pReceipt == true) {
                        //Append the title above where the signature will print
                        printer.appendPrnStr("Customer Signature:", 34, AlignEnum.LEFT, true);

                        //Append the signature image to the printer queue
                        printer.appendImage(signatureBitmap, AlignEnum.CENTER);
                    }

                    //Begin the print process using the the values set from above. Use callback to check print result.
                    printer.startPrint(true, new OnPrintListener() {
                        @Override
                        public void onPrintResult(final int retCode) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Print Result: " + retCode + "", Toast.LENGTH_SHORT).show();

                                    if (retCode == 0)
                                        Log.d("Printer retcode", "Printer Return code = [0] " + Thread.currentThread().getName());
                                    else if (retCode != 0)
                                        Log.e("Printer retCode", "Printer Error! Return code = [" + retCode + "] " + Thread.currentThread().getName());
                                }
                            });
                        }
                    });

                }

                //Show response to the user
                showAlertDialog("Response:",
                        "" + response,
                        MainActivity.this);

            }
        }
    }


    /**
     * Checks to assure that the activity required by the intent exists on the device.
     *
     * If the activity does not exist on the device, it will crash the app. Thus we must check that
     *  it exists before calling startActitivtyForResult(..) to prevent the crash scenario.
     *
     * If the activity does exist, then the intent can be called safely (assuming no other issues).
     *
     * @param intent the Intent that will be called in startActivityForResult(..) to call the Integrator application.
     * @return true if the activity requested by the intent exists, else returns false.
     */
    public boolean isActivityAvailable(Intent intent)
    {
        PackageManager manager = getApplicationContext().getPackageManager();
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        return (infos.size() > 0);
    }

    /**
     * This function is used to display text in an 'AlertDialog' displayed to the user.
     * <p>
     * In this sample application, it is used to display the transaction result message returned
     * from the Integrator.
     *
     * @param body - The text to be displayed in the AlertDialog.
     */
    public static void showAlertDialog(String title, String body, Context context) {
        new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

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

}
