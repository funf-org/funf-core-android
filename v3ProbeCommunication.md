# Probe Communication #

Probe communication occurs using standard Intents and Pending Intents.  The distributed communication model exists to allow multiple APKs to use the same probes,
in order to coordinate data gathering and cut down on cpu, memory, and power consumption.



## Requests ##
You can request information from a probe by sending an intent to that probe with one of the following actions
  * Probe.ACTION\_REQUEST
  * Probe.ACTION\_SEND\_DETAILS
  * Probe.ACTION\_SEND\_CONFIGURATION
  * Probe.ACTION\_SEND\_STATUS

All `SEND` requests can supply the `CALLBACK` extra, as a `PendingIntent`.  This pending intent will be filled with probe details, status, or data extras and sent.
The `PendingIntent` can be created using a service intent or a broadcast intent.  However, if you plan to use a generic action intent, or a broadcast intent
make sure you scope your intent using the [setPackage(&lt;package&gt;)](http://developer.android.com/reference/android/content/Intent.html#setPackage(java.lang.String)) method.
Otherwise the data from this probe may be sent to other packages on the device.

If you do not specify the `CALLBACK` parameter, these details will be sent to all `PendingIntents` that are registered using `Probe.ACTION_REQUEST`.

NOTE: If you specify an action for your `CALLBACK` intent, then Funf will not replace this action.  You will have to inspect the extras to decide what kind of message it is.


### Probe.ACTION\_REQUEST ###
To request data from a probe, send a service intent to it with the following required extras defined.
  * **CALLBACK** (`PendingIntent`)  - This pending intent will be filled with probe details, status, and data and sent periodically until you cancel the intent or send a blank data request.
  * **REQUESTS** (`Bundle[ ]` or `ArrayList<Bundle>`) - The data requests you would like the probe to fulfill.  If not specified, null, or blank, data requests are cleared for this callback.

The probe merges your data requests with all others to decide an appropriate schedule to run on.  Data and status is sent periodically until you cancel the intent or send a blank data request.


## Responses ##
All responses have the following extras specified.
  * PROBE - (`string`) The fully qualified class name of the probe.
  * TIMESTAMP - (`long` `timestamp`) The time at which the data was collected, in the form of a unix timestamp of seconds since the epoch.

Each response is sent using one of the following actions.

### Probe.DETAILS ###
Programmatic access to details about the probe.  This is useful for apps that dynamically discover probes on the system, or creating generic interfaces to probes.
  * NAME - (`string`) The full name of the probe including package prefix (probe class name)
  * DISPLAY\_NAME - (`string`) A human readable name for the probe
  * REQUIRED\_PERMISSIONS - (`String[ ]`) list of permissions needed to run this probe
  * REQUIRED\_FEATURES - (`String[ ]`) list of permissions needed to run this probe
  * PARAMETERS - (`Bundle[ ]`) An array of bundles that represent the available parameters.  Each bundle has the following defined
    * NAME - (`String`) System name for parameter
    * DISPLAY\_NAME - (`String`) Human readable name of probe
    * DEFAULT\_VALUE - (`Boolean, Short, Integer, Long, Float, Double, or String`) the value used if no value is specified
    * DESCRIPTION - (`String`) The behavior of the parameter

### Probe.STATUS ###
The current state of the Probe.  These are sent to the callbacks every time the probe changes state, and also when the probe receives a `ACTION_SEND_STATUS` action.
  * ENABLED - (`boolean`) true if the probe is currently enabled
  * RUNNING - (`boolean`) true if the probe is currently running
  * NEXT\_RUN - (`long` `timestamp`) The time of the next scheduled run for this probe.  0 if no schedule.
  * PREVIOUS\_RUN - (`long` `timestamp`) The time of the most recent run of this probe.  0 if never run.

### Probe.DATA ###
Data intents have extras that are custom to each probe.
For details on what extras are available for each probe, see [BuiltinProbes](BuiltinProbes.md).

NOTE: Probe may emit more data broadcasts than requested.


## Security ##
_The security model used in probe communication has not been extensively reviewed and critiqued.  Therefore there are no guarantees on level of security these methods enable. Consider the sensitivity of the data you are working with when using Funf._

Every probe has a list of Android permissions that a package must have in order to request data from that probe.
Before Funf will send data to a package it first verifies that the package has the necessary permissions to
receive that type of information.  All data is sent using the callback `PendingIntent`.  `PendingIntents` are
registered with the operating system, and so Funf can look up which package created it using
[PendingIntent.getTargetPackage()](http://developer.android.com/reference/android/app/PendingIntent.html#getTargetPackage()).