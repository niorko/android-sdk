<h1>Infinario Android SDK</h1>
Infinario Android SDK is available in this Git repository: <a href="https://github.com/infinario/android-sdk">https://github.com/infinario/android-sdk</a>.
<h2>Installation (Android Studio / Gradle)</h2>
<ol>
	<li>Download the <a href="https://github.com/infinario/android-sdk/releases">latest release</a> of the Android SDK</li>
	<li>Unzip / untar the downloaded SDK into a preferred directory</li>
	<li>In Android Studio, click <strong>File -&gt; New -&gt; Import Module...</strong> or in older version click <strong>File -&gt; Import Module...</strong></li>
	<li>In the opened dialog window, locate the unzipped Android SDK directory and click <strong>Finish</strong></li>
	<li>In Android Studio, click <strong>File -&gt; Project Structure...</strong></li>
	<li>In the opened dialog click on your <strong>app</strong> (on the left side) and open <strong>Dependencies</strong> tab</li>
	<li>Click on the <strong>Plus sign</strong> in the <strong>upper right corner</strong> and select <strong>Module dependency</strong></li>
	<li>Select <strong>:infinario-android-sdk-x.x.x</strong> and click <strong>OK</strong> and then click again <strong>OK</strong></li>
</ol>
After completing the steps above the Infinario Android SDK is now included in your app and ready to be used.
<h2>Usage</h2>
<h3>Basic Interface</h3>
Once the IDE is set up, you may start using the Infinario library in your code. First you need to you need to know the URI of your Infinario API instance, usually <code>https://api.infinario.com</code> and your <code>projectToken</code> (Overview page in the web application). To interact with the Infinario SDK, you need to obtain an instance of the Infinario class using the <code>application's context</code> and <code>projectToken</code> (the URI parameter is optional):
<pre><code>// Use public Infinario instance
Infinario infinario = Infinario.getInstance(getApplicationContext(), projectToken);

// Use custom Infinario instance
Infinario infinario = Infinario.getInstance(getApplicationContext(), projectToken, "http://url.to.your.instance.com");
</code></pre>
To start tracking, you need to identify the customer with their unique <code>customerId</code>. The unique <code>customerId</code> can either be a String, or an Map representing the <code>customerIds</code> as referenced in <a href="http://guides.infinario.com/technical-guide/rest-client-api/#Detailed_key_descriptions">the API guide</a>. Setting
<pre><code>String customerId = "123-foo-bar"</code></pre>
is equivalent to
<pre><code>Map&lt;String, String&gt; customerId = new HashMap&lt;&gt; ();
customerId.put("registered", "123-foo-bar");</code></pre>
In order to identify a customer, call the <code>identify()</code> method on the obtained Infinario instance as follows:
<pre><code>// Identify a customer with their customerId
infinario.identify(customerId);</code></pre>
The identification is performed asynchronously and there is no need to wait for it to finish. All tracked events are stored in the internal SQL database until they are sent to the Infinario API.

You may track any event by calling the <code>track()</code> method on the Infinario instance. The <code>track()</code> method takes one mandatory and two optional arguments. First argument is <code>String type</code> which categorizes your event. This argument is <strong>required</strong>. You may choose any string you like.

Next two arguments are <code>Map&lt;String, Object&gt; properties</code> and <code>Long timestamp</code>. Properties is a map which uses <code>String</code> keys and the value may be any <code>Object</code> which is serializable by <code>org.json.JSONObject</code> class. Properties can be used to attach any additional data to the event. Timestamp is standard UNIX timestamp in seconds and it can be used to mark the time of the event's occurence. The default timestamp is preset to the time of the tracking of the event.
<pre><code>Map&lt;String, Object&gt; properties = new HashMap&lt;&gt;();
properties.put("item_id", 45);
Long timestamp = System.currentTimeMillis() * 1000L;

// Tracking of buying an item with item's properties at a specific time
track("item_bought", properties, timestamp);

// Tracking of buying an item at a specific time
track("item_bought", timestamp);

// Tracking of buying an item with item's properties
track("item_bought", properties);

// Basic tracking that an item has been bought
track("item_bought");
</code></pre>
The Infinario Android SDK provides you with means to store arbitrary data that is not event-specific (e.g. customer's age, gender, initial referrer). Such data is tied directly to the customer as their properties. The <code>update()</code> method is used to store such data:
<pre><code>Map&lt;String, Object&gt; properties = HashMap&lt;&gt; ();
properties.put("age", 34);

// Store customer's age
infinario.update(properties);
</code></pre>

<h2>Automatic events</h2>
<p>
INFINARIO Android SDK automatically tracks some events on its own. Automatic events ensure that basic user data gets tracked with as little effort as just including the SDK into your game. Automatic events include sessions, installation, identification and payments tracking.
</p>

<h3>Sessions</h3>
<p>
Session is a real time spent in the game, it starts when the game is launched and ends when the game goes to background. But if the player returns to game in 60 seconds (To change TIMEOUT value, call <code>setSessionTimeOut</code>), game will continue in current session. Tracking of sessions produces two events, <code>session_start</code> and <code>session_end</code>. To track session start call <code>trackSessionStart()</code> from where whole game gets focus (e.g. onStart method) and to track session end call <code>trackSessionEnd()</code> from where whole game loses focus (e.g. onStop method). <b>If you have more activities, put them to all activities you have.</b></p>
<pre><code>Infiario infinario;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infinario = Infinario.getInstance(getApplicationContext(), "projectToken");
    }

    @Override
    protected void onStart(){
        super.onStart();

        infinario.trackSessionStart();

        /* or with properties

        <pre><code>Map&lt;String, Object&gt; properties = new HashMap&lt;&gt;();
        properties.put("item_id", 45);

        infinario.trackSessionStart(properties);
        */
    }

    @Override
    protected void onStop(){
        super.onStop();

        infinario.trackSessionEnd();

        /* or with properties

        <pre><code>Map&lt;String, Object&gt; properties = new HashMap&lt;&gt;();
        properties.put("item_id", 45);

        infinario.trackSessionEnd(properties);
        */
    }</code></pre>
<p>Both events contain the timestamp of the occurence together with basic attributes about the device (OS, OS version, SDK, SDK version and device model). Event <code>session_end</code> contains also the duration of the session in seconds. Example of <code>session_end</code> event attributes in <em>JSON</em> format:
</p>

<pre><code>{
  "duration": 125,
  "device_model": "LGE Nexus 5",
  "device_type": "mobile",
  "ip": "10.0.1.58",
  "os_name": "Android",
  "os_version": "5.0.1",
  "sdk": "AndroidSDK",
  "sdk_version": "1.1.4"
  "app_version": "1.0.0"
}
</code></pre>

<h3>Installation</h3>

<p>
Installation event is fired <strong>only once</strong> for the whole lifetime of the game on one device when the game is launched for the first time. Besides the basic information about the device (OS, OS version, SDK, SDK version and device model), it also contains additional attribute called <strong>campaign_id</strong> which identifies the source of the installation. For more information about this topic, please refer to the <a href="http://guides.infinario.com/user-guide/acquisition/">aquisition documentation</a>. Please note that <code>com.android.vending.INSTALL_REFERRER</code> intent is used to acquire the source of the installation. Example of installation event:
</p>

<pre><code>{
  "campaign_name": "Advertisement on my website",
  "campaign_id": "ui9fj4i93jf9083094fj9043",
  "link": "https://play.google.com/store/...",
  "device_model": "LGE Nexus 5",
  "device_type": "mobile",
  "ip": "10.0.1.58",
  "os_name": "Android",
  "os_version": "5.0.1",
  "sdk": "AndroidSDK",
  "sdk_version": "1.1.4"
}
</code></pre>

<h3>Identification</h3>

<p>
Identification event is tracked each time the <code>identify()</code> method is called. It contains all basic information regarding the device (OS, OS version, SDK, SDK version and device model) as well as <strong>registered</strong> attribute which identifies the player. Example of an identification event:
</p>

<pre><code>{
  "registered": "player@email.com",
  "device_model": "LGE Nexus 5",
  "device_type": "mobile",
  "ip": "10.0.1.58",
  "os_name": "Android",
  "os_version": "5.0.1",
  "sdk": "AndroidSDK",
  "sdk_version": "1.1.4"
}
</code></pre>

<h3>Payments for Google Play Store</h3>

<p>
In order to use the automatic payment tracking, the INFINARIO Android SDK needs to know when a purchase flow has finished. After the player goes through the whole purchase process, Android calls <code>onActivityResult</code> method on the activity which started the purchase flow. That is the place in the game code where INFINARIO Android SDK instance needs to get notified about the purchase using the <code>trackGooglePurchases(int, Intent)</code> method. For more details, please refer to the sample code below:
</p>

<pre><code>// override this method in each activity which starts the purchase flow
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // first check if the infinario instance has been initialized
    if (infinario != null) {
        // pass resultCode and the intent, INFINARIO Android SDK automatically
        // determines whether this is a purchase intent or not
        infinario.trackGooglePurchases(resultCode, data);
    }
}
</code></pre>

<h3>Payments for Amazon Store</h3>

<p>Implementation is made by 2 steps.</p>

<p>
First, you must get product's information by call method <code>loadAmazonProduct(JSONObject amazonJsonProductDataResponse)</code> from Amazon SDK Purchase Listener <code>onProductDataResponse(final ProductDataResponse response)</code>.
</p>

<pre><code>@Override
public void onProductDataResponse(final ProductDataResponse response) {
    final ProductDataResponse.RequestStatus status = response.getRequestStatus();

    switch (status) {
        case SUCCESSFUL:
            final Set unavailableSkus = response.getUnavailableSkus();
            try {
                infinario.loadAmazonProduct(response.toJSON());
            } catch (JSONException e) {
            }
            iapManager.enablePurchaseForSkus(response.getProductData());
            iapManager.disablePurchaseForSkus(response.getUnavailableSkus());
            break;
        case FAILED:
        ......
</code></pre>

<p>
Second, call method <code>trackPurchases(JSONObject amazonJsonPurchaseResponse)</code> in Amazon SDK Purchase Listener <code>onPurchaseResponse(final PurchaseResponse response)</code> to track Amazon purchases.
</p>

<pre><code>@Override
public void onPurchaseResponse(final PurchaseResponse response) {
    final PurchaseResponse.RequestStatus status = response.getRequestStatus();

    switch (status) {
        case SUCCESSFUL:
            final Receipt receipt = response.getReceipt();
            try {
                infinario.trackAmazonPurchases(response.toJSON());
            } catch (JSONException e) {
            }
            iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
            iapManager.handleReceipt(receipt, response.getUserData());
            iapManager.refreshOranges();
            break;
        case ALREADY_PURCHASED:
        .....
</code></pre>

<p>
Purchase events (called <code>hard_purchase</code>) contain all basic information about the device (OS, OS version, SDK, SDK version and device model) combined with additional purchase attributes <strong>brutto</strong>, <strong>currency</strong>, <strong>product_id</strong> and <strong>product_title</strong>. <strong>Brutto</strong> attribute contains price paid by the player. Attribute <strong>product_title</strong> consists of human-friendly name of the bought item (e.g. Silver sword) and <strong>product_id</strong> corresponds to the product ID for the in-app purchase as defined in your Google Play / Amazon Developer Console. Example of purchase event: 
</p>

<pre><code>{
  "brutto": 0.911702,
  "currency": "EUR",
  "item_id": "android.test.purchased",
  "item_title": "Silver sword",
  "device_model": "LGE Nexus 5",
  "device_type": "mobile",
  "ip": "10.0.1.58",
  "os_name": "Android",
  "os_version": "5.0.1",
  "sdk": "AndroidSDK",
  "sdk_version": "1.1.4"
}
</code></pre>

<h2>Virtual payment</h2>

<p>If you use in your project some virtual payments (e.g. purchase with in-game gold, coins, ...), now you can track them with simple call <code>trackVirtualPayment</code>.</p>

<pre><code>Infinario.trackVirtualPayment(String currency, int amount, String itemName, String itemType);</code></pre>

<h2>Segmentation</h2>

<p>If you want to get current segment of your player, just call <code>getCurrentSegment</code>. You will need id of your segmentation and project secret token.</p>

<pre><code>infinario.getCurrentSegment("segmentation_id", "project_secret", new SegmentListener() {
    @Override
    public void onSegmentReceive(boolean wasSuccessful, InfinarioSegment segment, String error) {
        String name = segment.getName();
    }
});</code></pre>

<h2>Google Push notifications</h2>
Infinario web application allows you to easily create complex scenarios which you can use to send push notifications directly to your customers. The following section explains how to enable receiving push notifications in the Infinario Andriod SDK.

For push notifications to work, you need a working Google API project. The following steps show you how to create one. If you already have created a Google API project and you have your <strong>project number (or sender ID)</strong> and <strong>Google Cloud Messaging API key</strong>, you may skip this part of the tutorial and proceed directly to <a href="#infinario-web-app">enabling of the push notifications</a> in the Infinario Android SDK.
<h3>Google API project</h3>
<ol>
	<li>In your preferred browser, navigate to <a href="https://console.developers.google.com/">https://console.developers.google.com/</a></li>
	<li>Click on <strong>Create Project</strong> button</li>
	<li>Fill in preferred project name and click <strong>Create</strong> button</li>
	<li>Please wait for the project to create, it usually takes only a few seconds</li>
	<li>After the project has been created you will be redirected to the <strong>Project Dashboard</strong> page where you'll find <strong>Project Number</strong> which is needed in the Infinario Android SDK</li>
	<li>In the left menu, navigate to <strong>APIs &amp; auth -&gt; APIs</strong> and find <strong>Google Cloud Messaging for Android</strong></li>
	<li>Please make sure the Google Cloud Messaging for Android is <strong>turned ON</strong></li>
	<li>In the left menu, navigate to <strong>APIs &amp; auth -&gt; Credentials</strong> and click on <strong>Create new Key</strong> button</li>
	<li>Click on <strong>Server key</strong> button and the click on <strong>Create</strong> button</li>
	<li>Copy the API key which is needed for the Infinario web application</li>
</ol>
<h3 id="infinario-web-app">Infinario web application</h3>
Once you have obtained <strong>Google Cloud Messaging API key</strong>, you need to enter it in the input field on the <strong>Company / Settings / Notifications</strong> in the Infinario web application.

<h3>Infinario Android SDK</h3>
By default, receiving of push notifications is disabled. You can enable them by calling the method <code>enableGooglePushNotifications()</code>. Please note that this method needs to be called only once. Push notifications remain enabled until the opposite method, <code>disableGooglePushNotifications()</code> is called. Method <code>enableGooglePushNotifications()</code> has one mandatory argument <code>String senderId</code> or <strong>Project number</strong>. The <strong>Project number</strong> can be obtained from <strong>Project Dashboard</strong> of your <strong>Google API project</strong> at <a href="https://console.developers.google.com/">https://console.developers.google.com/</a>. <code>enableGooglePushNotifications()</code> has one optional argument <code>int iconDrawable</code> which is ID of the icon drawable which should be used as icon in the Android notification.
<pre><code>String senderId = "your project number";

// Enable push notifications using given icon
infinario.enableGooglePushNotifications(senderId, R.drawable.icon);
</code></pre>
<h2>Flushing events</h2>
All tracked events are stored in the internal SQL database in the Android app. By default, Infinario Android SDK automagically takes care of flushing events to the Infinario API. This feature can be turned off with method <code>disableAutomaticFlushing()</code> which takes no arguments. Please be careful with turning automatic flushing off because if you turn it off, you need to manually call <code>Infinario.flush(context);</code> to flush the tracked events manually everytime there is something to flush.