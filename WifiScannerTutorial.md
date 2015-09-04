# Wifi Scanner Tutorial #
One of the big goals for Funf v0.4 was to make creating new apps simple.  This is a basic tutorial of how to build your own Funf app.  (Compare with the [Funf v0.3 Tutorial](v3WifiScannerTutorial.md) to see how much simpler things have gotten!)  This tutorial assumes you know the basics of creating an Android application.  If you are new to Android, start with Android’s Building Your First App tutorial. ( http://developer.android.com/training/basics/firstapp/index.html )
At the end of this tutorial you will have made an android application that periodically scans for Wifi access points and their location, and also has an interface for scanning on demand, and viewing the results.

First things first, create a brand new Android app.  You can do this using either Eclipse or the command line tools provided with the Android SDK.  (http://developer.android.com/training/basics/firstapp/creating-project.html )  Funf only supports API 8 and above, so add the following line to the android manifest:
```
<uses-sdk android:minSdkVersion="8" android:targetSdkVersion="17" />
```

## Required Libraries ##

First of all, you need to have the Funf library .jar file in your project's "libs" folder. In addition, you'll need to download a modified version of the GSON library and place it in the "libs" folder as well. Both can be found [here](https://code.google.com/p/funf-open-sensing-framework/downloads/list).

## Permissions ##

There are some permissions that are required by Funf, and others that are optional.  The required permissions should be added to the `AndroidManifest.xml`.

```
    <!-- Launching -->
    <uses-permission android:name="android.permission.BATTERY_STATS" />
   
    <!-- All probes -->
    <uses-permission android:name="android.permission.WAKE_LOCK"/>

    <!-- Storage -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

Since we will be using the WifiProbe and the LocationProbe we also have to add the following optional permissions:

```
    <!-- Location probe -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>  

    <!-- Wifi probe -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
```

For any given built-in probe, you can determine which permissions are required by looking for the @RequiredPermissions annotation on the class.  If you forget to add the required permissions than your app will crash with a system error message in the log, so remember to ensure you have all of the necessary permissions for your use case

## The FunfManager Service ##

The FunfManager service is responsible for managing and running all pipelines, probes, and schedules.  To allow the service to run you must declare it in the AndroidManifest.xml within the `<application>` tag. To ensure it continues to run, even after reboots and crashes, we also add the Launcher broadcast receiver which has the single job of keeping the FunfManager running.

```
<service android:name="edu.mit.media.funf.FunfManager">
    <meta-data android:name="default" android:value="@string/default_pipeline"/>
</service>
<receiver android:name="edu.mit.media.funf.Launcher" android:enabled="true">
    <intent-filter>
        <action android:name="android.intent.action.BATTERY_CHANGED" />
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.DOCK_EVENT" />
        <action android:name="android.intent.action.ACTION_SCREEN_ON" />
        <action android:name="android.intent.action.USER_PRESENT" />
    </intent-filter>
</receiver>
```

Every `<meta-data>` tag inside of FunfManager sets up a pipeline.  The name of the pipeleine is in the android:name property, and the configuration is in the android:value property.  To allow for easy readability, it is recommended you put the configuration in the res/values/strings.xml file instead of inline in the android manifest.

## Pipeline Configuration ##

In the last section we defined a pipeline called “default” and told the FunfManager that the configuration was in the res/values/strings.xml file.  Let’s build that configuration file.  Below is a basic configuration in your strings.xml file.

```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Funf Collector</string>
    <string name="default_pipeline">
        {"@type":"edu.mit.media.funf.pipeline.BasicPipeline",
         "name":"default",
         "version":1,
         "data":[
            "edu.mit.media.funf.probe.builtin.WifiProbe",
            "edu.mit.media.funf.probe.builtin.LocationProbe"
         ]
        }
    </string>
</resources>
```

As we update the pipeline config the examples will only show the JSON string inside the “default\_pipeline” element, but remember the entire config is surrounded by the string tag.

All kinds of pipelines can be configured, but this tutorial will focus only on the BasicPipeline, which is the only implementation of a pipeline that comes bundled with the Funf library.  This pipeline will record data to an SQLite file, archive the files periodically, and optionally upload those files.  The configuration we defined created a BasicPipeline which will run to the WifiProbe and the LocationProbe on the default schedule.
Running the Application

While the new application does not have an interface to speak of, it now has all of the components necessary to run. It will will gather the requested data and periodically store it on the SD card.
Install the application using either "ant install", or in Eclipse by going to "Run -> Run As -> Android Application".

Since there is no activity currently, you can verify the application is running by opening "Settings -> Appplications -> Running Services" on your Android device. In this list should be the "Wifi Scanner" application. If it is not there, try turning off the screen and and turning it back on as this should start the pipeline service. Tap the Wifi Scanner item to see the FunfManager service is running.

## Basic interface ##

To make this application a bit more user friendly we should give a few options to control the data collection. We are going to give the user the ability to enable and disable collection, as well as to scan immediately if they know they want wifi data at a given time.

Let’s start by creating a new Activity called MainActivity.java. Set it up to be launchable from the app list by adding the following to theAndroidManifest.xml.

```
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Next, let's create a layout called "res/layout/main.xml" so we can easily control how this activity looks. In this basic interface we will put
Some text explaining what the app does
A checkbox to enable or disable data collection
A button to move all data to the SD card immediately

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

Now we'll override the onCreate method of the MainActivity to respond to events from our new buttons. When buttons are clicked the activity will send an action to the pipeline, or enable/disable the pipeline entirely in the FunfManager.
```
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    // Displays the count of rows in the data
    dataCountView = (TextView) findViewById(R.id.dataCountText);
    
    // Used to make interface changes on main thread
    handler = new Handler();
    
    enabledCheckbox = (CheckBox) findViewById(R.id.enabledCheckbox);
    enabledCheckbox.setEnabled(false);

    // Runs an archive if pipeline is enabled
    archiveButton = (Button) findViewById(R.id.archiveButton);
    archiveButton.setEnabled(false);
    archiveButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (pipeline.isEnabled()) {
                pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);
              
                // Wait 1 second for archive to finish, then refresh the UI
                // (Note: this is kind of a hack since archiving is seamless and there are no messages when it occurs)
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "Archived!", Toast.LENGTH_SHORT).show();
                        updateScanCount();
                    }
                }, 1000L);
            } else {
                Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
            }
        }
    });
    
    // Bind to the service, to create the connection with FunfManager
    bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
}
```

Finally, let’s do some general setup and get a reference to the FunfManager when the service binds.  From the FunfManager we can get access to the same probe objects that all pipelines will use, as well as the pipelines themselves.

```
public class MainActivity extends Activity implements DataListener {

public static final String PIPELINE_NAME = "default";
private FunfManager funfManager;
private BasicPipeline pipeline;
private WifiProbe wifiProbe;
private SimpleLocationProbe locationProbe;
private CheckBox enabledCheckbox;
private Button archiveButton, scanNowButton;
private TextView dataCountView;
private Handler handler;
private ServiceConnection funfManagerConn = new ServiceConnection() {    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        funfManager = ((FunfManager.LocalBinder)service).getManager();
        
        Gson gson = funfManager.getGson();
        wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
        locationProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
        pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
        wifiProbe.registerPassiveListener(MainActivity.this);
        locationProbe.registerPassiveListener(MainActivity.this);
        
        // This checkbox enables or disables the pipeline
        enabledCheckbox.setChecked(pipeline.isEnabled());
        enabledCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (funfManager != null) {
                    if (isChecked) {
                        funfManager.enablePipeline(PIPELINE_NAME);
                        pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
                    } else {
                        funfManager.disablePipeline(PIPELINE_NAME);
                    }
                }
            }
        });
    
        // Set UI ready to use, by enabling buttons
        enabledCheckbox.setEnabled(true);
        archiveButton.setEnabled(true);
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
        funfManager = null;
    }
};
```

Now install the new application and check out the new interface. If you run into any issues with these steps, trying looking over the [Android Activity Tutorial](http://developer.android.com/guide/topics/fundamentals/activities.html).

## Live Interaction ##

Our interface allows users to start and stop collection of data, but offers no feedback about whether data is actually being collected. To fix this, we are going to implement a data collection count to display to the screen. So that users don't have to wait 15 minutes to see it work, we are going to implement a button to initiate a wifi scan now.
Data Count

First of all let's start keeping track of data that is received with a simple counter. In MainActivity add the following code:

```
private static final String TOTAL_COUNT_SQL = "SELECT count(*) FROM " + NameValueDatabaseHelper.DATA_TABLE.name;
/**
* Queries the database of the pipeline to determine how many rows of data we have recorded so far.
*/
private void updateScanCount() {
    // Query the pipeline db for the count of rows in the data table
    SQLiteDatabase db = pipeline.getDb();
    Cursor mcursor = db.rawQuery(TOTAL_COUNT_SQL, null);
    mcursor.moveToFirst();
    final int count = mcursor.getInt(0);
    // Update interface on main thread
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            dataCountView.setText("Data Count: " + count);
        }
    });
}
```

And ensure that the number gets incremented when the pipeline receives a complete data batch by overriding the onDataCompleted method.
```
@Override
public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
    updateScanCount();
    // Re-register to keep listening after probe completes.
    wifiProbe.registerPassiveListener(this);
    locationProbe.registerPassiveListener(this);
}
```

Now let's add an element to display this data count. In the main.xml layout file we add our UI element.
```
<TextView android:id="@+id/dataCountText" 
    android:text="None" 
    android:layout_width="wrap_content" 
    android:layout_height="wrap_content"/>
```

## Scan Now Button ##

Users may find the need to scan at a particular moment. Let's create the UI element in main.xml.
```
<Button android:id="@+id/scanNowButton"
        android:text="Scan Now"
        android:layout_width="fill_parent"
        android:layout_height="wrap_content"/>
```

Then set the button functionality by adding the following code to onCreate in MainActivity?.java to enable the scan now button.
```
// Forces the pipeline to scan now
scanNowButton = (Button) findViewById(R.id.scanNowButton);
scanNowButton.setEnabled(false);
scanNowButton.setOnClickListener(new OnClickListener() {
    @Override
    public void onClick(View v) {
        if (pipeline.isEnabled()) {
            // Manually register the pipeline
            wifiProbe.registerListener(pipeline);
            locationProbe.registerListener(pipeline);
        } else {
            Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
        }
    }
});
```

And lastly, make sure the buttons are enabled once the FunfManager is connected
```
public void onServiceConnected(ComponentName name, IBinder service) {
    …
    scanNowButton.setEnabled(true);
}
```

## Getting Data ##

Data collected by Funf is periodically archived to the SDCard as encrypted SQLite database files. They can be found at:"/sdcard/`<app_package_name>`/default".
Data analysis

Once you have your encrypted database files, you can decrypt and merge them using the Funf scripts package. Details on how to use these scripts can be found in the [README](http://code.google.com/p/funf-open-sensing-framework/source/browse/README?repo=scripts).
A typical usage would be
```
> python dbdecrypt.py *.db
... enter password (changeme by default)
> python dbmerge.py *.db
```

This will produce a SQlite database file with data from all of the individual files. Check out ProcessingData for details about the table structure. From here you can either use the [SQLite command line program](http://www.sqlite.org/download.html), [SQLite Database Browser](http://sqlitebrowser.sourceforge.net/), or write a custom script.