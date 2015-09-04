# Funf 0.4 Release Overview #
We're happy to announce the official release of Funf 0.4.

This is a major release, and as such it breaks some compatibility with Funf 0.3 based apps.

There were a few aspects of Funf 0.3 that we wanted to reconsider based on how the framework has been used and issues that were surfaced by our team as well as users. The high level goal for Funf 0.4 was to minimize effort for the developer, both for developers using the existing probes and developers working on their own probes.

## Funf 0.4 Key Updates ##
Lets take a closer look at the key updates that Funf 0.4 brings:

**Funf now runs as a single service instead of a service per probe.**

In Funf 0.3, every probe was implemented as a standalone service that had to be declared in the manifest. The thought was to attach probes to the Android services lifecycle. One advantage was exposing to a user what probes are running at any given moment via the "running apps" screen. However, there were other issues. Aside from reduced efficiency, it was pretty annoying for developers working on new probes. If a probe had a bug or some other problem, or even if the developer just forgot to add it to the manifest, the service could silently crash, or not start at all, while the rest of the app would go on running. We decided it was not worth sticking with the service lifecycle, and in 0.4 this was rewritten so that all of Funf runs in a single service.

**Internal communication is now implemented using internal function calls in the same JVM rather than inter-process communications.**

In our initial assumptions, we envisioned a common use case of very distributed Funf implementations. For example, multiple APKs on the same phone, each having different sets of probes and functionalities, communicating with one another. We wanted to enable a client app APK register to a probe APK, or even multiple client apps all registering to a single probe APK. We also defined the "Open Probe Protocol", or OPP, in coordination with other institutions that have been developing android sensing tools. OPP would allow different frameworks supporting it to communicate with one another and act as a client or data provider to one another. That all is very nice in theory, but it also has a price. Since we thought this would be a common case, we optimized all of Funf towards inter-app messaging. This means that probes and components residing in the same APK also communicated with one another via broadcasts and intents. The even worse side of that implementation, is that once someone set a high-bandwidth sensing configuration (e.g. raw accelerometer), data started to drop, and rather than reporting errors Android only raised warning messages.

In the end, we realized that almost no one was doing multi-apk implementations, and the internal APK communication were using the less efficient inter-process communication methods. So we decided to scrap it in Funf 0.4.

In 0.4, since we assume the main use case is compiling everything in a single APK, everything is done via internal function calls in the same JVM. OPP can easily be re-implemented on top of this infrastructure, however it is not part of the Funf 0.4 release.

**Probes: Probe data exchange re-architected to use a GSON/JSON implementation.**

By using a JSON library, there is no need to hard-code everything into bundles, which resulted in having no generic way to serialize things in the system. In Funf 0.4, we directly serialize Android's built in output structure. A big benefit is that when Android adds new fields to API and sensor outputs, we don't have to manually add code to retrieve them in Funf.  There is an important implication to this change: We no longer have guarantee of consistent data structures across Android versions for the built-in probes. Developers making their own probes could handle the different cases as desired.

Funf also now includes immutable instances of Gson’s JsonArray and JsonObject objects. Since probes use a publish-subscribe model, using immutable instances adds to multi-client safety and information integrity while also being efficient on processing and memory, since we only have one copy of the data, not one per data listener.

**Pipelines: Generalized pipeline interface.**

In 0.3, the framework only supported a single pipeline. This was not very extensible when wanting to add new Funf features. Funf 0.4 exposes a general interface for setting up as many pipelines as needed, with a lot of flexibility. You can swap different components, and do things like specify custom archivers, uploaders, backend updaters, and so on.

**Configuration: Extensive redesign of the configuration process.**

Funf 0.4 configuration now uses GSON/JSON to configure almost every component in the Funf system.  This includes a series of GSON extensions that enable functionality like runtime class configuration, configurable in-code annotations (e.g. expose some parts of a class and not others), allow for default typing, or injecting context as needed.

**Time normalization improvements.**

In Funf 0.3, some time was in milliseconds since the epoch, some time was in nanoseconds since the last boot, some were in seconds since the epoch in the configured time zone.  In addition one could not set configurations with values of less than 1 second. Funf 0.4 makes use of universal time. Everything works in decimal seconds, and all dates are in Unix timestamp seconds since the epoch in UTC.