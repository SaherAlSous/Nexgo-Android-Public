# Application Signing

All **Release Mode** terminals require applications to be signed with a Nexgo-generated signature before the terminals will allow their installation. 

Nexgo utilizes the [SMS Signing Server](https://sms.xgd.com/) (sms.xgd.com) for handling the signing process. 

A high level overview of the signature process is as follows:
1. User logs on to their account on sms.xgd.com.
2. User selects **Signature Management** from the left-side menu.
3. User uploads their APK to the system. 
4. System signs the APK with vendor approved signature/certificate.
5. User downloads the signed APK from the sms.xgd.com website. 
6. User is able to install application on device. 

Below is a more detailed, step-by-step, guide for performing the signature process.

## Upload APK for Signature
1. Navigate to the sms.xgd.com website on supported browser.
1. Enter the email and password of a valid account, and then click the *retrieve code* button to be sent an authentication code to your email to validate the login. 
![Login Page](/res/img/1_login.png)
1. From the main page after login was successful, select **Signature Management** > *Android Signature* from the menu on the left-side of the page. 
![Android Signature](/res/img/2_android_signature.png)
1. From the *Android Signature* page, select **Add** to bring up the new signature prompts.
![Sign New APK](/res/img/3_add_new_file.png)
1. Enter in the required fields, and then click **Confirm**.
![New Sig Prompt](/res/img/4_new_signature_prompt.png)
1. You will be returned to the main Signature page. The uploaded file should show up on the top row of the file listings, and may take a few moments before it is ready for download. 

## Download the Nexgo-Signed APK
1. Once you have uploaded an APK for signing, navigate to the main *Android Signature* page. Your file should appear on the top row of the listings. 
1. Find the corresponding file listing, and click the **Download** button to download the *signed* version of the application. 
![Download APK](/res/img/5_download_file.png)
1. When prompted, select where to save the file on your computer and click SAVE. 
1. Signed file will download accordingly. 


