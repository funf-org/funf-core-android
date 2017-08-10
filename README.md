# Funf Open Sensing Framework

The Funf Open Sensing Framework is an Android-based extensible framework,
originally developed at the MIT Media Lab, for doing phone-based mobile sensing.
Funf provides a reusable set of functionalities enabling the collection,
uploading, and configuration for a broad range of data types.

## [Get Started->](https://github.com/funf-org/funf-core-android/wiki/GettingStarted)
![Image of Funf data flow](http://funf.org/images/sneakPeek2v2.png)

## Gather Rich Data
Over [30 existing probes](https://github.com/funf-org/funf-core-android/tree/master/src/edu/mit/media/funf/probe/builtin)
to gather a wide array of in depth data on users' social behavior.

## Easily Configure
Setup data collection across multiple devices by loading a
[configuration file](https://github.com/funf-org/funf-core-android/wiki/Configuration)
on the phone, or using the automatic
[server synchronization](https://github.com/funf-org/funf-core-android/wiki/ServerCommunication).

## Customize Collection
Build your own
[custom probes](https://github.com/funf-org/funf-core-android/wiki/DevelopingNewProbes)
to collect the data you want. You can extend existing probes or build a new type
of probe. You can even combine data from other probes.

## Reliably Store
Let Funf ensure the
[data is reliably stored](https://github.com/funf-org/funf-core-android/wiki/StoringData),
encrypted, and transparently moved to disks with available space.

## Automatically Upload
Gather data from one or more phones automatically by having the
[data routinely uploaded](https://github.com/funf-org/funf-core-android/wiki/ServerCommunication)
to your server.

## Analyze
Easily decrypt and merge many data files into
[one convenient database](https://github.com/funf-org/funf-core-android/wiki/ProcessingData).

# How to Build
The Funf project can be used as an Android library module, or can be packaged as a compiled ARR/JAR
file. The typical method of using Funf is by using an ARR/JAR file. See the [Android
documentation](https://developer.android.com/studio/projects/android-library.html) for building the
ARR/JAR file.

If you think that you will be making frequent changes to the Funf library, you may want to integrate
Funf as a library module. To use it as an Android library, add this project as a library dependency
of your Android application project. See the [Android
documentation](https://developer.android.com/studio/projects/android-library.html) for integrating a
library project.
