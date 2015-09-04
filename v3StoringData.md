# Storing Data #

The storage mechanisms can be used independently from the rest of Funf.

## Database Service ##
The database service is a simple recording service, which writes to a database all data sent to it via intents.
Any data can be recorded just by sending the service an intent.

```
Intent i = new Intent(context, databaseServiceClass);
i.setAction(DatabaseService.ACTION_RECORD); // The default action if none is specified
Bundle b = new Bundle();
b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, databaseName);
b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, System.currentTimeMillis());
b.putString(NameValueDatabaseService.NAME_KEY, "name");
b.putString(NameValueDatabaseService.VALUE_KEY, "aStringValue");
i.putExtras(b);
startService(i);
```

## Archiving ##
To prevent databases from becoming to large and minimize the chance of corrupting a large amount of data, the database is periodically archived to another file.  This behavior of the archive is customizable.  Here we will discuss the behavior of the default archive in Funf.

An archive is triggered by sending the appropriate intent to the `DatabaseService`.
```
Intent i = new Intent(context, databaseServiceClass);
i.setAction(DatabaseService.ACTION_ARCHIVE); 
startService(i);
```

What happens is dependent on the type of `Archive` that the `getArchive(String dbName)` method returns in the `DatabaseService`.
The default implementation of the `Archive` service behaves as follows.

![https://docs.google.com/drawings/pub?id=1zeaBeTLK1hHl0MWe2c_YRAVcvxQLLjMzVfFuDcyNwgA&w=817&h=349&t=.png](https://docs.google.com/drawings/pub?id=1zeaBeTLK1hHl0MWe2c_YRAVcvxQLLjMzVfFuDcyNwgA&w=817&h=349&t=.png)

The file is encrypted and moved to either the SD Card or the internal memory depending if the SD Card is currently available.
The encrypted file is given a name that includes its database name as well as a timestamp of when it was archived.
When the file is removed from the Archive, a copy of the file is put into the backup folder on the SD Card.
The backup folder is occasionally cleaned up to delete older files.
Backup files are kept as long as they are not infringing on the free space of the disk.
The default archive will keep a minimum of 10MB of backup, and a maximum equal to 50% of the free space available on the drive.
For instance if there is 200MB of free space available on the SD Card, then the archive will store a backup of up to 100MB.  Deleting files based on free space ensures that room will be made for new data, as well as for other applications.

## Upload Service ##
Collecting the data from phones can be tedious if you have to access the phones SD cards directly.  To automate this process the Upload service will scan your archive for files and periodically upload them.  The built-in `HttpUploadService` will POST them to a configurable URL.
See the [ServerCommunication](ServerCommunication.md) page for details.