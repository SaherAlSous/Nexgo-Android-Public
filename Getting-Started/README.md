# Getting Started

## Basic Development Requirements
In order to get started developing applications on the Nexgo terminals, you will need the following items:

1. Nexgo POS Terminal in [Debug Mode ](#Debug-Mode-Devices)
2. Computer with Android Studio installed
3. Micro-USB cable (N5) or USB-C cable (N6)
4. ADB Drivers installed on the development machine

### Sideload APK
There are two methods for sideloading an APK to the terminal:

1. Copy the APK to the terminal while attached to USB, then click the file from the terminal's File Explorer to initiate the installation. 
1. Using ADB, enter `adb install <APK File Path>` command to install an APK file over the ADB connection with the terminal. 
  1.1  Note that Debug Mode terminal is required to install unsigned applications on the Nexgo terminals. 

### Debug Mode Devices
Nexgo Android devices must be in Debug mode in order for rapid development using Android Studio, as only devices in Debug Mode with Developer Mode enabled will allow you to use the ADB connection. 

Devices in Debug Mode will display Debug Mode in red letters at the bottom right of the screen:
![Debug Mode](../res/img/debugMode.png)

If your device is **not** in debug mode, it cannot be changed to it remotely. The device must be sent back to Nexgo to change the mode for security reasons. 