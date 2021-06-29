package com.nexgo.msrsample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.device.reader.CardInfoEntity;
import com.nexgo.oaf.apiv3.device.reader.CardReader;
import com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum;
import com.nexgo.oaf.apiv3.device.reader.OnCardInfoListener;

import java.util.HashSet;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements OnCardInfoListener {

    private final String TAG = "MSR_SAMPLE";    //Tag used when outputting to ADB logcat

    private DeviceEngine deviceEngine;  //DeviceEngine object used to retrieve cardReader object.
    private String[] trackData; //String array where we will store/retrieve MSR track data
    private CardReader cardReader;  //cardReader class we will used to initialize and read from the MSR hardware on the device.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize button used to start cardRead action
        Button readCardButton = (Button) findViewById(R.id.readCardButton);

        //Initialize the deviceEngine object
        deviceEngine = APIProxy.getDeviceEngine(MainActivity.this);

        //Get the CardReader object from the previously initialized deviceEngine object
        cardReader = deviceEngine.getCardReader();

        //Setup OnClickListener on the button so that when pressed, we search for card swipe
        readCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //The time in seconds the cardReader should stay open to listen for a card swipe.
                int cardSearchTimeoutSeconds = 60;

                //Initialize the HashSet used to determine which capture methods (i.e. Swipe, ICC, NFC) are to be listened for.
                HashSet<CardSlotTypeEnum> slotTypes = new HashSet<>();

                //Since this is MSR sample app, we are only listening for SWIPE - others will be ignored.
                slotTypes.add(CardSlotTypeEnum.SWIPE);

                //Begin 'searching' for the card swipe. Once swiped, the callback methods will be called to handle the swipe.
                cardReader.searchCard(slotTypes, cardSearchTimeoutSeconds, MainActivity.this);
            }
        });

    }


    /**
     *  Callback method
     *
     *  Once the user has called the 'searchCard' function, when the application detects a valid cardSwipe (i.e. not a swipe error or multiple cards)
     *  it will call this method.
     *
     *  The cardSwipe information is contained within the cardInfoEntity object. You can extract the track data using the sample below.
     *
     *  We should call the 'stopSearch' method once a card is swiped to close the reader.
     */
    @Override
    public void onCardInfo(int i, CardInfoEntity cardInfoEntity) {

        //We got the card swipe, now we should stop listening for additional swipes.
        cardReader.stopSearch();
        Log.d(TAG, "Called stopSearch()");

        if (cardInfoEntity == null)
        {
            //if cardInfo returned is null, there is issue. Break out of the method.
            Log.e(TAG, "Received cardInfoEntity that was NULL. Breaking.");
            return;
        }

        //cardInfo is not null; initialize the trackData array so we can store the track data
        trackData = new String[3];

        //Store the track data parsed from the cardSwipe into an array for retreival later.
        trackData[0] = "" + cardInfoEntity.getTk1();
        trackData[1] = "" + cardInfoEntity.getTk2();
        trackData[2] = "" + cardInfoEntity.getTk3();

        //Output the track data read from the card swipe.
        Log.d(TAG, "Received Card Swipe...");
        Log.d(TAG, "Track1:\t" + trackData[0]);
        Log.d(TAG, "Track2:\t" + trackData[1]);
        Log.d(TAG, "Track3:\t" + trackData[2]);

        //Show the swiped card track data in a toast message on UI thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Output the track data read from the card swipe.
                String toastTrackData = "Track1:\t" + trackData[0] + "\n" +
                        "Track2:\t" + trackData[1] + "\n" +
                        "Track3:\t" + trackData[2];
                Toast.makeText(MainActivity.this, toastTrackData, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Callback Method
     *
     * If there was an error reading the swiped card, such as in a bad swipe (angled, too quick, etc.) - this method will be called by the cardReader class.
     *
     * You can either call stopSearch to stop reading card, or can show message to user like 'Swipe Error' and keep the reader open to allow
     * the user to try an additional swipe.
     *
     */
    @Override
    public void onSwipeIncorrect() {
        Log.e(TAG, "Swipe Error!");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Swipe Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Callback Method
     *
     * This method is called when multiple cards are detecting in the cardRead method.
     *
     * This should *not* occur for an MSR swipe, but can occur for NFC for example if multiple cards are tapped onto the device.
     *
     * Including here for completeness.
     *
     */
    @Override
    public void onMultipleCards() {
        Log.e(TAG, "Received Multiple Cards!");
    }
}
