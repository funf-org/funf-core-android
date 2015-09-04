# Scheduling #
Funf v0.5 introduces a new paradigm for scheduling which provides the user with more control over the conditions under which any task may be executed.

## What's New ##
Until Funf v0.4, it was only possible to trigger a task (archive, upload, update or data collection) via the Android system alarm, which could be set to go off once, or regularly at a fixed interval. However, an application may require more flexibility over the scheduling of a task. Here are some examples:

  * Collect data from a probe every hour with a fixed uniform probability.
  * Upload data to the server every 12 hours only if the battery level is above a threshold.
  * Collect accelerometer data every time a phone call is received.
  * Record a video whenever the activity state of the device is "driving".

The new scheduler design supports the above use-cases and many more. The pipeline config format is extended to allow the users to specify complex schedules in a simple manner (see next section). Those who require more functionality from the scheduler can easily add new probes/filters/actions. See "Digging Deeper" for an in-depth understanding of the scheduler module.

## Config ##

The config file format has been extended to support the new features of the scheduler, while maintaining compatibility with the older versions of the config file (See [Configuration](Configuration.md)). Use the @schedule tag to specify different types of schedules:

### Basic Schedule (Alarm Trigger) ###
This case consists of setting an alarm with the Android AlarmManager. This is the only scheduling mechanism available upto Funf v0.4. The config format for this use-case is left unchanged to maintain compatibility with config files written for previous versions of Funf. The following parameters can be used:

  * **interval** - The period (in seconds) after which the task is to be repeated. If this is zero or not specified, the task will be executed only once.
  * **duration** - The time (in seconds) for which the task should be executed (must be less than interval). If the task only needs to be triggered and not stopped, make this is zero or non-existent.
  * **strict** - If the trigger time needs to be exact, make this "true". However, inexact triggers are more power-efficient, so keep this "false" if aprroximate trigger time is suitable for the application. (Default is false.)
  * **offset** - The Unix timestamp (in seconds) at which to start the task. If "offset" is in the past, the start time is set as the immediate next time in the future that occurs in the sequence starting at "offset" and having a period of "interval" seconds. If "offset" is null or zero, the task will be scheduled to start immediately. If "interval" is invalid and "offset" is valid, this schedules a one-time alarm to go off at the Unix timestamp referred by "offset" (in seconds).

Example:

```
		"data": [
			{"@type": ".AccelerometerSensorProbe",
		 	 "@schedule": {"interval": 300, "duration": 10},
		 	 "sensorDelay": "NORMAL"
			},
			{"@type": ".SimpleLocationProbe",
		 	 "@schedule": {"interval": 600, "offset": 84600},
		 	 "goodEnoughAccuracy": 80,
		 	 "maxWaitTime": 60
			}
		]
```

### Generic Probe Triggers ###
In some applications, you may want to run a task whenever a particular probe fires, instead of a simple alarm. You can achieve this by simply specifying the probe's type and parameters in the @schedule tag. For example, to record the call log whenever the battery status changes:

```
		"data": [
			{"@type": ".CallLogProbe",
		 	 "@schedule": {"@type": ".BatteryProbe"}
			}
		]
```

Note that if you are specifying a probe in the @schedule object using a "@type" tag, all other members (except the @filter tag) in that @schedule object will be considered as parameters for that probe.

### Filters ###
In many cases, you would want to run a task only if the output of a probe satisfies a certain condition, or if some other general condition is satisfied. You can achieve this by using the "@filter" tag in the @schedule object. For example, to collect accelerometer data every hour with a fixed probability:

```
		"data": [
			{"@type": ".AccelerometerSensorProbe",
  			 "sensorDelay": "NORMAL",
		 	 "@schedule": {"interval": 3600,
		 	               "@filter": {"@type": ".ProbabilisticFilter", 
		 	                           "probability": 0.5} 
		 	              }
			}
		]
```

The object specified in the @filter tag must be a filter class. Sample filters are supplied in the package "edu.mit.media.funf.filter", and new ones can be created easily (see "Digging Deeper").

If you would like to use a chain of filters to achieve a complex scheduling behavior, simply add the filters as an array with the @filter tag:

```
		"data": [
			{"@type": ".AccelerometerSensorProbe",
  			 "sensorDelay": "NORMAL",
		 	 "@schedule": {"interval": 3600, "duration": 10,
		 	               "@filter": [
                                {"@type": ".LocalTimeOfDayFilter", 
                                 "start": "06:00", "end": "18:00"},
                                {"@type": ".ProbabilisticFilter", 
                                 "probability": 0.5}
                                ]
                          }
			}
		]
```

If the @filter tag is specified with an array of filter objects (as above), they will be converted into a chain of filters, with each filter feeding into the next, in the order of their appearance in the array. In the above example, the alarm is scheduled to go off every hour, which activates the LocalTimeOfDayFilter. If the local device time is between 06:00 and 18:00, the ProbabilisticFilter is activated, which will fire with a probability of 0.5. Finally, the behavior of this pipeline is that the AccelerometerSensorProbe will be fired for 10 seconds approximately every hour between 06:00 and 18:00, with a probability of 0.5.

### Nested Schedules ###
To achieve much more complex scheduling functionality, you can use nested @schedule tags. For example, to upload data to the server every 24 hours only if the battery level is above a threshold:

```
		"upload": {
			"url": \"http://www.samplewebsite.com/uploadurl\",
			"@schedule": {"@type": ".BatteryStateProbe",
			              "@schedule": {"interval": 86400},
			              "@filter": {"@type": ".KeyValueFilter",
			                          "matches": { "state": "HIGH" } } }
		}
```

The nested (inner) schedule object acts as a trigger for the outer schedule object, which in turn is the trigger for the task in question. So in the above example, the alarm is triggered every 86400 seconds (24 hours), which triggers the BatteryStateProbe, which emits the current battery state. The KeyValueFilter checks the data returned from the BatteryStateProbe, and only if the state is "HIGH", the upload task is executed.

This simple concept of creating nested schedule annotations can be used to implement increasingly complex scheduling functionality.

### Note ###
Starting from Funf v0.5, we have added support for shortened resource names in the config file in certain cases:

  * **Builtin probes**: When specifying builtin probes via the "@type" tag, you can skip the prefix "edu.mit.media.funf.probe.builtin" and it will be added automatically.
  * **Filters**: When specifying filters via the "@type" tag within a "@filter" object, you can skip the prefix "edu.mit.media.funf.filter" and it will be added automatically.

### Consolidated Example ###

```
	{
		"name": "example",
		"version":1,
		"archive": {
            "@schedule": {"@type": ".BatteryStateProbe",
		                  "@schedule": {"interval": 86400},
		                  "@filter": {"@type": ".KeyValueFilter",
		                              "matches": { "state": "HIGH" } } }
		},
		"upload": {
			"url": \"http://www.samplewebsite.com/uploadurl\",
			"@schedule": {"interval": 86400}
		},
		"update": {
			"url": \"http://www.samplewebsite.com/funfconfig\",
			"@schedule": {"interval": 86400}
		},
		"data": [
			{"@type": ".AccelerometerSensorProbe",
  			 "sensorDelay": "NORMAL",
		 	 "@schedule": {"interval": 3600, "duration": 10,
		 	               "@filter": [
                                {"@type": ".LocalTimeOfDayFilter", 
                                 "start": "06:00", "end": "18:00"},
                                {"@type": ".ProbabilisticFilter", 
                                 "probability": 0.5}
                                ]
                          }
			},
			{"@type": ".SimpleLocationProbe",
		 	 "goodEnoughAccuracy": 80,
		 	 "maxWaitTime": 60,
		 	 "@schedule": {"@type": ".BatteryProbe"}
			}
		]
	}
```

## Digging Deeper ##
The scheduler design makes it very easy to write new components that perform specific tasks which can be plugged in easily with the rest of the system. If you implement a new probe, it can be used as a trigger by simply referring to it via the "@type" tag within a "@schedule" object, and specifying its parameters in the same "@schedule" object. Similarly, you could implement a new filter to suit your purpose and refer to it in the "@filter" object.

Here is a brief description of the building blocks of the scheduler, which should help you get started with extending it to implement your desired functionality. For complete details, please view the actual source code.

### Probes ###
Probes are exactly the same classes as used for data collection in Funf. However, starting from Funf v0.5, probes can be used for scheduling purposes as well. You can use any of the probes in "edu.mit.media.funf.probe.builtin" package as a scheduling trigger. Moreover, new probes can be developed for supporting new scheduling triggers (see [DevelopingNewProbes](DevelopingNewProbes.md)).

### Filters ###
Filter classes implement DataListener and DataSource interfaces, and their basic operation is simple: (1) listen for data, (2) when data is received check for some specific condition in the data or in the Android device, (3) decide whether to pass on the data or to discard it. See the sample filter classes in "edu.mit.media.funf.filter" package. You can implement new filter classes and use them in your config file directly.

### Actions ###
Action classes, as their name suggests, implement a specific action. Starting from Funf v0.5, all probe (start/stop a probe) and pipeline (write to a database, archive, upload, update) operations are encapsulated into different action classes. Example actions from the "edu.mit.media.funf.action" package: WriteDataAction (write probe data to a database), RunArchiveAction (archive the contents of a database), RunUploadAction (upload data to a server), StartableAction (start a Startable object for a particular duration), etc.

You can implement new actions to perform custom tasks. All action classes must subclass the base Action class (edu.mit.media.funf.action.Action). By default, the action runs on the thread of the object which calls its run() method. To make an action run on a specific thread (for eg. a pipeline's thread), call setHandler() and pass that thread's Handler.

By default, the scheduler uses the StartableAction to run the specified probe. To use your custom action, you must include a "@trigger" member within the "@schedule" tag which identifies your action class. For example, in the following config, the AccelerometerSensorProbe will be started/stopped for 10 seconds every hour by the StartableAction:

```
		"data": [
			{"@type": ".AccelerometerSensorProbe",
  			 "sensorDelay": "NORMAL",
		 	 "@schedule": {"interval": 3600, "duration": 10 }
			},
		]
```

However, if you want to "toggle" the probe between on/off every hour, you could implement a custom ToggleAction that activates/deactivates the ProbeDataSource, and modify your config as follows:

```
		"data": [
			{"@type": ".AccelerometerSensorProbe",
  			 "sensorDelay": "NORMAL",
		 	 "@schedule": {"interval": 3600, 
		 	               "@trigger": {"@type": ".ToggleAction"} }
			},
		]
```

### DataSource ###
The interface DataSource is implemented by classes that behave as data sources and accept a DataListener object (via setListener() method) to which the data will be broadcast. Examples include Filters, StartableDataSource, etc.

### Startable ###
The interface Startable exposes two functions: start() and stop(). It is implemented by classes whose activity must be started/stopped by other objects during runtime. Examples are StartableDataSource, ProbeDataSource, CompositeDataSource.

### StartableDataSource ###
The StartableDataSource class implements both the interfaces Startable and DataSource. A pipeline will consist of multiple connected StartableDataSource objects, each of which operates independently and can be controlled by three functions: start()/stop() to activate/deactivate the StartableDataSource, and setListener(), to register a DataListener to which this StartableDataSource will broadcast its data. You can subclass the StartableDataSource class to implement your desired functionality. Example subclasses (in "edu.mit.media.funf.datasource" package) are: ProbeDataSource, CompositeDataSource.

### ProbeDataSource ###
This is a simple StartableDataSource that acts as a wrapper for probe objects. It allows probes to be used as data sources in the scheduler.

### CompositeDataSource ###
This class combines the power of data sources, filters and actions to perform complex tasks. It has a DataSource "source" and a DataListener "filter". The behavior of CompositeDataSource is straightforward: activating the CompositeDataSource in turn activates the "source" object, to which the "filter" is registered as a listener. Whenever the "source" emits some data, it is passed to the "filter", which in turn passes it to the external listener of this CompositeDataSource (which was set by a call to setListener()).

The "source" can be a ProbeDataSource, in which case the "filter" receives data whenever the probe is activated. However, interesting things can be done when the "source" is itself a CompositeDataSource, in which case the "filter" receives the final output of the internal CompositeDataSource.

Similarly, the "filter" can be simply a single filter object (eg ProbabilisticFilter), which passes on the data based on some condition. However the "filter" can be a chain of filter objects, which can act as a complex filter. Additionally, the filter chain can terminate with an Action object, which can control some other CompositeDataSource. This concept can be leveraged to achieve complex scheduling/data collection tasks.