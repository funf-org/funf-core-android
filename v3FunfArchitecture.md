# Funf Architecture #

The Funf [source code](http://code.google.com/p/funf-open-sensing-framework/source/checkout) is divided into a few mostly independent packages.
This is a quick overview to get developers acquainted with the functionality and structure of the Funf package structure.
Many classes are public, to encourage experimentation and extending the system.

## Probe ##
Contains all of logic for probes.  The abstract class Probe is the class from which all probes extend.
However, there are other abstract classes that may be relevant to extend if you are building a new Probe.
For example, `DatedContentProviderProbe` may be more appropriate if you are building a Probe which reads a
content provider that contains a date.  A `SynchronousProbe` is useful for very simple probes that don't have
long running processes.


## Probe.builtin ##
Many common probes are already built and available in this package.  For details see the [built-in probes](BuiltinProbes.md) page.

## Storage ##
Recording data to disk and sending it to a server is a common requirement.  The `storage` package contains
convenience interfaces and classes for ensuring the persistence of data, and sending it to a central location.
Storage has file agnostic services like `ArchiverService` and `RemoteArchiverService`, as well as the database
specific `DatabaseService`.  These classes are designed to be extended with custom behavior.  See the
Configured section for an example.

Archiving is performed using two interfaces.  Programming to these interfaces allows decoration and
composition of existing archive types.

  * **Recording Data**
    * **`DatabaseService`** - create and manage synchronous access to database files
      * `NameValueDatabaseService` - uses `NameValueDatabaseHelper` and simple writes timestamp, name, value pairs to the a database table
    * `NameValueProbeDataListener` - an example of a listener that sends a record intent to the `NameValueDatabaseService`
    * `BundleSerializer` - An interface for serializing Funf data bundles into a string that can be recorded to a database

  * **Manage Files**
    * **`Archive`** - Interface for a class that stores files on disk
      * `FileDirectoryArchive` - basic directory archive
      * `BackedUpArchive` - creates a backup of any file removed from this archive
      * `CompositeFileArchive` - files will attempt to be added to the first available sub archive

  * **Uploading Files**
    * **`RemoteArchive`** - Interface for a class that sends file to a remote server
      * `HttpArchive` - represents a server url that files will be sent to via multipart form POST
    * **`UploadService`** - root class for uploading files
      * `HttpUploadService` - uses http to upload files to a url

## Configured ##
Many Funf features can be controlled using a JSON configuration file.  This package contains the `ConfiguredPipeline` service which allows you to configure data pipelines using json files.  To use this abstract class create a non-abstract class that extends `ConfiguredPipeline` and override methods as appropriate.  Some methods you may want to consider overriding are:
  * **`getBundleSerializer()`** - returns the serializer object that will convert data bundles into strings (No default)
  * **`getConfig()`** - returns the `FunfConfig` object this service will use.  This can be used to load configuration from file or from a remote site the first time it is used. (Default: blank)
  * **`getDatabaseServiceClass()`** - returns the class of the database service that is used data recorded to.  (Default: `NameValueDatabaseService`)
  * **`getUploadServiceClass()`** - returns the class of the upload service that is used. (Default: `HttpUploadService`)
  * **`getProbeDataListener()`** - returns the `BroadcastReceiver` class that will listen for Funf broadcasts and send write intents to the `DatabaseService`.  (Default: `NameValueProbeDataListener`)
