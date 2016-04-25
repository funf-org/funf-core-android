# Funf Open Sensing Framework
Fork of the popular [Funf open sensing framework](https://github.com/funf-org/funf-core-android) which has been inactive for some years.
Currently developed for and used in data collection projects at Google and at the Technical University of Denmark.

For details about architecture design, documentation as well as scripts for processing collected data, see the original repo's [wiki](https://github.com/funf-org/funf-core-android/wiki).

# Improvements
Forked from Funf v0.4.2. See the [changelog](https://github.com/OpenSensing/funf-core-android/blob/master/CHANGELOG.md) for the full set of improvements over v0.4.2. Some of the more important ones include:
* Geofencing
* Gzipping of files before upload
* Token based authentication
* File storage in JSON format
* Gradle integration

# How to build
Run ./gradlew assembleRelease in the project root to produce an .aar that you can use as a library in your Android app.

# Usage
Funf is meant to be used as a dependency for another Android app that can provide user interaction and data collection configuration. See original project for a [tutorial](https://github.com/funf-org/funf-core-android/wiki/WifiScannerTutorial) on this.




