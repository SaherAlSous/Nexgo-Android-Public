# LEDSample
A sample application that toggles the various LEDs (blue, green, yellow, red) on/off using the Nexgo SmartSDK.
 
The .aar library is already imported/configured inside the project. You can build/install this project as-is to a NEXGO N-series payment terminal from Android Studio directly, or by building an APK and installing it onto the terminal manually. 

## Table of Contents
* [Importing the Sample Project](#importing-the-sample-project)
* [LED Control](#led-control)
* [Use Cases](#use-cases)
* [Miscellaneous](#miscellaneous)

## Importing the Sample Project
To download and use the project:
  1. Download the project by downloading the zip directly, or by using the git clone command
  2. Open Android Studio on your computer
  3. Select New > Import Project...
  4. Select the project from the Import dialog window that appears
  
## LED Control
The N series terminals have 4 color LEDs (blue, green, yellow, and red) located on the front of each terminal. These LEDs can be used to help denote information to the user of an application. For example, when an error has occurred, you can illuminate the red LED. 

To control the LEDs, you first need to get an instance of the LED Driver object from the deviceEngine:
```java
   DeviceEngine deviceEngine = APIProxy.getDeviceEngine(MainActivity.this);
   LEDDriver ledDriver = deviceEngine.getLEDDriver();
```

After you get the `LEDDriver` object, you can use the `setLed(LightModeEnum enum, boolean illuminated)` to control each of the LEDs. If the boolean in the second argument is TRUE, it will turn on the specified LED. If the boolean is FALSE, it will turn off the specified LED:
```java
   //Turn the red LED off
   ledDriver.setLed(LightModeEnum.RED, false);
   
   //Turn the red LED on
   ledDriver.setLed(LightModeEnum.RED, true);
```

The valid `LightModeEnum` values are as follows:
| LightModeEnum | Color |
| :--------------- | :--------------- |
| LightModeEnum.RED | RED |
| LightModeEnum.BLUE | BLUE |
| LightModeEnum.GREEN| GREEN |
| LightModeEnum.YELLOW| YELLOW |

The boolean values for `setLed(..., boolean illuminated)` are as follows:
| boolean | Action |
| :--------------- | :--------------- |
| true | LED on |
| false | LED off |

> Note: The system will turn on/off certain LEDs itself, such as green/red when charging. Once your application utilizes the LED driver to change the status of an LED - the system will not change it back.

## Use Cases
In our own applications, we use the LEDs to indicate status of various tasks. Besides giving the user some feedback when actions are being done, it can also be helpful for diagnosing and troubleshooting issues.

For example, in our own applications, we use the LEDs like below:
| Task | Color |
| :--------------- | :--------------- |
| Error | RED |
| Processing | BLUE |
| Success/Done | GREEN |
| Waiting on User | YELLOW |

* In the case where a user was having issues and contacted support, we could ask the user what lights were showing before/when the issue was occurring. 
  * This helps to very quickly narrow down what the issue could be without having to get logs (i.e. app is crashing when yellow light is on could indicate some issue with input).

## Miscellaneous
* Since the system, as well as other applications, can control the LEDs on the device - *if* you plan on utilizing the LEDs inside your own application it is recommended to set them all OFF when your application starts. 

