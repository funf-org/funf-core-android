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
[server synchronization](https://github.com/funf-org/funf-core-android/wiki/ServerCommunication.wiki).

## Customize Collection
Build your own
[custom probes](https://github.com/funf-org/funf-core-android/wiki/DevelopingNewProbes.wiki)
to collect the data you want. You can extend existing probes or build a new type
of probe. You can even combine data from other probes.

## Reliably Store
Let Funf ensure the
[data is reliably stored](https://github.com/funf-org/funf-core-android/wiki/StoringData.wiki),
encrypted, and transparently moved to disks with available space.

## Automatically Upload
Gather data from one or more phones automatically by having the
[data routinely uploaded](https://github.com/funf-org/funf-core-android/wiki/ServerCommunication.wiki)
to your server.

## Analyze
Easily decrypt and merge many data files into
[one convenient database](https://github.com/funf-org/funf-core-android/wiki/ProcessingData.wiki).


# How to Build
The Funf project can be used as an Android library, or can be packaged as a jar file.
The typical method of using Funf is by using the Funf jar file.  To build the funf jar
file, use the 'release' target of the Ant build script.  (Make sure you have ant 1.8.*
or later installed.)  Then, copy the jar file (bin/funf.jar) to the libs directory of your project.
The Android build scripts (Command line Ant or in Eclipse) will take care of compiling
the jar.

If you think that you will be making frequent changes to the Funf library, you may
want to integrate Funf as a library project.
To use it as an Android library, add this project as a library dependency of your
Android application project.  See the Android documentation for integrating a
library project for
[Eclipse projects](http://developer.android.com/guide/developing/projects/projects-eclipse.html#ReferencingLibraryProject) 
or via the
[command line](http://developer.android.com/guide/developing/projects/projects-cmdline.html#ReferencingLibraryProject).
