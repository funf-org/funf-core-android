# Funf Architecture #


The Funf [source code](http://code.google.com/p/funf-open-sensing-framework/source/checkout) is divided into a few mostly independent packages.
This is a quick overview to get developers acquainted with the functionality and structure of the Funf package structure.
Many classes are public, to encourage experimentation and extending the system.

## Root (edu.mit.media.funf) ##
This is the root of all funf packages.  It has only 3 classes FunfManager, Launcher, and Schedule.  Launcher is a BroadcastReceiver responsible for keeping the FunfManager alive.  Schedule is used by many packages to specify run times for different components.  The most important class and sole Android Service in Funf is the FunfManager.
### FunfManager ###
The FunfManager serves as the connection to the rest of the Android OS.  It serves as the context used by all components, and is responsible for managing pipelines, probes, and scheduling and receiving alarms with the AlarmManager.

  * **Pipelines**
    * **registerPipeline(name, pipeline)** - add pipeline from Funf
    * **unregisterPipeline(name)** - remove pipeline from Funf
    * **getRegisteredPipelin(name)** - get access to the pipeline object
    * **enablePipeline(name)** - continue runnning the pipeline
    * **disablePipeline(name)** - stop running the pipeline
    * **(un)registerPipelineAction** - used mostly by pipelines to register to be called back at certain times
  * **getGson()** - A factory to build pipelines, probes, and all other Funf components from configuration.
  * **requestData(dataListener, dataRequest[, schedule])** - Request data of the specified type on the (optional) schedule



## config ##
Contains the TypeAdapter and TypeAdapterFactory classes that allow any object to be configurable using the @Configurable annotation.  Also includes the ConfigUpdater for updating configuration remotely.

## probe.Probe ##
Contains all of logic for probes.  The abstract class Probe.Base is the class from which all builtin probes extend.
However, there are other abstract classes that may be relevant to extend if you are building a new Probe.
For example, `DatedContentProviderProbe` may be more appropriate if you are building a Probe which reads a
content provider that contains a date.  A `ImpulseProbe` is useful for very simple probes that don't have
long running processes and just return one burst of data.

## probe.builtin ##
Many common probes are already built and available in this package.  For details see the [javadocs](javadocs.md).

## storage ##
Recording data to disk and sending it to a server is a common requirement.  The `storage` package contains
convenience interfaces and classes for ensuring the persistence of data, and sending it to a central location.

Storage has file type agnostic interfaces FileArchive and RemoteFileArchive, as well as an implementation of the SQLiteOpenHelper for recording data to a database.  These classes are designed to be extended with custom behavior.

Archiving is performed using two interfaces.  Programming to these interfaces allows decoration and
composition of existing archive types.

  * **Recording Data**
    * `NameValueDatabaseHelper - SQLiteOpenHelper to create db with timestamp, name, value tuples
  * **Manage Files**
    * **`FileArchive`** - Interface for a class that stores files on disk
      * `FileDirectoryArchive` - basic directory archive, with customizable naming, copying, and cleaning.
      * `BackedUpArchive` - creates a backup of any file removed from this archive
      * `CompositeFileArchive` - files will attempt to be added to the first available sub archive
  * **Uploading Files**
    * **`RemoteFileArchive`** - Interface for a class that sends file to a remote server
      * `HttpArchive` - represents a server url that files will be sent to via multipart form POST


## pipeline ##
This package contains the Pipeline interface, the BasicPipeline, and the PipelineFactory TypeAdaptorFactory for generating pipelines from json configuration files.

## Other Utility Packages ##
  * **data** - Data normalization tools
  * **json** - Tools for immutable JsonElements, element manipulation, and the Bundle type adapter
  * **math** - Mathematical manipulation tools like Matrix math, FFT, and MFCC
  * **time** - Time utilities for normalizing all times to seconds
  * **security** - Encoding, hashing and encryption tools
  * **util** - Catch-all bucket for other utility functions

