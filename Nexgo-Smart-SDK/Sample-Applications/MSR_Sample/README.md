# MSRSample
> An MSR sample application that is able to read an MSR swiped card and display the card number in a toast and in logcat. Utilizes the Nexgo SmartSDK to interact with the card reader. 
> 
> The .aar library is already imported/configured inside the project. You can build/install this project as-is to a NEXGO N-series payment terminal from Android Studio directly, or by building an APK and installing it onto the terminal manually. 

## Table of Contents
* [Importing the Sample Project](#importing-the-sample-project)
* [Sample Project Flow Description](#sample-project-flow-description)

## Importing the Sample Project
To download and use the project:
  1. Download the project by downloading the zip directly, or by using the git clone command
  2. Open Android Studio on your computer
  3. Select New > Import Project...
  4. Select the project from the Import dialog window that appears

## Sample Project Flow Description
The Sample MSR application is fairly straightforward. 
1. Initialize the `DeviceEngine` object `deviceEngine` using the APIProxy. 
2. Initialize the `CardReader` object `cardReader` using the deviceEngine. 
3. When the 'button' is pressed in the application, it triggers the `cardReader.searchCard(..)` function. 

When the `searchCard(HashSet<CardSlotTypeEnum> slotTypes, int cardSearchTimeoutSeconds, OnCardInfoListener listener)` function is called, it begins searching for a card swipe event. When such an event occurs, the function will trigger a callback to one of the functions of the `OnCardInfoListener`. 
```java
 //Begin 'searching' for the card swipe. Once swiped, the callback methods will be called to handle the swipe.
 cardReader.searchCard(slotTypes, cardSearchTimeoutSeconds, MainActivity.this);
```

The `OnCardInfoListener` contains the following functions, one of which will be called by the CardReader after a card swipe event is detected:
* `onCardInfo(..)`
> Once the user has called the 'searchCard' function, when the application detects a valid cardSwipe (i.e. not a swipe error or multiple cards) it will call this method.
```java
   @Override
   public void onCardInfo(int i, CardInfoEntity cardInfoEntity) {
	...
   }
```
> The cardSwipe information is contained within the cardInfoEntity object. You can extract the card number from the CardInfoEntity object returned in the callback with the `.getTk1()` function. 
```java
   @Override
   public void onCardInfo(int i, CardInfoEntity cardInfoEntity) {

     if (cardInfoEntity == null)
     {
         //if cardInfo returned is null, there is issue. Break out of the method.
         Log.e(TAG, "Received cardInfoEntity that was NULL. Breaking.");
         return;
     }

     //cardInfo is not null; Store the track data parsed from the cardSwipe.
     String trackData = cardInfoEntity.getTk1();
```

> We should call the `stopSearch` method once a card is swiped to close the reader.
```java
   @Override
   public void onCardInfo(int i, CardInfoEntity cardInfoEntity) {

     //We got the card swipe, now we should stop listening for additional swipes.
     cardReader.stopSearch();
     Log.d(TAG, "Called stopSearch()");

     if (cardInfoEntity == null)
     {
         //if cardInfo returned is null, there is issue. Break out of the method.
         return;
     }

     //cardInfo is not null; Store the track data parsed from the cardSwipe.
     String trackData = cardInfoEntity.getTk1();
   }
```

* `onSwipeIncorrect(..)`
> If there was an error reading the swiped card, such as in a bad swipe (angled, too quick, etc.) - this method will be called by the cardReader class. You can either call stopSearch to tell the CardReader to stop listening for card swipes, or you can show message to user like 'Swipe Error' and keep the reader open to allow the user to try an additional swipe.
```java
   @Override
   public void onSwipeIncorrect() {
       Log.e(TAG, "Swipe Error!");

   }
```
* `onMultipleCards(..)`
> This method is called when multiple cards are detecting in the cardRead method. This should *not* occur for an MSR swipe, but can occur for NFC for example if multiple cards are tapped onto the device.
```java
   @Override
   public void onMultipleCards() {
       Log.e(TAG, "Received Multiple Cards!");
   }
```
## Miscellaneous

