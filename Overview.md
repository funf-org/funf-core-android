# Overview #

The Funf Library enables researchers and developers to easily and
reliably collect and use data from Android devices.

### Minimize impact on user ###
Probes built using Funf coordinate with each other to reduce power and
processor usage, and minimize the effect on the device user.

### Security ###
Care is taken to secure sensitive data.  Applications on the device
only have access to data that they already have permissions to access
directly.  Sensitive personal data, such as names, phone numbers, and texts
messages, are one way hashed.

### Remote Configuration ###
Many studies don't have direct access to the phones gathering data in the
study.  Funf will update its configuration periodically from a
configured URL.  In addition you can specifiy that data be uploaded to
your server on a regular basis.

### Extensibility ###
Developing custom probes is simple.  New probes will automatically be
integrated into the system and are configurable like built-in probes.

### Reliability ###
Reliably persisting data to disk, and uploading it to a server can be
inconsistent on mobile devices.  Funf's data management services
is robust in the face of unavailable SD cards and inconsistent network
availability.

### Javadocs ###
Complete Javadocs can be found [here](http://wiki.funf-open-sensing-framework.googlecode.com/git/docs/index.html).