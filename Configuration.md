# Configuration #
Whether you build your own Android App or use the Funf demonstration app, you can take advantage of the built in configuration code to dynamically and remotely configure you Funf application.

## Pipeline ##
Funf configuration is in the Json format and represents a logical data stream you are trying to capture. The root item for configuration is called the Pipeline.  A pipeline manages scheduling for data collection and data processing actions.  A Pipeline can be any custom class, that conforms to the Pipeline interface.  However, in practice most people will find the BasicPipeline (or custom classes that extend it) to be more than enough for most scenarios.  If you build your own pipeline, probes, or other components, you can add the @Configurable annotation to fields to allow that field to be configured.

## BasicPipeline ##

The built in pipeline is called the BasicPipeline.  Unless specified otherwise, all periods are in seconds.  All fields are optional and have defaults if they are not specified.

  * **name** - (string) Name for this configuration.  Used to identify the data stream you are capturing.
  * **version** - (integer) Arbitrary number representing the version of your application.  Can be used to determine the latest version of the app
  * **archive** - (FileArchive) Describes the archive method for database files
  * **upload** - (RemoteFileArchive) The method for uploading the database files from the archive to a remote location
  * **update** - (ConfigUpdater) The strategy for updating this configuration and updating the pipeline
  * **data** - (list) Probe configurations to use for collecting data
    * (string) - The fully qualified class name of the probe, which will use the default configuration
    * (object) - A probe configuration, which allows you to set fields with the Configurable annotation
  * **schedules** - Use to specify actions to run on a specified schedule.
    * **name** - (string) The name of the action to run
    * **value** - (object) A schedule object, which with the BasicSchedule includes the following fields
      * **interval** - (integer) the period of the schedule
      * **duration** - (integer) the amount of time to run, when available
      * **opportunistic** - (boolean) do passive listening when available (default true)
      * **strict** - (boolean) run exactly every interval instead of allowing flexible scheduling (default false)

### Scheduling ###
The archive, upload, and update objects, along with all of the data objects can optionally be given a custom schedule. By adding the "@schedule" field to their objects.  For example:
```
	"archive": {
		"@schedule": {"interval": 3600}
	},
	"data": [
		{"@type": "edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe",
		 "@schedule": {"interval": 300, "duration": 10, "strict": true}
		}
	]
```

Starting from Funf v0.5, we have redesigned the scheduler module to incorporate more complex scheduling tasks. See the [Scheduling](v5Scheduling.md) page for a detailed description.


### Example ###
```
	{
		"name": "example",
		"version":1,
		"archive": {
			"@schedule": {"interval": 500}
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
			"edu.mit.media.funf.probe.builtin.BatteryProbe",
			{"@type": "edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe",
		 	 "@schedule": {"interval": 300, "duration": 10},
		 	 "sensorDelay": "NORMAL"
			},
			{"@type": "edu.mit.media.funf.probe.builtin.SimpleLocationProbe",
		 	 "@schedule": {"interval": 600},
		 	 "goodEnoughAccuracy": 80,
		 	 "maxWaitTime": 60
			}
		]
	}
```