# Probe Communication #

The simplest method for communicating with probes is using the `ProbeCommunicator` class.  This class
contains convenience methods that help with the asynchronous nature of Probe requests.  Most developers
will be fine using only this interface.  Please see [GettingStarted](GettingStarted.md) for an example.

Internally communication with probes occur through broadcast intents.
The distributed communication model exists to allow multiple APKs to use the same probes,
in order to coordinate data gathering and cut down on cpu, memory, and power consumption.
The probe protocol has four actions,
and all parameters are passed as extras on the intent.

  * **POLL** is sent by funf clients to discover probes on the device and learn details about them.
  * **STATUS** is sent by probes to report their identity, parameters, and status.
  * **REQUEST** is sent by funf clients to register to receive data from a probe.
  * **DATA** is sent by probes and includes data as extras.




## Security ##
_The security model used in probe communication has not been extensively reviewed and critiqued.  Therefore there are no guarantees on level of security these methods enable. Consider the sensitivity of the data you are working with when using Funf._

Broadcasts are, by default, sent to any package with receivers registered for that type of broadcast, an that also have the required permissions.
Probes broadcast sensitive information, some of which is secured using the Android permission system.
Rather than define a custom permission system for the probes, we instead chose to use the existing
Android permissions.  A client requesting data from a probe must have the same permissions they would have
needed to access the data directly.

To prevent arbitrary packages from listening for broadcasts that contain data they don't have access to,
data is sent using Android's built in scoped broadcasts.  Scoped broadcasts are broadcasts
that are only delivered to a specific package on the device.  Sending a scoped broadcasts is achieved by calling
[setPackage()](http://developer.android.com/reference/android/content/Intent.html#setPackage(java.lang.String))
on an intent before calling
[sendBroadcast()](http://developer.android.com/reference/android/content/Context.html#sendBroadcast(android.content.Intent))

Every probe has a list of Android permissions that a package must have in order to request data from that probe.
Before Funf will send data to a package it first verifies that the package has the necessary permissions to
receive that type of information.  Since Android does not have a mechanism for discovering who sent a broadcast,
Funf relies on the client to send that information along with the broadcast.  The PACKAGE extra is reserved for
this purpose.

To guarantee that client is only requesting data for their own package, and not for some other package,
a client package must complete a nonce handshake.

### REQUEST Nonce Handshake ###
![https://docs.google.com/drawings/pub?id=1z9ha-d5BLpmSv_uyNI00hTVQrUAVf7sQ8y-MsFZtVaU&w=762&h=436&t=.png](https://docs.google.com/drawings/pub?id=1z9ha-d5BLpmSv_uyNI00hTVQrUAVf7sQ8y-MsFZtVaU&w=762&h=436&t=.png)

#### Client (data consumer) ####
  1. Request nonce using broadcast and specifying own package
  1. Receive broadcast with nonce in it and package to send request to
  1. Send data request broadcast to specified package with nonce to verify identity
  1. Your package will start receiving data broadcasts for that type

#### Probe ####
  1. Receive data request broadcast with no nonce, or nonce request with requester specified
  1. Verify the requesting package has permissions to use requested probe
  1. If client has required permissions register nonce with 1 second timeout and send a nonce response with package of probe controller
  1. Receive data request broadcast with valid nonce, forward data request to probe
  1. Probe will send data broadcasts to requester package


## Probe Protocol Specification ##

  * **`<funf_package>`** is edu.mit.media.hd.funf
  * **`<probe_name>`** is the full java class name of the probe (e.g. `edu.mit.media.hd.funf.probe.builtin.WifiProbe`)

### POLL (Request Status) ###
Request a status broadcast from all probes on the system or a specific probe.

Action is “`<funf_package>.POLL`" or "`<probe_name>.POLL`”
  * PACKAGE - (string) required.  the full package name of the Android package sending the poll
  * NONCE - (boolean) optional.  if true the status message will include a nonce that can be used to send a REQUEST broadcast

### STATUS ###
A probe's current state and details about its parameters and required features and permissions.

Action is “`<funf_package>.STATUS`”
  * NAME - (`string`) The full name of the probe including package prefix (probe class name)
  * DISPLAY\_NAME - (`string`) A human readable name for the probe
  * ENABLED - (`boolean`) true if the probe is currently enabled
  * RUNNING - (`boolean`) true if the probe is currently running
  * NEXT\_RUN - (`timestamp`) The time of the next scheduled run for this probe.  0 if no schedule.
  * PREVIOUS\_RUN - (`timestamp`) The time of the most recent run of this probe.  0 if never run.
  * REQUIRED\_PERMISSIONS - (`String[ ]`) list of permissions needed to run this probe
  * REQUIRED\_FEATURES - (`String[ ]`) list of permissions needed to run this probe
  * PARAMETERS - (`Bundle[ ]`) An array of bundles that represent the available parameters.  Each bundle has the following defined
    * NAME - (`String`) System name for parameter
    * DISPLAY\_NAME - (`String`) Human readable name of probe
    * DEFAULT\_VALUE - (`Boolean, Short, Integer, Long, Float, Double, or String`) the value used if no value is specified
    * DESCRIPTION - (`String`) The behavior of the parameter
  * NONCE - (`long`) if NONCE=true was specified in the POLL, then this will be a long value that the requester can use to make a get request.  Will only be included if requester has necessary android permissions to run probe.  The nonce value must be used immediately, as it is only valid for ten seconds.


### REQUEST (Request Data) ###
Receiver that allows others to request a data broadcast from the probe.  Probe should keep track of each requester making requests and possibly be smart about combining more than one request.

Action is “`<probe_name>.REQUEST`”
  * PACKAGE - (`string`) required.  the Android package name of the sender of this request.
  * REQUEST\_ID - (`string`) optional.  a unique id for your package to represent this set of requests
  * REQUESTS - (`Bundle[ ]`) required. an array of parameter bundles that specifies the data desired by the requesting client package and therefore affects how the probe is configured.  If blank, data requests are cleared.
  * NONCE - (`long`)  required.  number acquired from funf in a nonce request, used to verify the requester is who they say they are.


### DATA ###
Broadcast results of probe.  May emit more data broadcasts than requested.

Action is “`<probe_name>.DATA`”
  * TIMESTAMP - The time at which the data was collected and broadcast
  * Other Values - The custom key-value data pairs the probe emits