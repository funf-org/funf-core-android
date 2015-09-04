# Getting Started #

Funf is designed to be used by people with any level of technical expertise.  If you are looking to explore what Funf can do before jumping into the code, you may want to go through the tutorial on the [Funf website](http://funf.org/about.html).

Creating a Funf applications can be done in 3 easy steps.

# Download the library from the [Downloads page](https://code.google.com/p/funf-open-sensing-framework/downloads/list), and put the jar in the libs directory of your Android application.
# Add the necessary permissions and component declarations in your Android manifest.  (Remember to add the required permissions for any probe you use too!)
```
    <uses-permission android:name="android.permission.BATTERY_STATS" />
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <!-- Insert required permission for other probes -->

    <application ...>
    	<service android:name="edu.mit.media.funf.FunfManager">
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
    </application>
    ...
```
# Optionally add a pipeline for automatic background data collection using configuration.  First declare the configuration in the meta-data for the FunfManager in the AndroidManifest.xml.
```
	<service android:name="edu.mit.media.funf.FunfManager">
		<meta-data android:name="default" android:value="@string/default_pipeline"/>
	</service>
```
Then specify the configuration in your res/values/strings.xml file.
```
	<?xml version="1.0" encoding="utf-8"?>
	<resources>
	    <string name="app_name">Funf Collector</string>
	    <string name="default_pipeline">
	        {"@type":"edu.mit.media.funf.pipeline.BasicPipeline",
	         "name":"default",
	         "version":1,
	         "data":[
	         	"edu.mit.media.funf.probe.builtin.WifiProbe"
	         ]
	         }
	    </string>
	</resources>
```
Or you can bind to the FunfManager service and programatically register any pipeline.
```
	private ServiceConnection funfMgrConn = new ServiceConnection() {    
	    @Override
	    public void onServiceConnected(ComponentName name, IBinder service) {
	      FunfManager funfMgr = ((FunfManager.LocalBinder)service).getManager();
		  funfMgr.registerPipeline("default", new MyCustomPipeline());
		}
	    @Override
	    public void onServiceDisconnected(ComponentName name) {}
	};
	@Override
  	protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
    }
```

See the [Configuration](Configuration.md) document to learn how to configure pipelines. For a step-by-step tutorial of how to build Funf apps, check out the [Tutorial](WifiScannerTutorial.md).