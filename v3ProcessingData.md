# Processing Data #

Funf stores data in numerous encrypted Sqlite database files.  Breaking data up into many small files helps prevent all data from getting lost because of a database corruption.  However, processing data is more convenient when it all resides in one unencrypted database file.

The Funf [Scripts repository](http://code.google.com/p/funf-open-sensing-framework/source/checkout?repo=scripts) includes scripts for decrypting database files and merging them.  In the future there will also be scripts for converting data into other formats, such as CSV.  See the README file in the [Scripts repository](http://code.google.com/p/funf-open-sensing-framework/source/checkout?repo=scripts) for details about each script.

The merged database has only one table named "data," and it has the following schema:
  * **id** - text, the unique id for the data entry
  * **device** - text, the UUID for the device that recorded the data
  * **probe** - text, the name of the probe that collected the data
  * **timestamp** - long, the phone time that this data was recorded (UTC in seconds since epoch)
  * **value** - text, a json format that is the data bundle that was recorded by the probe.  each probe has a unique key value structure.  However, the format should be consistent per probe.  For examples of value json see the documentation for [built-in probes](BuiltinProbes.md).