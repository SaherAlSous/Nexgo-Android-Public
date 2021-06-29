package com.nexgo.ledsample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.device.led.LEDDriver;
import com.nexgo.oaf.apiv3.device.led.LightModeEnum;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Button toggleRedButton, toggleBlueButton, toggleYellowButton, toggleGreenButton;
    private boolean isRedOn = false;
    private boolean isBlueOn = false;
    private boolean isYellowOn = false;
    private boolean isGreenOn = false;

    private DeviceEngine deviceEngine;
    private LEDDriver ledDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize the device engine and get instance of the LED driver
        deviceEngine = APIProxy.getDeviceEngine(MainActivity.this);
        ledDriver = deviceEngine.getLEDDriver();

        //Turn off all LED to start since some might be turned on from other apps/processes
        ledDriver.setLed(LightModeEnum.RED, false);
        ledDriver.setLed(LightModeEnum.BLUE, false);
        ledDriver.setLed(LightModeEnum.GREEN, false);
        ledDriver.setLed(LightModeEnum.YELLOW, false);

        //Initialize the Button view objects
        toggleRedButton = (Button) findViewById(R.id.toggleRedButton);
        toggleBlueButton = (Button) findViewById(R.id.toggleBlueButton);
        toggleYellowButton = (Button) findViewById(R.id.toggleYellowButton);
        toggleGreenButton = (Button) findViewById(R.id.toggleGreenButton);

        //Set the button OnClickListeners
        toggleRedButton.setOnClickListener(this);
        toggleBlueButton.setOnClickListener(this);
        toggleYellowButton.setOnClickListener(this);
        toggleGreenButton.setOnClickListener(this);
    }

    /*
    Handle the OnClick events for the buttons in the layout.
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.toggleRedButton:
                //Set RED button action
                if (isRedOn){
                    //if RED is already on, turn it off and set the corresponding boolean
                    ledDriver.setLed(LightModeEnum.RED, false);
                    isRedOn = false;
                } else {
                    ledDriver.setLed(LightModeEnum.RED, true);
                    isRedOn = true;
                }
                break;
            case R.id.toggleBlueButton:
                //Set BLUE button action
                if (isBlueOn){
                    ledDriver.setLed(LightModeEnum.BLUE, false);
                    isBlueOn = false;
                } else {
                    ledDriver.setLed(LightModeEnum.BLUE, true);
                    isBlueOn = true;
                }
                break;
            case R.id.toggleYellowButton:
                //Set YELLOW button action
                if (isYellowOn){
                    ledDriver.setLed(LightModeEnum.YELLOW, false);
                    isYellowOn = false;
                } else {
                    ledDriver.setLed(LightModeEnum.YELLOW, true);
                    isYellowOn = true;
                }
                break;
            case R.id.toggleGreenButton:
                //Set GREEN button action
                if (isGreenOn){
                    ledDriver.setLed(LightModeEnum.GREEN, false);
                    isGreenOn = false;
                } else {
                    ledDriver.setLed(LightModeEnum.GREEN, true);
                    isGreenOn = true;
                }
                break;
        }
    }
}
