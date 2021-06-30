# SmartConnect 
"SmartConnect" is Nexgo's Semi-Integrated Android API for allowing external applications to process payments through Nexgo's certified payment applications. Its main method of communication between external applications and the payment application is through JSON messages using android Intents. 

SmartConnect allows 3rd parties the ability to process payments using Nexgo's certified payment application without having to go through a lengthy certification process themselves. 
*  An external application requesting a certain transaction type (i.e. Sale) with a pre-defined amount (i.e. $4.00) to be processed. 
   *  The 3rd party application will send this request to the Nexgo Integrator application. 
*  The Integrator application takes control of the screen, and prompts the user for payment and then handles the payment processing. 
*  After a transaction has been processed by the gateway/host, the Integrator application will return certain information back to the requesting application that contains the authorization result and details. 

> No secure card-holder information is exposed, and only permitted fields are returned to the caller application. 

Please review the SmartConnect API PDF documentation in this repository for full documentation. This repository is currently being updated. 

## Table of Contents
* [SmartConnect Flow](#smartconnect-flow)
* [Variants](#variants)
* [SmartConnect Intent](#smartconnect-intent)
  * [Example Intent Request](#example-intent-request)
  * [Example Intent Response](#example-intent-response)
* [SmartConnect TCP](#smartconnect-tcp)
  * [Example TCP Request](#example-tcp-request)
* [Miscellaneous](#miscellaneous)
  * [Parse Signature from SmartConnect Response](#parse-signature-from-smartconnect-response)
  * [resultCode Values](#resultcode-values)


## SmartConnect Flow
![SmartConnect Flow](/res/img/smartconnect_flow.jpg)

## SmartConnect Intent
The Android Intent integration mode is the preferred method for communicating with the Integrator application for a number of reasons.

### Example Intent Request
The following is a SmartConnect JSON request message to perform a basic **Sale** for **$1.00**:
```json
{
  "action":{
    "processor":"EVO",
    "receipt":true
  },
  "payment":{
    "type":"Sale",
    "amount":"1.00"
  }
}
```

To send a transaction request message to the Integrator using an ‘Intent’, we need to specify the package to call, include the request message in the intent (the **intent.putExtra("Input",message)** section), and then send the Intent while listening for the response:
```java
Intent intent = new Intent();                         //Initialize the ‘intent’ object
intent.setAction("android.intent.action.Integrator"); //Specify the package to call with Intent
intent.putExtra("Input",message);                     //Put the request message into the Intent
startActivityForResult(intent, REQUEST_CODE)          //Call the Intent, listen for the result
```
**Note**: message should be the full JSON request message.

### Example Intent Response
Since the Intent is invoked using the `startActivityForResult(…)` function, when the Integrator action is completed, the `startActivityForResult` callback from the calling application will be executed. We can expect to receive a response back from the Integrator denoting the result of the transaction.

The response will be contained within the ‘data’ Intent object:
```java
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
```


For example, we can get the ‘Transaction Data’ by parsing the ‘transdata’ field in the ‘data’ Intent object:
```java
String response = data.getStringExtra("transdata");
```

## SmartConnect TCP
The Android Intent integration mode is the preferred method for communicating with the Integrator application for a number of reasons.

### Example TCP Request
The following is a SmartConnect JSON request message to perform a basic **Sale** for **$1.00**:
```json
{
  "action":{
    "processor":"EVO",
    "receipt":true
  },
  "payment":{
    "type":"Sale",
    "amount":"1.00"
  }
}
```

To send a transaction request message to the Integrator using TCP, we need to create a connection to the device with the port the application is listening on. Afterwards, send the TCP request and listen for the response:
```java
//--------------[POS Network Settings]----------------------------------
public String POSIPADDR = "192.168.30.6";  //The IP Address of N5 we are trying to connect to (must be routable)(should be on same LAN)
static final int APP_PORT = 8765;          //Port SmartConnect is listening on (TCP is 8765)
static int CONN_TIMEOUT = 200000;          //Connect Timeout (PC -> N5)
static int READ_TIMEOUT = 120000;          //Read Timeout (millis)
//-------------------[END]----------------------------------------------

StringBuilder response = new StringBuilder();

//Build the JSON request string
String request = message;

try {
  //Create new socket that will connect to the POS endpoint (POS_IP_ADDR:8765)
  Socket socket = new Socket(); // = new Socket(hostURL, hostPort);
  socket.connect(new InetSocketAddress(POSIPADDR, APP_PORT), CONN_TIMEOUT); //set timeout for host failure
  socket.setSoTimeout(READ_TIMEOUT);

  //write data
  BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
  bos.write(request.getBytes("UTF-8"));
  bos.flush();

  //read response
  DataInputStream dis = new DataInputStream(socket.getInputStream());
  response.append(dis.readLine());


  //shutdown socket stuff
  socket.shutdownInput();
  socket.shutdownOutput();
  socket.close();

	} catch (IOException e) {
    onConnectError(e.getMessage());
    e.printStackTrace();
    response.append(e.toString());
    return;
	}

```
**Note**: `message` should be the full JSON request message.


## Miscellaneous

### Parse Signature from SmartConnect Response
If you had set **signature:true** in the JSON request, and the transaction performed supports a signature, then the application will prompt the user for their signature after an approved transaction. In this case, the signature will be returned to the calling application inside the Intent data object.
```java
data.getByteArrayExtra("signature")
```

In such case, you can create a Bitmap object of the signature object for display, storing, or printing using the following sample code:
```java
Bitmap signatureBitmap = BitmapFactory.decodeByteArray(
                                  data.getByteArrayExtra("signature"), 
								  0, 
								  data.getByteArrayExtra("signature").length
								  );
```

### resultCode Values
The following table represents the possible resultCode values:

| resultCode | Description |
| :--------------- | :--------------- |
| 00 | Success / Approved |
| Non-zero | Unsuccessful; check the processor response list for the error meaning. |
| -50X | General Application Error |
| -501 | Transaction Cancelled |
