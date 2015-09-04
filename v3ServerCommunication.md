# Server Communication #

Funf will download configuration and/or upload data to any server.
If you use the Funf Collector APK or use the built-in Funf Configured service classes for updating configuration and sending data,
then the communication protocol for uploading data and downloading new configuration is described below.

## Config download ##
The CONFIG url should support the HTTP Get operation, and the content of the response should contain a json Funf Config file, or custom config file if custom implementation.
Default path is ‘/config’.

## Data upload ##
The DATA url should support the HTTP Post operation, and allow arbitrary file uploads using a multipart form data format, and the parameter name ‘uploadedfile’.  The filename should be unique to minimize naming collisions on the server.  Only supports uploading one file per post.
Default path is ‘/data’.


## Example ##
An [example python server](http://code.google.com/p/funf-open-sensing-framework/source/browse/simple_server/funfserver.py?repo=scripts) that supports this protocol is available in the [scripts repository](http://code.google.com/p/funf-open-sensing-framework/source/checkout?repo=scripts).