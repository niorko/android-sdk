<h1>Infinario Android SDK</h1>
Infinario Android SDK is available in this Git repository: <a href="https://github.com/7segments/infinario-android-sdk">https://github.com/7segments/infinario-android-sdk</a>.
<h2>Installation (Android Studio / Gradle)</h2>
<ol>
	<li>Download the <a href="https://github.com/7segments/infinario-android-sdk/releases">lastest release</a> of the Android SDK</li>
	<li>Unzip / untar the downloaded SDK into a preferred directory</li>
	<li>In Android Studio, click <strong>File -&gt; Import Module...</strong></li>
	<li>In the opened dialog window, locate the unzipped Android SDK directory and click <strong>Finish</strong></li>
	<li>In Android Studio, click <strong>File -&gt; Project Structure...</strong></li>
	<li>In the opened dialog click on your <strong>app</strong> (on the left side) and open <strong>Dependencies</strong> tab</li>
	<li>Click on the <strong>Plus sign</strong> in the <strong>upper right corner</strong> and select <strong>Module dependency</strong></li>
	<li>Select <strong>:infinario-android-sdk-x.x.x</strong> and click <strong>OK</strong> and then click again <strong>OK</strong></li>
</ol>
After completing the steps above the Infinario Android SDK is now included in your app and ready to be used.
<h2>Usage</h2>
<h3>Basic Interface</h3>
Once the IDE is set up, you may start using the Infinario library in your code. First you need to you need to know the URI of your Infinario API instance, usualy <code>https://api.7segments.com</code> and your <code>companyToken</code> (located at the Company / Overview page in the web application). To interact with the Infinario SDK, you need to obtain an instance of the Infinario class using the <code>application's context</code> and <code>companyToken</code> (the URI parameter is optional):
<pre><code>// Use public Infinario instance
Infinario infinario = Infinario.getInstance(getApplicationContext(), companyToken);

// Use custom Infinario instance
Infinario infinario = Infinario.getInstance(getApplicationContext(), companyToken, "http://url.to.your.instance.com");
</code></pre>
To start tracking, you need to identify the customer with their unique <code>customerId</code>. The unique <code>customerId</code> can either be a String, or an Map representing the <code>customerIds</code> as referenced in <a href="https://docs.7segments.com/technical-guide/rest-client-api/#Detailed_key_descriptions">the API guide</a>. Setting
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
<h2>Push notifications</h2>
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
By default, receiving of push notifications is disabled. You can enable them by calling the method <code>enablePushNotifications()</code>. Please note that this method needs to be called only once. Push notifications remain enabled until the opposite method, <code>disablePushNotifications()</code> is called. Method <code>enablePushNotifications()</code> has one mandatory argument <code>String senderId</code> or <strong>Project number</strong>. The <strong>Project number</strong> can be obtained from <strong>Project Dashboard</strong> of your <strong>Google API project</strong> at <a href="https://console.developers.google.com/">https://console.developers.google.com/</a>. <code>enablePushNotifications()</code> has one optional argument <code>int iconDrawable</code> which is ID of the icon drawable which should be used as icon in the Android notification.
<pre><code>String senderId = "your project number";

// Enable push notifications using given icon
infinario.enablePushNotifications(senderId, R.drawable.icon);
</code></pre>
<h2>Flushing events</h2>
All tracked events are stored in the internal SQL database in the Android app. By default, Infinario Android SDK automagically takes care of flushing events to the Infinario API. This feature can be turned off with method <code>disableAutomaticFlushing()</code> which takes no arguments. Please be careful with turning automatic flushing off because if you turn it off, you need to manually call <code>Infinario.flush(context);</code> to flush the tracked events manually everytime there is something to flush.
