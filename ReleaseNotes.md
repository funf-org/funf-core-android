# Release Notes #
Details of the changes to the Funf Framework


## Funf ##
The main Funf library jar.

### Funf v0.4.2 ###
  * Fixed bug where non-us locales were unable to create databases

### Funf v0.4.1 ###
> For more details on the key updates see the [Funf v0.4 Release Overview](v4ReleaseOverview.md).

  * Funf now runs as a single service instead of a service per probe.
  * Internal communication now implemented using internal function calls in the same JVM rather than inter-process communications.
  * Probes: Probe data exchange re-architected to use a GSON/JSON implementation.
  * Pipelines: Generalized pipeline interface.
  * Configuration: Extensive redesign of the configuration process
  * Configuration: Extensive redesign of the configuration process.
  * Time normalization improvements. In particular, added ability to set configuration values of less than 1 second.
  * Added dependency to modified Gson 2.1 library (see downloads page)
  * Bug fixes

### Funf v0.3.2 ###
  * Time Offset probe
  * Bug fixes

### Funf v0.3.1 ###
  * Added Accounts, Services, and Process Statistics probes
  * Added wifi only option for uploads
  * No longer sends extra data to requesters who set opportunistic to false
  * Bug fixes

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