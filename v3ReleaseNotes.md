# Release Notes #
Details of the changes to the Funf Framework


## Funf ##
The main Funf library jar.

### Funf v0.3.0 ###
  * Updated communication mechanism for probes to use `PendingIntents` as callbacks, instead of `Broadcasts`.
    * Higher reliability  (Fewer missed messages due to IPC mechanism overload)
  * Lower startup time (`BroadcastsReceivers` no longer have to be dynamically registered)
    * Better security (Rely on Android to find out requesting package, instead of complicated nonce exchange)
    * Probes now conduct all actions on a separate thread, and shut down on their own if they have no data requests pending.
    * Separated probe status messages, into details (constant probe information) and status (transient state information).  More details in ProbeCommunication.

### Funf v0.2.1 ###
  * Fixed AccelerometerSensorProbe duplicate data problem
  * Switched ActivityProbe from milliseconds to seconds
  * Build system now uses git annotated tag for version instead of manual version number

### Funf v0.2.0 ###
  * Initial release of Funf