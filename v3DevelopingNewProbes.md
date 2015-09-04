# Developing New Probes #

The Funf system was designed to be extended to fit your needs.  If one of the built-in probes does not fit your
needs, a custom probe can be developed to gather any data available to the device.
If you think your custom probe can be generalized, consider submitting it back to the project to become a built-in probe.

## Probe Lifecycle ##
![https://docs.google.com/drawings/pub?id=1bWPK6pInlUztmzTsoGo0MjoZs2xybmHsoI8cs7mLGno&w=960&h=720&t=.png](https://docs.google.com/drawings/pub?id=1bWPK6pInlUztmzTsoGo0MjoZs2xybmHsoI8cs7mLGno&w=960&h=720&t=.png)
A Probe is an Android service that has been programmed with a custom lifecycle.
This lifecycle is designed to ease the process of creating a probe to collect data from the device.
Many probes are long running

The following methods can be used to hook into the various lifecycle changes.
### onEnable() ###
Setup method when probe is first enabled.  Can be used to start any passive listeners.
### onRun(Bundle params) ###
Start actively running the probe.  Send data broadcast when done (or when appropriate) and stop.  May be called multiple times to update params.
### onStop() ###
Stop actively running the probe.  Passive listeners will continue,
### onDisable() ###
Disable passive listeners and tear down the service.  Will be called when probe service is destroyed.
### sendProbeData() ###
Used by Funf to force the probe to send a data broadcast for whatever state it currently has.  This is most likely the last value it observed.  This may not be needed and could be accomplished using sticky broadcasts.  If we are supporting on demand “half-baked” data, it will certainly be required.
### sendProbeStatus() ###
Convenience method to send correct status broadcast.  Called by the controller when a poll is sent.  This method can be overloaded to customize the status broadcast.

In this model onEnable and onRun will be called the first time onStartCommand is called, not when the service is bound to and onCreate is called.  That way code can feel free to bind to a service without fear of side effects.  We also no long will rely on service destroy to send out a data broadcast, since it will be the probe implementer's responsibility to send that out at the right time and put itself in the stopped state.



## Helper Methods ##
### sendProbeData(long timestamp, Bundle data) ###
This method takes care of the details of sending a valid DATA broadcast.  Simply create the data bundle, and call this method.

### isRunning() ###
Returns true if the probe is currently running, false otherwise.




## Composing Probes ##
Custom probes can minimize their resource usage by gathering their data from existing probes as much as possible.
Using existing probes allows schedules to be coordinated when gathering data.  An built-in example is the `ActivityProbe`,
which uses the `AccelerometerProbe`.  If data is requested from both the `AccelerometerProbe` and the `ActivityProbe` then
the the `AccelerometerProbe` can coordinate both schedules.