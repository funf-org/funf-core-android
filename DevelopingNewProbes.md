# Developing New Probes #

The Funf system was designed to be extended to fit your needs.  If one of the built-in probes does not fit your
needs, a custom probe can be developed to gather any data available to the device.
If you think your custom probe can be generalized, consider submitting it back to the project to become a built-in probe.

## The Probe Interfaces ##
Probes were designed after the built in Android sensor interface.  Probes run while they have listeners, and stop when there are no longer any subscribers.  Since there are many types of data flows, there are additional interfaces that you can choose to implement.  For instance, if your data is a continuous stream that does not stop (such as the sensors), then you can implement the ContinuousProbe interface.
### Probe ###
Base interface that all probes must implement.
#### registerListener(DataListener... listener) ####
Request data from a probe.

#### destroy() ####
Used by the system to immediately terminate a probe.

#### addStateListener(StateListener... listener) ####
Request state changes from the probe.

#### removeStateListener(StateListener... listener) ####
Stop listening for state changes for this probe.

### ContinuousProbe ###
Probes who's data has no specified end.
#### unregisterListener(DataListener... listener) ####
Stop receiving data updates.

### PassiveProbe ###
Probes who allow clients to passively listen for data when others request it, but do not actively run the probe.
#### registerPassiveListener(DataListener... listener) ####
#### unregisterPassiveListener(DataListener... listener) ####

### ContinuableProbe ###
Probes who's state can be saved and reloaded to continue the data stream at this point.
#### getCheckpoint() ####
Returns a JsonElement that represents the current state of the probe.
#### setCheckpoint(JsonElement checkpoint) ####
Allows you to set a previously retreieved checkpoint before enabling the probe to continue data collection.


## Extending Probe.Base ##
To make implementing the Probe interface convenient, there are is already an abstract parent class that will implement the necessary threading and state machine to ensure your probe responds correctly.  By extending the Probe.Base class you only need to deal with changes in the lifecycle of the probe, in the same way you do for Activities and Services in Android.  Even the methods for ContinuousProbe, PassiveProbe, and ContinuableProbe have default implementations in this class, so all you need to do is declare that your probe class implements the corresponding interface and your probe will behave appropriately.

## Probe Lifecycle ##
![https://docs.google.com/drawings/pub?id=1bWPK6pInlUztmzTsoGo0MjoZs2xybmHsoI8cs7mLGno&w=960&h=720&t=.png](https://docs.google.com/drawings/pub?id=1bWPK6pInlUztmzTsoGo0MjoZs2xybmHsoI8cs7mLGno&w=960&h=720&t=.png)
A Probe has a custom lifecycle designed to ease the process of creating a probe to collect data from the device.
Many probes are long running, and need to be run asynchronously.  The lifecycle allows a probe developer to
ignore the complexities of state machines, threading and keeping the device awake, and instead of focus on acquiring
and processing data.

The following methods can be used to hook into the various lifecycle changes.
### onEnable() ###
Setup method when probe is first enabled.  Can be used to start any passive listeners.
### onRun() ###
Start actively running the probe.  Send data when done (or when appropriate) and stop.  May be called multiple times to update params.
### onStop() ###
Stop actively running the probe.  Passive listeners will continue,
### onDisable() ###
Disable passive listeners and tear down the service.  Will be called when probe service is destroyed.


## Helper Methods ##

### sendData(JsonObject data) ###
This method takes care of the details of sending a valid DATA broadcast.  Simply create the data bundle, and call this method.
If you do not include a TIMESTAMP field in your object one will be created and added automatically with the now time.

### getGson() ###
Factory for creating probes, and parsing and serializing Json.  Use getGsonBuilder() to add your own TypeAdapters and TypeAdapterFactories.

### getContext() ###
Access to a context.

### getHandler() ###
The handler for the probe's thread.  Use this to ensure code is running the probes thread instead of the main thread.

### sensitiveData(String data) ###
Mark a piece of data as sensitve will force it to be hashed.  Alternatively you can use the other function, sensitiveData(String data, DataNormalizer<String normalizer), to ensure the data is normalized before being hashed.



## Annotations ##
### @DisplayName ###
The human readable name of the probe.
### @Description ###
A quick description of the data the probe outputs.
### @RequiredPermissions ###
Any necessary android permissions required to run this probe.
### @RequiredFeatures ###
Any necessary hardware features needed to run this probe.
### @RequiredProbes ###
Any probes who's output is used by this probe as input.  (See Composing Probes below)


## Composing Probes ##
Custom probes can minimize their resource usage by gathering their data from existing probes as much as possible.
Using existing probes allows schedules to be coordinated when gathering data.  An built-in example is the `ActivityProbe`,
which uses the `AccelerometerProbe`.  If data is requested from both the `AccelerometerProbe` and the `ActivityProbe` then
the the `AccelerometerProbe` can coordinate both schedules.