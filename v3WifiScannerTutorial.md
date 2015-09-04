# Wifi Scanner Tutorial #
This tutorial will walk you through building a basic Android application that uses Funf.
We will start with the template, and will end with an android application that periodically scans
for Wifi access points and their location, and also has an interface for scanning on demand, and viewing the results.



## Seting up the project ##

Get latest template from the repository:
```
git clone https://code.google.com/p/funf-open-sensing-framework.samples/ temp_repo
git --work-tree=temp_repo/ --git-dir=temp_repo/.git archive --format=tar --prefix=wifi_scanner/ HEAD:template/ | tar xf -
rm -Rf temp_repo
```

Create Eclipse Project:
```
File > New > Android Project
Enter Project Name: wifi_scanner
Create Project From Existing Source
Select directory for wifi_scanner
Finish
```

Create a local.properties file so the build script can find your Android SDK.  As an example:
```
sdk.dir=/path/to/android-sdk-linux_x86
```

In build.xml change:
```
<project name="FunfCollector"
to 
<project name="WifiScanner"
```

In res/values/strings.xml change:
```
<string name="app_name">Funf Collector</string>
to 
<string name="app_name">Wifi Scanner</string>
```

Rename package to custom app package:
```
Right click package "edu.mit.media.funf.bgcollector"
Select Refactor > Rename...
Enter "edu.mit.media.funf.wifiscanner" as the new package names
Make sure all of the check boxes are checked
Click OK
```

## Source code overview ##

The `MainPipeline` class is a custom implementation of `ConfiguredPipeline`.
It is the service that is responsible for reading configuration files and making sure that probes have the appropriate data requests.
When probes emit data, the pipeline will take those data requests, serialize them to json, and send them to the Database service to be encrypted and stored.
`ConfiguredPipeline` classes have hooks to control what happens when data is requested, and what happens when data is received.

The `LauncherReceiver` has the simple job of keeping the `MainPipeline` service on.  It listens for common system events and simply uses them to start the `MainPipeline` service.  `JsonUtils` uses Gson to serialize Funf data in intents into Json strings.


## Configuring Collection ##

The `MainPipeline` bootstraps its configuration using the file assets/default\_config.json.
This file is only read once the first time the application is installed.
We will set this up to collect Wifi and Location data.

```
{       
	"name": "WifiScanner",
    "version":1,
    "dataArchivePeriod":3600,
    "dataRequests":{
        "edu.mit.media.funf.probe.builtin.LocationProbe": [
            { "PERIOD": 900, "DURATION": 30 }
        ],
        "edu.mit.media.funf.probe.builtin.WifiProbe": [
            { "PERIOD": 900 }
        ]
    }
}
```

Ensure that you have the following services and receivers defined, but the remaining ones can be removed.
```
<!-- Probe Services -->
<service android:name="edu.mit.media.funf.probe.builtin.LocationProbe"></service>
<service android:name="edu.mit.media.funf.probe.builtin.WifiProbe"></service>

<!-- Framework services -->
<service android:name=".MainPipeline"></service>
<service android:name="edu.mit.media.funf.storage.NameValueDatabaseService"></service>
<receiver android:name=".LauncherReceiver" android:enabled="true">
	<intent-filter>
		<action android:name="android.intent.action.BATTERY_CHANGED" />
		<action android:name="android.intent.action.BOOT_COMPLETED" />
		<action android:name="android.intent.action.DOCK_EVENT" />
		<action android:name="android.intent.action.ACTION_SCREEN_ON" />
		<action android:name="android.intent.action.USER_PRESENT" />
	</intent-filter>
</receiver>
```

We want to limit the permissions that are available to this application, so that users can be confident in what we are collecting.
```
    <!-- Launching -->
    <uses-permission android:name="android.permission.BATTERY_STATS" />
    
    <!-- All probes -->
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    
    <!-- Location probe -->
	<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/> 
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>   
    
    <!-- Wifi probe -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/> 
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/> 

    <!-- Storage -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/> 
```

## Running the Application ##
While the new application does not have an interface, it now has all of the components necessary to run.  It will  will gather the requested data and periodically store it on the SD card.

Install the application using either "ant install", or in Eclipse by going to "Run -> Run As -> Android Application".

Since there is no activity currently, you can verify the application is running by opening "Settings -> Appplications -> Running Services" on your Android device.  In this list should be the "Wifi Scanner" application.  If it is not there, try turning off the screen and and turning it back on as this should start the pipeline service.
Tap the Wifi Scanner item to see the services it is running, which include the `MainPipeline`, the `WifiProbe`, and the `LocationProbe`.  These services will be active as long as there are data requests for them.

## Basic interface ##
To make this application a bit more user friendly we would like to give a few options to control the data collection.  We are going to give the user the ability to enable and disable collection, as well as to scan immediately if they know they want wifi data from this spot.

Create a new Activity called `MainActivity.java`.  Set it up to be launchable from the app list by adding the following to the `AndroidManifest.xml`.
```
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Next, let's create a layout called "res/layout/main.xml" so we can easily control how this activity looks.  In this basic interface we will put
  * Some text explaining what the app does
  * A checkbox to enable or disable data collection
  * A button to move all data to the SD card immediately
```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
         android:orientation="vertical"
         android:layout_width="fill_parent"
         android:layout_height="fill_parent">
    <TextView android:text="Wifi Scanner periodically scans your location and surrounding wifi access points, and saves that information." 
		    android:layout_width="wrap_content" 
		    android:layout_height="wrap_content"/>
    <LinearLayout android:layout_width="fill_parent" 
	    android:layout_height="wrap_content"
	    android:orientation="horizontal" >
	    <TextView android:text="Enabled" 
		    android:layout_width="wrap_content" 
		    android:layout_height="wrap_content"/>
		<CheckBox android:id="@+id/enabledCheckbox" 
			android:layout_width="wrap_content" 
		    android:layout_height="wrap_content"/>
	</LinearLayout>
	<Button android:id="@+id/archiveButton"
			android:text="Save Data to SD Card" 
		    android:layout_width="fill_parent" 
		    android:layout_height="wrap_content"/>
</LinearLayout>
```

Now we'll override the onCreate method of the `MainActivity` to respond to events from our new buttons.
When buttons are clicked the activity will send intents to our `MainPipeline` to tell it to Enable, Disable, or Archive the existing data to the SD Card.
```
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		final Context context = this;
		
		CheckBox enabledCheckbox = (CheckBox)findViewById(R.id.enabledCheckbox); 
		enabledCheckbox.setChecked(MainPipeline.isEnabled(context));
		enabledCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent archiveIntent = new Intent(context, MainPipeline.class);
				String action = isChecked ? MainPipeline.ACTION_ENABLE : MainPipeline.ACTION_DISABLE;
				archiveIntent.setAction(action);
				startService(archiveIntent);
			}
		});
		
		Button archiveButton = (Button)findViewById(R.id.archiveButton);
		archiveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent archiveIntent = new Intent(context, MainPipeline.class);
				archiveIntent.setAction(MainPipeline.ACTION_ARCHIVE_DATA);
				startService(archiveIntent);
			}
		});
	}
```

Now install the new application and check out the new interface.
If you run into any issues with these steps, trying looking over the [Android Activity Tutorial](http://developer.android.com/guide/topics/fundamentals/activities.html).


## Live Interaction ##
Our interface allows users to start and stop collection of data, but offers no feedback about whether data is actually being collected.
To fix this, we are going to implement a data collection count to display to the screen.  So that users don't have to see wait 15 minutes to see it work, we are going to implement a button to initiate a wifi scan now.

### Data Count ###
First of all let's start keeping track of data that is received with a simple counter.  In Main Pipeline add the following code:
```
	public static final String SCAN_COUNT_KEY = "SCAN_COUNT";
	
	public static long getScanCount(Context context) {
		return getSystemPrefs(context).getLong(SCAN_COUNT_KEY, 0L);
	}

	private void incrementCount() {
		boolean success = false;
		while(!success) { 
			SharedPreferences.Editor editor = getSystemPrefs().edit();
			editor.putLong(SCAN_COUNT_KEY, getScanCount(this) + 1L);
			success = editor.commit();
		}
	}
```

And ensure that the number gets incremented when the pipeline receives data by overriding the onDataReceived method.
```
@Override
public void onDataReceived(Bundle data) {
	super.onDataReceived(data);
	incrementCount();
}
```

Now let's add an element to display this data count.
In the main.xml layout file we add our UI element.
```
<TextView android:id="@+id/dataCountText" 
	    android:layout_width="wrap_content" 
	    android:layout_height="wrap_content"/>
```

Lastly, let's set the value in our TextView and register for updates on the SharedPreferences class to make sure this text view is updated in realtime.
Add the following to our MainActivity class:
```
@Override
public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	Log.i("WifiScanner", "SharedPref change: " + key);
	if (MainPipeline.SCAN_COUNT_KEY.equals(key)) {
		updateScanCount();
	}
}

private void updateScanCount() {
	TextView dataCountView = (TextView)findViewById(R.id.dataCountText);
	dataCountView.setText("Data Count: " + MainPipeline.getScanCount(this));
}
```

In the onCreate method add:
```
MainPipeline.getSystemPrefs(this).registerOnSharedPreferenceChangeListener(this);
updateScanCount();
```

### Scan Now Button ###
Users may find the need to scan at a particular moment.  Let's create the UI element in main.xml.
```
<Button android:id="@+id/scanNowButton"
		android:text="Scan Now" 
	    android:layout_width="fill_parent" 
	    android:layout_height="wrap_content"/>
```


Then set the button functionality by adding the following code to onCreate in MainActivity.java to enable the scan now button.
```
Button scanNowButton = (Button)findViewById(R.id.scanNowButton);
scanNowButton.setOnClickListener(new OnClickListener() {
	@Override
	public void onClick(View v) {
		Intent runOnceIntent = new Intent(context, MainPipeline.class);
		runOnceIntent.setAction(MainPipeline.ACTION_RUN_ONCE);
		runOnceIntent.putExtra(MainPipeline.RUN_ONCE_PROBE_NAME, WifiProbe.class.getName());
		startService(runOnceIntent);
		runOnceIntent.putExtra(MainPipeline.RUN_ONCE_PROBE_NAME, LocationProbe.class.getName());
		startService(runOnceIntent);
	}
});
```

<a href='Hidden comment: 
== Coordinating Probe Schedules ==
One of the challenges we have is to enforce that location data be gathered at the same time wifi data is collected.  Otherwise we will be missing the location of wifi access points.  To ensure this we need to either take control of the scheduling ourself, or use Funf Triggers to coordinate scans.
'></a>

## Getting Data ##
Data collected by Funf is periodically archived to the SDCard as encrypted SQLite database files.  They can be found at:"/sdcard/edu.mit.media.funf.wifiscanner/main".

## Data analysis ##
Once you have your encrypted database files, you can decrypt and merge them using the Funf scripts package.  Details on how to use these scripts can be found in the [README](http://code.google.com/p/funf-open-sensing-framework/source/browse/README?repo=scripts).

A typical usage would be
```
> python dbdecrypt.py *.db
... enter password (changeme by default)
> python dbmerge.py *.db
```

This will produce a SQlite database file with data from all of the individual files.
Check out ProcessingData for details about the table structure.
From here you can either use the [SQLite command line program](http://www.sqlite.org/download.html), [SQLite Database Browser](http://sqlitebrowser.sourceforge.net/), or write a custom script.




## Advanced ##
Coming soon
  * Creating a custom probe (Free Wifi Probe)
  * Remotely configuring
  * Automatically Uploading data, Force Upload Button