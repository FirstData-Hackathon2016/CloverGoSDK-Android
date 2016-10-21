##Integrating CloverGo Android SDK

The sample app is completely setup to integrate with the CloverGo Android SDK. To set it up on your own project, perform the following

Add both the clovergoreader and clovergosdk AAR files into your project (New > Module > Import .AAR Package)
- clovergoreader AAR file
- clovergoclient AAR file

Add the following dependencies in your project build.gradle file -- make sure the project name you enter below matches your AAR file's module name
```
dependencies {
    ...
    compile project(":clovergosdk")
    compile project(":clovergoreader")
    compile 'com.google.dagger:dagger:2.5'
    compile 'com.squareup.retrofit2:retrofit:2.0.2'
    compile 'io.reactivex:rxandroid:1.2.0'
    compile 'io.reactivex:rxjava:1.1.5'
    compile 'com.squareup.retrofit2:adapter-rxjava:2.0.2'
    compile 'com.squareup.retrofit2:converter-jackson:2.0.2'
}
```

Add the following settings in your project build.gradle file
```
android {
    ...
    packagingOptions {
        exclude 'META-INF/LICENSE'
    }
}
```

Perform a Gradle Sync to verify that everything has been setup completely.

##Initializing the SDK

```
CloverGo mCloverGo = new CloverGo(MainActivity.this, "deviceId", "merchantId", "employeeId");
```

Initialization should be done only once in the Main Activity and set as a member variable, you should get the CloverGo instance rest of the application by accessing the getter method of the member instance.

```
CloverGo mCloverGo = MainActivity.getCloverGo();
```

###Implement the Card Reader Callback

The Fragment that initializes the card reader should implement **_CardReaderCallBack_**

Below are the methods that should be implemented by the Fragment that acts as the **_CardReaderCallBack_**

```
void onConnected();
void onDisconnected();
void onProgress(CardReaderEvent readerEvent);
void onError(CardReaderErrorEvent readerErrorEvent);
void onReady();
void onReaderResetProgress(CardReaderEvent readerEvent);
void onReaderCalibrationProgress(int progress);
void onAidMatch(List<ApplicationIdentifier> applicationIdentifierList,AidSelection aidSelection);
void onDeviceDiscovered(List<CardReaderInfo> discoveredDevices);
```

###Initialize the Card Reader
CardReader needs to be initialized first before your app can start connecting to card readers. Provide the card reader type you want to connect to and the Fragment implementing the **_CardReaderCallBack_**
```
mCloverGo.init(CloverGoConstants.CARD_READER_TYPE.RP450X, MyFragment.this);
```

CloverGo will detect the card reader when connected and will start the initializing process. The cardReaderCallback methods will be called during the different stages of initialization.

###Implement the Transaction Callback

The Fragment that handles the payment transaction processing should implement **_TransactionCallBack_**

Below are the methods that should be implemented by the Fragment that acts as the **_TransactionCallBack_**

```
void onSuccess(T successResponse);
void onFailure(ErrorResponse errorResponse);
boolean proceedOnError(TransactionEvent transactionEvent);
```

###Initiate a Transaction

Once the reader is initialized create the **_TransactionRequest_** instance and the below values, please note all amounts are in pennies

```
List<OrderItem> mOrderItems = new ArrayList<>();
transactionRequest = new TransactionRequest();
transactionRequest.setTips(0);
transactionRequest.setExternalPaymentId("999");
transactionRequest.setOrderItemList(mOrderItems);
```

Call the doReaderTransaction method in CloverGo instance to initiate the transaction

```
mCloverGo.doReaderTransaction(transactionRequest, MyFragment.this);
```

Once the transaction is complete, the response **_TransactionResponse_** will be sent to the **_TransactionCallBack_** method **_onSuccess_**


###Additional Transactional properties

Additional transactional properties can be set on the CloverGo **_TransactionRequest_** instance to 
- Allow Duplicate Transactions

```
setForceDuplicate(true);
```

###Get Inventory

The Fragment that handles the inventory items should implement InventoryCallBack

Below are the methods that should be implemented by the Fragment that acts as the InventoryCallBack

```
void onSuccess(T successResponse);
void onFailure(ErrorResponse errorResponse);
```

To get InventoryResponse which contains a list of Inventory call the loadInventory method in CloverGo

```
mCloverGo.loadInventory(MyFragment.this);
```

###Get Tax Rates

The Fragment that handles the Tax Rates should implement TaxRateCallBack

Below are the methods that should be implemented by the Fragment that acts as the TaxRateCallBack

```
void onSuccess(T successResponse);
void onFailure(ErrorResponse errorResponse);
```

To get a TaxRateResponse which contains a list of TaxRate call the getTaxRates method in CloverGo instance.

```
mCloverGo.loadTaxes(MyFragment.this);
```
