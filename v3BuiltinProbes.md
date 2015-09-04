Below are the list of probes that come bundled with the Funf framework.  Not all devices have access to the sensors or data required for any given probe.


### Common Parameters ###
  * **DURATION** - Minimum length of time probe will run (seconds)
  * **PERIOD** - Length of time between probe runs (seconds)
  * **START** - Date after which probe is allowed to run (unix timestamp, seconds since epoch)
  * **END** - Date before which probe is allowed to run (unix timestamp, seconds since epoch)

# Positioning #
Use surrounding wireless signals to gather information about a device's absolute location and relative location to other devices.

## Location ##
Records the most accurate location available for the device, given device limitations and respecting battery life.

  * **Name:** `edu.mit.media.funf.probe.builtin.LocationProbe`
  * **Required Permissions:** `android.permission.ACCESS_FINE_LOCATION`, `android.permission.ACCESS_COARSE_LOCATION`
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 1800s)
    * DURATION - the max time the probe will scan before sending the best value so far (default 120s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316035121,
  "LOCATION": {
    "mResults": [
      0.0,
      0.0
    ],
    "mProvider": "network",
    "mExtras": {
      "networkLocationType": "wifi",
      "networkLocationSource": "cached"
    },
    "mDistance": 0.0,
    "mTime": 1316035121482,
    "mAltitude": 0.0,
    "mLongitude": -71.08764001,
    "mLon2": 0.0,
    "mLon1": 0.0,
    "mLatitude": 42.36126327000001,
    "mLat1": 0.0,
    "mLat2": 0.0,
    "mInitialBearing": 0.0,
    "mHasSpeed": false,
    "mHasBearing": false,
    "mHasAltitude": false,
    "mHasAccuracy": true,
    "mAccuracy": 52.0,
    "mSpeed": 0.0,
    "mBearing": 0.0
  }
}
```

## Bluetooth ##
Detects Bluetooth devices within range.

  * **Name:** `edu.mit.media.funf.probe.builtin.BluetoothProbe`
  * **Required Permissions:** `android.permission.BLUETOOTH`, `android.permission.BLUETOOTH_ADMIN`
  * **Required Features:** `android.hardware.bluetooth`
  * **Parameters:**
    * PERIOD (default 300s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316011505,
  "DEVICES": [
    {
      "android.bluetooth.device.extra.DEVICE": {
        "mAddress": "00:00:00:00:00:00"
      },
      "android.bluetooth.device.extra.NAME": "my mouse",
      "android.bluetooth.device.extra.RSSI": -76,
      "android.bluetooth.device.extra.CLASS": {
        "mClass": 3670276
      }
    },
    {
      "android.bluetooth.device.extra.DEVICE": {
        "mAddress": "00:00:00:00:00:00"
      },
      "android.bluetooth.device.extra.NAME": "MacBook Pro",
      "android.bluetooth.device.extra.RSSI": -70,
      "android.bluetooth.device.extra.CLASS": {
        "mClass": 3801356
      }
    }
  ]
}


```

## Wifi ##
Records available wifi access points.

  * **Name:** `edu.mit.media.funf.probe.builtin.WifiProbe`
  * **Required Permissions:** `android.permission.ACCESS_WIFI_STATE`, `android.permission.CHANGE_WIFI_STATE`
  * **Required Features:** `android.hardware.wifi`
  * **Parameters:**
    * PERIOD (default 1200s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125938,
  "SCAN_RESULTS": [
    {
      "BSSID": "00:00:00:00:00:00",
      "SSID": "WifiRouter",
      "capabilities": "[WPA-PSK-TKIP+CCMP][WPA2-PSK-TKIP+CCMP][WPS]",
      "frequency": 2422,
      "level": -61
    },
    {
      "BSSID": "00:00:00:00:00:00",
      "SSID": "MyWifi",
      "capabilities": "[WPA-PSK-TKIP][WPA2-PSK-TKIP]",
      "frequency": 2412,
      "level": -71
    },
    {
      "BSSID": "00:00:00:00:00:00",
      "SSID": "Home",
      "capabilities": "[WPA-PSK-TKIP+CCMP][WPA2-PSK-TKIP+CCMP][WPS]",
      "frequency": 2462,
      "level": -78
    }
  ]
}
```

## Cell ##
Records ids for the current cell tower the device is connected to.

  * **Name:** `edu.mit.media.funf.probe.builtin.CellProbe`
  * **Required Permissions:** `android.permission.ACCESS_COARSE_LOCATION`
  * **Required Features:** `android.hardware.telephony`
  * **Parameters:**
    * PERIOD (default 1200s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125879,
  "psc": -1,
  "type": 1,
  "cid": 22682,
  "lac": 6012
}
```



# Social #
Record information about relationships and communication between people.

## Contact ##
Detailed information about the contacts available to the device.  Sensitive information is one way hashed to ensure user privacy.

  * **Name:** `edu.mit.media.funf.probe.builtin.ContactProbe`
  * **Required Permissions:** `android.permission.READ_CONTACTS`
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
    * FULL - If true, probe will return all contacts, otherwise return contacts that have changed since the most recent scan (default false)
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125952,
  "CONTACT_DATA": [
    {
      "last_time_contacted": 1715907994,
      "times_contacted": 7,
      "display_name": "{\"ONE_WAY_HASH\":\"7b305371debcc36f47be8bfedf9ca10875d98643\"}",
      "custom_ringtone": "",
      "in_visible_group": 1,
      "starred": 0,
      "contact_id": 1038,
      "CONTACT_DATA": [
        {
          "is_super_primary": 0,
          "data_version": 8,
          "mimetype": "vnd.android.cursor.item/photo",
          "is_primary": 0,
          "_id": 6917,
          "raw_contact_id": 1030
        },
        {
          "data_version": 0,
          "data1": "{\"ONE_WAY_HASH\":\"7b305371debcc36f47be8bfedf9ca10875d98643\"}",
          "data4": "",
          "data5": "",
          "data2": "{\"ONE_WAY_HASH\":\"10f6a317b6bcbdd49ec6cbe6174073736b156a3\"}",
          "data3": "{\"ONE_WAY_HASH\":\"4beb46d36a8d0d77bfa696083bfeab4b6ee156d4\"}",
          "data8": "",
          "data9": "",
          "data6": "",
          "data7": "",
          "data11": 3,
          "data10": 1,
          "mimetype": "vnd.android.cursor.item/name",
          "_id": 6918,
          "is_super_primary": 0,
          "is_primary": 0,
          "raw_contact_id": 1030
        },
        {
          "is_super_primary": 0,
          "data_version": 56,
          "mimetype": "vnd.android.cursor.item/note",
          "is_primary": 0,
          "_id": 6919,
          "data1": "{\"ONE_WAY_HASH\":\"10f6a317b6bcbdd49ec6cbe6174073736b156a3\"}",
          "raw_contact_id": 1030
        },
        {
          "data_version": 0,
          "mimetype": "vnd.android.cursor.item/nickname",
          "_id": 6920,
          "data1": "",
          "is_super_primary": 0,
          "data2": 0,
          "is_primary": 0,
          "raw_contact_id": 1030
        },
        {
          "data_version": 56,
          "mimetype": "vnd.com.google.cursor.item/contact_misc",
          "_id": 6921,
          "is_super_primary": 0,
          "is_primary": 0,
          "data8": 4,
          "data9": 5,
          "raw_contact_id": 1030
        },
        {
          "data_version": 1,
          "mimetype": "vnd.android.cursor.item/email_v2",
          "_id": 6922,
          "data1": "{\"ONE_WAY_HASH\":\"b453ce4c59b89a19216ba90b06b157210bf1c502\"}",
          "data4": "",
          "is_super_primary": 0,
          "data2": 3,
          "is_primary": 1,
          "raw_contact_id": 1030
        },
        {
          "is_super_primary": 0,
          "data_version": 56,
          "mimetype": "vnd.android.cursor.item/group_membership",
          "is_primary": 0,
          "_id": 6925,
          "data1": 1,
          "raw_contact_id": 1030
        },
        {
          "data_version": 1,
          "mimetype": "vnd.android.cursor.item/phone_v2",
          "_id": 9407,
          "data1": "{\"ONE_WAY_HASH\":\"aae90a83b2d60c4f52fdbc21266e89b9ecafd4e6\"}",
          "data4": -28678,
          "is_super_primary": 0,
          "data2": 1,
          "is_primary": 0,
          "raw_contact_id": 1030
        },
        {
          "data_version": 2,
          "mimetype": "vnd.android.cursor.item/website",
          "_id": 9952,
          "data1": "{\"ONE_WAY_HASH\":\"bbe90ec3f2d60c4f52fdbc2d5f6e89b9ecafd4e6\"}",
          "is_super_primary": 0,
          "data2": 3,
          "is_primary": 0,
          "raw_contact_id": 1030
        }
      ],
      "photo_id": 6917,
      "lookup": "684i3b5ad05d0b7a5fd4",
      "send_to_voicemail": 0
    },
    {
      "last_time_contacted": 0,
      "times_contacted": 0,
      "display_name": "{\"ONE_WAY_HASH\":\"c258fa2a27a6bbc03351f63c0bb3b0d237cc9ea5\"}",
      "custom_ringtone": "",
      "in_visible_group": 0,
      "starred": 0,
      "contact_id": 1670,
      "CONTACT_DATA": [
        {
          "is_super_primary": 0,
          "data_version": 1,
          "mimetype": "vnd.android.cursor.item/photo",
          "is_primary": 0,
          "_id": 10546,
          "raw_contact_id": 1646
        },
        {
          "data_version": 0,
          "data1": "{\"ONE_WAY_HASH\":\"c258fa2a27a6bbc03351f63c0bb3b0d237cc9ea5\"}",
          "data4": "",
          "data5": "",
          "data2": "{\"ONE_WAY_HASH\":\"60b543bfaaf4e1677077510d003f94bafdf0c999\"}",
          "data3": "{\"ONE_WAY_HASH\":\"388e376d033973dbb8fdc0f9ee05cc8e7ed010cd\"}",
          "data8": "",
          "data9": "",
          "data6": "",
          "data7": "",
          "data11": 3,
          "data10": 1,
          "mimetype": "vnd.android.cursor.item/name",
          "_id": 10547,
          "is_super_primary": 0,
          "is_primary": 0,
          "raw_contact_id": 1646
        },
        {
          "is_super_primary": 0,
          "data_version": 0,
          "mimetype": "vnd.android.cursor.item/note",
          "is_primary": 0,
          "_id": 10548,
          "data1": "{\"ONE_WAY_HASH\":\"\"}",
          "raw_contact_id": 1646
        },
        {
          "data_version": 0,
          "mimetype": "vnd.android.cursor.item/nickname",
          "_id": 10549,
          "data1": "",
          "is_super_primary": 0,
          "data2": 0,
          "is_primary": 0,
          "raw_contact_id": 1646
        },
        {
          "data_version": 0,
          "mimetype": "vnd.com.google.cursor.item/contact_misc",
          "_id": 10550,
          "is_super_primary": 0,
          "is_primary": 0,
          "data8": 4,
          "data9": 5,
          "raw_contact_id": 1646
        },
        {
          "data_version": 1,
          "mimetype": "vnd.android.cursor.item/email_v2",
          "_id": 10551,
          "data1": "{\"ONE_WAY_HASH\":\"7f16fca20788f7fa944bfd8547455cd7aac894d2\"}",
          "data4": "",
          "is_super_primary": 0,
          "data2": 3,
          "is_primary": 1,
          "raw_contact_id": 1646
        }
      ],
      "photo_id": 10546,
      "lookup": "684ic305f0709a5cc9f",
      "send_to_voicemail": 0
    }
  ]
}
```

## Call Log ##
Records the calls that are made by the device.  Sensitive information is normalized and hashed consistently and can be compared to contacts on this device, or with other devices.

  * **Name:** `edu.mit.media.funf.probe.builtin.CallLogProbe`
  * **Required Permissions:** `android.permission.READ_CONTACTS`
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1314737856,
  "CALLS": [
    {
      "duration": 35,
      "numbertype": "{\"ONE_WAY_HASH\":\"da4b9237bacccdf19c0760cab7aec4a8359010b0\"}",
      "_id": 1744,
      "numberlabel": "",
      "name": "{\"ONE_WAY_HASH\":\"9e2a6962a1f0b97df0897656c74e3b5cb65b8692\"}",
      "number": "{\"ONE_WAY_HASH\":\"3e432ffc629a38f8d11ee9550f76884abe66ba4b\"}",
      "date": 1314737856980,
      "type": 2
    },
    {
      "duration": 160,
      "numbertype": "{\"ONE_WAY_HASH\":\"da4b9237bacccdf19c0760cab7aec4a8359010b0\"}",
      "_id": 1743,
      "numberlabel": "",
      "name": "{\"ONE_WAY_HASH\":\"2d54779163d21f957c80990d0d562f41f63c0cf5\"}",
      "number": "{\"ONE_WAY_HASH\":\"91009d91747611eda5a382d727370fea84f52d32\"}",
      "date": 1314726289567,
      "type": 1
    },
    {
      "duration": 128,
      "numbertype": "{\"ONE_WAY_HASH\":\"da4b9237bacccdf19c0760cab7aec4a8359010b0\"}",
      "_id": 1742,
      "numberlabel": "",
      "name": "{\"ONE_WAY_HASH\":\"2d54779163d21f957c80990d0d562f41f63c0cf5\"}",
      "number": "{\"ONE_WAY_HASH\":\"91009d91747611eda5a382d727370fea84f52d32\"}",
      "date": 1314717601664,
      "type": 1
    },
    {
      "duration": 155,
      "numbertype": "{\"ONE_WAY_HASH\":\"da4b9237bacccdf19c0760cab7aec4a8359010b0\"}",
      "_id": 1741,
      "numberlabel": "",
      "name": "{\"ONE_WAY_HASH\":\"2d54779163d21f957c80990d0d562f41f63c0cf5\"}",
      "number": "{\"ONE_WAY_HASH\":\"91009d91747611eda5a382d727370fea84f52d32\"}",
      "date": 1314716019980,
      "type": 1
    },
    {
      "duration": 249,
      "numbertype": "{\"ONE_WAY_HASH\":\"b6589fc6ab0dc82cf12099d1c2d40ab994e8410c\"}",
      "_id": 1740,
      "numberlabel": "",
      "name": "",
      "number": "{\"ONE_WAY_HASH\":\"b852acac1928ed61c4e0744b9a3b67ca1baa6c0a\"}",
      "date": 1314715573010,
      "type": 2
    }
  ]
}
```

## SMS ##
Messages sent and received by this device using SMS.  Sensitive data is hashed for user privacy.

  * **Name:** `edu.mit.media.funf.probe.builtin.SMSProbe`
  * **Required Permissions:** `android.permission.READ_SMS`
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316012728,
  "MESSAGES": [
    {
      "body": "{\"ONE_WAY_HASH\":\"fdafac7beb27ed0894bd95b6097ebd7748264b06\"}",
      "person": "",
      "protocol": 0,
      "address": "{\"ONE_WAY_HASH\":\"ae8c29c470ada4a70511cead2f5ba03eac7d5129\"}",
      "status": -1,
      "subject": "",
      "read": true,
      "locked": false,
      "type": 2,
      "date": 1316012728602,
      "thread_id": 102,
      "reply_path_present": false
    },
    {
      "body": "{\"ONE_WAY_HASH\":\"6043aca4dd1122b3c9109522e0ec4f8e2710299b\"}",
      "person": "{\"ONE_WAY_HASH\":\"b93caee71a9d214d0bbbc5622ea29507e3b8a7a\"}",
      "protocol": 0,
      "address": "{\"ONE_WAY_HASH\":\"ae8c29c470ada4a70511cead2f5ba03eac7d5129\"}",
      "status": -1,
      "service_center": "+12404492157",
      "subject": "",
      "read": true,
      "locked": false,
      "type": 1,
      "date": 1316011523937,
      "thread_id": 102,
      "reply_path_present": false
    },
    {
      "body": "{\"ONE_WAY_HASH\":\"4a26672f95fedbcddbb06a09883d2a38924a5d3b\"}",
      "person": "{\"ONE_WAY_HASH\":\"8d25b4c973c5dafa021036664b080a79e0bb69a0\"}",
      "protocol": 0,
      "address": "{\"ONE_WAY_HASH\":\"3c1402eab5e74768e154d7d54cf907d19b79d7b4\"}",
      "status": -1,
      "service_center": "+12404492157",
      "subject": "",
      "read": true,
      "locked": false,
      "type": 1,
      "date": 1315924277556,
      "thread_id": 1,
      "reply_path_present": false
    }
  ]
}
```



# Motion #
Capture a devices motion to understand how a user is moving.

## Accelerometer Sensor ##
Measures the acceleration applied to the device.  All values are in SI units (m/s^2).

More information in the [Android Reference](http://developer.android.com/reference/android/hardware/SensorEvent.html)

  * **Name:** `edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe`
  * **Required Permissions:** none
  * **Required Features:** `android.hardware.sensor.accelerometer`
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1315975642,
  "SENSOR": {
    "RESOLUTION": 0.019153614,
    "POWER": 0.23,
    "NAME": "KR3DM 3-axis Accelerometer",
    "VERSION": 1,
    "TYPE": 1,
    "MAXIMUM_RANGE": 19.6133,
    "VENDOR": "STMicroelectronics"
  },
  "ACCURACY": [
    3,
    3
  ],
  "X": [
    -0.15322891,
    -0.15322891
  ],
  "Y": [
    -0.15322891,
    -0.15322891
  ],
  "Z": [
    9.844957,
    9.844957
  ],
  "EVENT_TIMESTAMP": [
    87694509262000,
    87694509262000
  ]
}
```

## Gravity Sensor ##
A three dimensional vector indicating the direction and magnitude of gravity. Units are m/s^2. The coordinate system is the same as is used by the acceleration sensor.

Note: When the device is at rest, the output of the gravity sensor should be identical to that of the accelerometer.
[Android Reference](http://developer.android.com/reference/android/hardware/SensorEvent.html)

  * **Name:** `edu.mit.media.funf.probe.builtin.GravitySensorProbe`
  * **Required Permissions:** none
  * **Required Features:** `android.hardware.sensor.accelerometer`, `android.hardware.sensor.gyroscope`
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data**:
```
{
  "TIMESTAMP": 1316125874,
  "SENSOR": {
    "RESOLUTION": 0.019153614,
    "POWER": 0.23,
    "NAME": "Gravity Sensor",
    "VERSION": 1,
    "TYPE": 9,
    "MAXIMUM_RANGE": 19.6133,
    "VENDOR": "Google Inc."
  },
  "ACCURACY": [
    3,
    3
  ],
  "X": [
    -0.31413803,
    -0.31413803
  ],
  "Y": [
    -0.19617876,
    -0.19617876
  ],
  "Z": [
    9.716458,
    9.716458
  ],
  "EVENT_TIMESTAMP": [
    151028165470000,
    151028165470000
  ]
}
```


## Linear Acceleration Sensor ##
Records a three dimensional vector indicating acceleration along each device axis, not including gravity. All values have units of m/s^2. The coordinate system is the same as is used by the acceleration sensor.

The output of the accelerometer, gravity and linear-acceleration sensors obey the following relation:
acceleration = gravity + linear-acceleration

[Android Reference](http://developer.android.com/reference/android/hardware/SensorEvent.html)

  * **Name:** `edu.mit.media.funf.probe.builtin.LinearAccelerationProbe`
  * **Required Permissions:** none
  * **Required Features:** `android.hardware.sensor.accelerometer`, `android.hardware.sensor.gyroscope`
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data**:
```
{
  "SENSOR": {
    "RESOLUTION": 0.019153614,
    "POWER": 0.23,
    "NAME": "Linear Acceleration Sensor",
    "VERSION": 1,
    "TYPE": 10,
    "MAXIMUM_RANGE": 19.6133,
    "VENDOR": "Google Inc."
  },
  "ACCURACY": [
    3,
    3,
    3
  ],
  "Y": [
    -0.011546448,
    -0.011546448,
    -0.011546448
  ],
  "TIMESTAMP": 1316125877,
  "X": [
    -0.007435143,
    -0.007435143,
    -0.007435143
  ],
  "EVENT_TIMESTAMP": [
    151031141567000,
    151031141567000,
    151031141567000
  ],
  "Z": [
    -0.029112816,
    -0.029112816,
    -0.029112816
  ]
}

```

## Gyroscope Sensor ##
Measures angular speed around each axis.

All values are in radians/second and measure the rate of rotation around the X, Y and Z axis.
The coordinate system is the same as is used for the acceleration sensor.
Rotation is positive in the counter-clockwise direction. That is, an observer looking from some positive location on the x, y. or z axis at a device positioned on the origin would report positive rotation if the device appeared to be rotating counter clockwise.
Note that this is the standard mathematical definition of positive rotation and does not agree with the definition of roll given earlier.

[Android Reference](http://developer.android.com/reference/android/hardware/SensorEvent.html)

  * **Name:** `edu.mit.media.funf.probe.builtin.GyroscopeSensorProbe`
  * **Required Permissions:** none
  * **Required Features:** `android.hardware.sensor.gyroscope`
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data**:
```
{
  "SENSOR": {
    "RESOLUTION": 0.0012217305,
    "POWER": 6.1,
    "NAME": "K3G Gyroscope sensor",
    "VERSION": 1,
    "TYPE": 4,
    "MAXIMUM_RANGE": 34.906586,
    "VENDOR": "STMicroelectronics"
  },
  "ACCURACY": [
    0,
    0,
    0
  ],
  "Y": [
    0.030543262,
    0.030543262,
    0.034208454
  ],
  "TIMESTAMP": 1316125928,
  "X": [
    0.021991149,
    0.021991149,
    0.018325957
  ],
  "EVENT_TIMESTAMP": [
    151081819673000,
    151081819673000,
    151082442978000
  ],
  "Z": [
    -0.02565634,
    -0.02565634,
    -0.02076942
  ]
}


```

## Orientation Sensor ##
Measures orientation of device.

All values are angles in degrees.
  * Azimuth, angle between the magnetic north direction and the y-axis, around the z-axis (0 to 359). 0=North, 90=East, 180=South, 270=West
  * Pitch, rotation around x-axis (-180 to 180), with positive values when the z-axis moves toward the y-axis.
  * Roll, rotation around y-axis (-90 to 90), with positive values when the x-axis moves toward the z-axis.

Note: This definition is different from yaw, pitch and roll used in aviation where the X axis is along the long side of the plane (tail to nose).

Important note: For historical reasons the roll angle is positive in the clockwise direction (mathematically speaking, it should be positive in the counter-clockwise direction).

[Android Reference](http://developer.android.com/reference/android/hardware/SensorEvent.html)

  * **Name:** `edu.mit.media.funf.probe.builtin.OrientationSensorProbe`
  * **Required Permissions:** none
  * **Required Features:** `android.hardware.sensor.gyroscope`
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data**:
```
{
  "TIMESTAMP": 1316125934,
  "SENSOR": {
    "RESOLUTION": 0.015625,
    "POWER": 7.8,
    "NAME": "AK8973 Orientation sensor",
    "VERSION": 1,
    "TYPE": 3,
    "MAXIMUM_RANGE": 360.0,
    "VENDOR": "Asahi Kasei Microdevices"
  },
  "ACCURACY": [
    2,
    2,
    2
  ],
  "AZIMUTH": [
    149.95313,
    149.95313,
    149.875
  ],
  "PITCH": [
    1.484375,
    1.484375,
    1.453125
  ],
  "ROLL": [
    -1.796875,
    -1.796875,
    -2.046875
  ],
  "EVENT_TIMESTAMP": [
    151087905209000,
    151087905209000,
    151088155342000
  ]
}

```


## Rotation Vector Sensor ##
The rotation vector represents the orientation of the device as a combination of an angle and an axis, in which the device has rotated through an angle θ around an axis <x, y, z>.

The three elements of the rotation vector are <x\*sin(θ/2), y\*sin(θ/2), z\*sin(θ/2)>, such that the magnitude of the rotation vector is equal to sin(θ/2), and the direction of the rotation vector is equal to the direction of the axis of rotation.

The three elements of the rotation vector are equal to the last three components of a unit quaternion <cos(θ/2), x\*sin(θ/2), y\*sin(θ/2), z\*sin(θ/2)>.

Elements of the rotation vector are unitless. The x,y, and z axis are defined in the same way as the acceleration sensor.

[Android Reference](http://developer.android.com/reference/android/hardware/SensorEvent.html)

  * **Name:** `edu.mit.media.funf.probe.builtin.RotationVectorSensorProbe`
  * **Required Permissions:** none
  * **Required Features:** `android.hardware.sensor.gyroscope`
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data**:
```
{
  "TIMESTAMP": 1316125897,
  "SENSOR": {
    "RESOLUTION": 5.9604645E-8,
    "POWER": 7.03,
    "NAME": "Rotation Vector Sensor",
    "VERSION": 1,
    "TYPE": 11,
    "MAXIMUM_RANGE": 1.0,
    "VENDOR": "Google Inc."
  },
  "ACCURACY": [
    -44,
    -44,
    -44
  ],
  "X_SIN_THETA_OVER_2": [
    0.011889799,
    0.011889799,
    0.011889799
  ],
  "Y_SIN_THETA_OVER_2": [
    0.015241201,
    0.015241201,
    0.015241201
  ],
  "Z_SIN_THETA_OVER_2": [
    -0.9821749,
    -0.9821749,
    -0.9821749
  ],
  "EVENT_TIMESTAMP": [
    151051185118000,
    151051185118000,
    151051185118000
  ]
}
```

## Activity ##
Records how active the person is.  Uses the AccelerometerProbe data to calculate how many intervals the variance of a device's acceleration is above a certain threshold.  Intervals are 1 seconds long.

  * **Name:** `edu.mit.media.funf.probe.builtin.ActivityProbe`
  * **Required Permissions:** none
  * **Required Features:** `android.hardware.sensor.accelerometer`
  * **Parameters:**
    * DURATION (default 5s)
    * PERIOD (default 60s)
    * START
    * END
  * **Example Data**:
```
{
  "TIMESTAMP": 1316126,
  "ACTIVE_INTERVALS": 3,
  "TOTAL_INTERVALS": 5
}
```


# Environment #
Discover details of the ambient surroundings of the phone.

## Light Sensor ##
Detects the ambient light level in SI lux units.

[Android Reference](http://developer.android.com/reference/android/hardware/SensorEvent.html)

  * **Name:** `edu.mit.media.funf.probe.builtin.LightSensorProbe`
  * **Required Permissions:** none
  * **Required Features:** `android.hardware.sensor.light`
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125846,
  "SENSOR": {
    "RESOLUTION": 1.0,
    "POWER": 0.75,
    "NAME": "GP2A Light sensor",
    "VERSION": 1,
    "TYPE": 5,
    "MAXIMUM_RANGE": 3000.0,
    "VENDOR": "Sharp"
  },
  "ACCURACY": [
    0
  ],
  "LUX": [
    10.0
  ],
  "EVENT_TIMESTAMP": [
    150999170458000
  ]
}
```

## Proximity Sensor ##
How far the front of the device is from an object.
Most implementations of this uses the light sensor and outputs a zero distance if something is against the face of the device, and a nonzero distance if nothings is against the face of the device.

[Android Reference](http://developer.android.com/reference/android/hardware/SensorEvent.html)

  * **Name:** `edu.mit.media.funf.probe.builtin.ProximitySensorProbe`
  * **Required Permissions:** none
  * **Required Features:** `android.hardware.sensor.proximity`
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125910,
  "SENSOR": {
    "RESOLUTION": 5.0,
    "POWER": 0.75,
    "NAME": "GP2A Proximity sensor",
    "VERSION": 1,
    "TYPE": 8,
    "MAXIMUM_RANGE": 5.0,
    "VENDOR": "Sharp"
  },
  "ACCURACY": [
    0,
    0,
    0
  ],
  "DISTANCE": [
    5.0,
    0.0,
    5.0
  ],
  "EVENT_TIMESTAMP": [
    151063438896354,
    151063437759000,
    151063437889000
  ]
}
```

## Magnetic Field Sensor ##
Detect the local magnetic field.  Can be used as to detect the earth's magnetic field, but may be distorted by metal objects around device.

[Android Reference](http://developer.android.com/reference/android/hardware/SensorEvent.html)

  * **Name:** `edu.mit.media.funf.probe.builtin.MagneticFieldSensorProbe`
  * **Required Permissions:** none
  * **Required Features:** `android.hardware.sensor.compass`
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125850,
  "SENSOR": {
    "RESOLUTION": 0.0625,
    "POWER": 6.8,
    "NAME": "AK8973 3-axis Magnetic field sensor",
    "VERSION": 1,
    "TYPE": 2,
    "MAXIMUM_RANGE": 2000.0,
    "VENDOR": "Asahi Kasei Microdevices"
  },
  "ACCURACY": [
    3,
    3
  ],
  "Y": [
    -17.375,
    -17.375
  ],
  "X": [
    -6.375,
    -6.375
  ],
  "EVENT_TIMESTAMP": [
    151004165361000,
    151004165361000
  ],
  "Z": [
    -32.6875,
    -32.6875
  ]
}
```

## Pressure Sensor ##
Records the pressure on the touch screen of the devices.

  * **Name:** `edu.mit.media.funf.probe.builtin.PressureSensorProbe`
  * **Required Permissions:** none
  * **Required Features:** varies
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END

## Temperature Sensor ##
Used to record temperature.  Implementation depends on the device and does not exist on all devices.
Some will record temperature of battery, others temperature of CPU or environment.

  * **Name:** `edu.mit.media.funf.probe.builtin.TemperatureSensorProbe`
  * **Required Permissions:** none
  * **Required Features:** varies
  * **Parameters:**
    * DURATION (default 60s)
    * PERIOD (default 3600s)
    * START
    * END

# Device #
## Android Info ##
Information about the version of Android the device is running.

  * **Name:** `edu.mit.media.funf.probe.builtin.AndroidInfoProbe`
  * **Required Permissions:** none
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125815,
  "BUILD_NUMBER": "soju-user 2.3.4 GRJ22 121341 release-keys",
  "FIRMWARE_VERSION": "2.3.4",
  "SDK": 10
}
```

## Battery ##
Information about the type and current state of the battery in the device.

  * **Name:** `edu.mit.media.funf.probe.builtin.BatteryProbe`
  * **Required Permissions:** `android.permission.BATTERY_STATS`
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 300s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316035006,
  "icon-small": 17302188,
  "present": true,
  "scale": 100,
  "level": 94,
  "technology": "Li-ion",
  "status": 2,
  "voltage": 4176,
  "plugged": 2,
  "health": 2,
  "temperature": 280
}

```

## Hardware Info ##
Details about the specific hardware the device is running, including component identifiers.

  * **Name:** `edu.mit.media.funf.probe.builtin.HardwareInfoProbe`
  * **Required Permissions:** `android.permission.ACCESS_WIFI_STATE`, `android.permission.BLUETOOTH`
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 300s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316035010,
  "WIFI_MAC": "00:00:00:00:00:00",
  "DEVICE_ID": "344031460718032",
  "BLUETOOTH_MAC": "00:00:00:00:00:00",
  "MODEL": "Nexus S",
  "BRAND": "google",
  "ANDROID_ID": "c3a5c9a115d94ea2"
}
```


## Time Offset ##
Checks NTP servers to determine the devices current offset from the time servers.

  * **Name:** `edu.mit.media.funf.probe.builtin.TimeOffsetProbe`
  * **Required Permissions:** `android.permission.INTERNET`
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 21600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125834,
  "TIME_OFFSET": -0.06
}
```


## Telephony ##
Records telephony hardware, software, and account information.

  * **Name:** `edu.mit.media.funf.probe.builtin.TelephonyProbe`
  * **Required Permissions:** `android.permission.READ_PHONE_STATE`
  * **Required Features:** `android.hardware.telephony`
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125829,
  "NETWORK_OPERATOR": "310410",
  "SUBSCRIBER_ID": "310410000000000",
  "SIM_OPERATOR": "310410",
  "HAS_ICC_CARD": true,
  "SIM_OPERATOR_NAME": "AT\u0026T",
  "VOICEMAIL_ALPHA_TAG": "Voicemail",
  "NETWORK_OPERATOR_NAME": "AT\u0026T",
  "DEVICE_ID": "344031460718032",
  "SIM_SERIAL_NUMBER": "00000000000000000000",
  "CALL_STATE": 0,
  "NETWORK_COUNTRY_ISO": "us",
  "VOICEMAIL_NUMBER": "5555555555",
  "NETWORK_TYPE": 2,
  "LINE_1_NUMBER": "{\"ONE_WAY_HASH\":\"d812b8cf215ae80704bd4cc5926667976ba24159\"}",
  "PHONE_TYPE": 1,
  "SIM_STATE": 5,
  "DEVICE_SOFTWARE_VERSION": "03",
  "SIM_COUNTRY_ISO": "us"
}
```



# Device Interaction #
Information about user behavior on their device.

## Running Applications ##
The current running stack of applications.

  * **Name:** `edu.mit.media.funf.probe.builtin.RunningApplicationsProbe`
  * **Required Permissions:** `android.permission.GET_TASKS`
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125889,
  "RUNNING_TASKS": [
    {
      "baseActivity": {
        "mClass": "edu.mit.media.funf.collector.RootActivity",
        "mPackage": "edu.mit.media.funf.collector"
      },
      "topActivity": {
        "mClass": "edu.mit.media.funf.collector.RootActivity",
        "mPackage": "edu.mit.media.funf.collector"
      },
      "numRunning": 1,
      "numActivities": 1,
      "id": 129
    },
    {
      "baseActivity": {
        "mClass": "com.android.launcher2.Launcher",
        "mPackage": "com.android.launcher"
      },
      "topActivity": {
        "mClass": "com.android.launcher2.Launcher",
        "mPackage": "com.android.launcher"
      },
      "numRunning": 1,
      "numActivities": 1,
      "id": 77
    },
    {
      "baseActivity": {
        "mClass": "com.android.contacts.DialtactsActivity",
        "mPackage": "com.android.contacts"
      },
      "topActivity": {
        "mClass": "com.android.contacts.DialtactsActivity",
        "mPackage": "com.android.contacts"
      },
      "numRunning": 1,
      "numActivities": 1,
      "id": 123
    }
  ]
}
```


## Applications ##
What applications are installed on the device. Also specifies applications that were uninstalled.

  * **Name:** `edu.mit.media.funf.probe.builtin.ApplicationsProbe`
  * **Required Permissions:** none
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316125944,
  "INSTALLED_APPLICATIONS": [
    {
      "dataDir": "/data/data/com.google.android.location",
      "taskAffinity": "com.google.android.location",
      "sourceDir": "/system/app/NetworkLocation.apk",
      "nativeLibraryDir": "/data/data/com.google.android.location/lib",
      "processName": "com.google.process.gapps",
      "publicSourceDir": "/system/app/NetworkLocation.apk",
      "installLocation": -1,
      "flags": 572997,
      "enabled": true,
      "targetSdkVersion": 10,
      "descriptionRes": 0,
      "theme": 0,
      "uid": 10019,
      "packageName": "com.google.android.location",
      "logo": 0,
      "labelRes": 2130837504,
      "icon": 0
    },
    {
      "dataDir": "/data/data/com.android.voicedialer",
      "taskAffinity": "com.android.voicedialer",
      "sourceDir": "/system/app/VoiceDialer.apk",
      "nativeLibraryDir": "/data/data/com.android.voicedialer/lib",
      "processName": "com.android.voicedialer",
      "publicSourceDir": "/system/app/VoiceDialer.apk",
      "installLocation": -1,
      "flags": 572997,
      "enabled": true,
      "targetSdkVersion": 10,
      "descriptionRes": 0,
      "theme": 16973835,
      "uid": 10004,
      "packageName": "com.android.voicedialer",
      "logo": 0,
      "labelRes": 2130968576,
      "icon": 2130837504
    }
  ],
  "UNINSTALLED_APPLICATIONS": [
    {
      "dataDir": "/data/data/com.weather.Weather",
      "taskAffinity": "com.weather.Weather",
      "sourceDir": "/data/app/com.weather.Weather-1.apk",
      "nativeLibraryDir": "/data/data/com.weather.Weather/lib",
      "processName": "com.weather.Weather",
      "publicSourceDir": "/data/app/com.weather.Weather-1.apk",
      "installLocation": -1,
      "flags": 48710,
      "enabled": true,
      "targetSdkVersion": 4,
      "descriptionRes": 0,
      "theme": 16973830,
      "uid": 10079,
      "packageName": "com.weather.Weather",
      "logo": 0,
      "labelRes": 2131361793,
      "icon": 2130837560
    }
  ]
}  
```


## Screen ##
Records when the screen turns off and on.

  * **Name:** `edu.mit.media.funf.probe.builtin.ScreenProbe`
  * **Required Permissions:** none
  * **Required Features:** none
  * **Parameters:**
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316129309,
  "SCREEN_ON": true
}
```


## Browser Bookmarks ##
Records the bookmarks stored in the browser.

  * **Name:** `edu.mit.media.funf.probe.builtin.BrowserBookmarksProbe`
  * **Required Permissions:** `android.permission.READ_HISTORY_BOOKMARKS`
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316046146,
  "BOOKMARKS": [
    {
      "title": "{\"ONE_WAY_HASH\":\"287bfbbdc7cfd48b82a9c569d27067a34faead53\"}",
      "bookmark": 0,
      "_id": 1595,
      "date": 1316046146049,
      "visits": 1,
      "created": 0,
      "url": "{\"ONE_WAY_HASH\":\"287bfbbdc7cfd48b82a9c569d27067a34faead53\"}"
    },
    {
      "title": "{\"ONE_WAY_HASH\":\"876275b15bef65dd0d2ca9dc08ac3b8f90245570\"}",
      "bookmark": 0,
      "_id": 1594,
      "date": 1316046124410,
      "visits": 0,
      "created": 0,
      "url": "{\"ONE_WAY_HASH\":\"876275b15bef65dd0d2ca9dc08ac3b8f90245570\"}"
    },
    {
      "title": "{\"ONE_WAY_HASH\":\"789a555c1ee75217cd7aa2c5f8afd8ced9e65826\"}",
      "bookmark": 0,
      "_id": 1593,
      "date": 1315948293765,
      "visits": 1,
      "created": 0,
      "url": "{\"ONE_WAY_HASH\":\"30448464a84dc37c036a667c0e31eb9aecc85a82\"}"
    }
  ]
}
```

## Browser Searches ##
Records the searches made in the browser.

  * **Name:** `edu.mit.media.funf.probe.builtin.BrowserSearchesProbe`
  * **Required Permissions:** `android.permission.READ_HISTORY_BOOKMARKS`
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316046124,
  "SEARCHES": [
    {
      "_id": 80,
      "date": 1316046124901,
      "search": "{\"ONE_WAY_HASH\":\"876275b15bef65dd0d2ca9dc08ac3b8f90245570\"}"
    },
    {
      "_id": 79,
      "date": 1315778981866,
      "search": "{\"ONE_WAY_HASH\":\"e5a2206ed277c1056cea9338115148645bd8555c\"}"
    },
    {
      "_id": 78,
      "date": 1315671849267,
      "search": "{\"ONE_WAY_HASH\":\"68bb75d24bdb38da03227dc3d90452019f96c5c1\"}"
    }
  ]
}
```


## Videos ##
Information about video files on the device.

  * **Name:** `edu.mit.media.funf.probe.builtin.VideosProbe`
  * **Required Permissions:** none
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1314900176,
  "VIDEOS": [
    {
      "bucket_id": "1174776152",
      "bookmark": 0,
      "date_modified": 1314900176,
      "album": "1_Sep_2011_18-01-53_GMT",
      "bucket_display_name": "1_Sep_2011_18-01-53_GMT",
      "title": "{\"ONE_WAY_HASH\":\"25f8366a6f3f222d2484dbddf227c08905785cfa\"}",
      "duration": 3584,
      "mini_thumb_magic": 1271538286,
      "_id": "47",
      "mime_type": "video/mp4",
      "date_added": 1315695843,
      "_display_name": "{\"ONE_WAY_HASH\":\"ae919b8a8758bf5ab5b39910199fa2090f0b79ca\"}",
      "isprivate": 0,
      "_size": 1171546,
      "longitude": 0.0,
      "artist": "\u003cunknown\u003e",
      "latitude": 0.0,
      "datetaken": 1314900176000
    },
    {
      "bucket_id": "1506676782",
      "bookmark": 0,
      "date_modified": 1313600186,
      "album": "Camera",
      "bucket_display_name": "Camera",
      "title": "{\"ONE_WAY_HASH\":\"3d56d956450b97c037f9a546598c2f2a330ac5ec\"}",
      "duration": 14653,
      "mini_thumb_magic": -440241642,
      "_id": "46",
      "mime_type": "video/mp4",
      "date_added": 1313600186,
      "_display_name": "{\"ONE_WAY_HASH\":\"e2d66bc63cb6c2f2d124aeec6cbf46da0ca10630\"}",
      "isprivate": 0,
      "_size": 5480596,
      "longitude": 0.0,
      "artist": "\u003cunknown\u003e",
      "latitude": 0.0,
      "datetaken": 1313600186000
    }
  ]
}
```

## Audio Files ##
Information about audio files on the device.

  * **Name:** `edu.mit.media.funf.probe.builtin.AudioFilesProbe`
  * **Required Permissions:** none
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1311937,
  "AUDIO_FILES": [
    {
      "date_modified": 1311937252,
      "album": "download",
      "is_alarm": false,
      "is_ringtone": false,
      "track": 0,
      "artist_id": 2,
      "is_music": true,
      "album_id": 2,
      "title": "{\"ONE_WAY_HASH\":\"bec2d6c68c9319a5812f6d4ca11455cb71250bfe\"}",
      "duration": 3239,
      "is_notification": false,
      "_id": "35",
      "mime_type": "audio/mpeg",
      "date_added": 1311948053,
      "_display_name": "{\"ONE_WAY_HASH\":\"b6c38acef286fbb12c8b72087715865293cc7ed2\"}",
      "_size": 6478,
      "year": 0,
      "artist": "\u003cunknown\u003e"
    },
    {
      "date_modified": 1307305844,
      "album": "download",
      "is_alarm": false,
      "is_ringtone": false,
      "track": 0,
      "artist_id": 2,
      "is_music": true,
      "album_id": 2,
      "title": "{\"ONE_WAY_HASH\":\"6ebf629279d53d1c4c34bbccaea4d0c447e9605e\"}",
      "duration": 31243,
      "is_notification": false,
      "_id": "34",
      "mime_type": "audio/mpeg",
      "date_added": 1307291445,
      "_display_name": "{\"ONE_WAY_HASH\":\"c50c439afd65685cc126b3b387d0bd70a65df5c6\"}",
      "_size": 62485,
      "year": 0,
      "artist": "\u003cunknown\u003e"
    }
  ]
}
```

## Images ##
Information about image files on the device.

  * **Name:** `edu.mit.media.funf.probe.builtin.ImagesProbe`
  * **Required Permissions:** none
  * **Required Features:** none
  * **Parameters:**
    * PERIOD (default 3600s)
    * START
    * END
  * **Example Data:**
```
{
  "TIMESTAMP": 1316000712,
  "IMAGES": [
	{
      "bucket_id": "1506676782",
      "orientation": 0,
      "date_modified": 1312060620,
      "bucket_display_name": "{\"ONE_WAY_HASH\":\"4da9c9af9631e294961d5a16fdc681ca3d84f508\"}",
      "title": "{\"ONE_WAY_HASH\":\"ecc613e31642f16783da3fbb25943c5eafefe52\"}",
      "mini_thumb_magic": 867321541,
      "_id": "142",
      "mime_type": "image/jpeg",
      "date_added": 1312071421,
      "_display_name": "{\"ONE_WAY_HASH\":\"8eec46fa3d33efc804ef6202d3bc4b62a19a6471\"}",
      "isprivate": 0,
      "description": "",
      "_size": 2272384,
      "longitude": -71.087637,
      "latitude": 42.360938,
      "datetaken": 1312071420000
    },
    {
      "bucket_id": "1506676782",
      "orientation": 180,
      "date_modified": 1312057590,
      "bucket_display_name": "{\"ONE_WAY_HASH\":\"4da9c9af9631e294961d5a16fdc681ca3d84f508\"}",
      "title": "{\"ONE_WAY_HASH\":\"aa1bc843e2b6ad420a73e415af2d795b6552eab7\"}",
      "mini_thumb_magic": -483944364,
      "_id": "141",
      "mime_type": "image/jpeg",
      "date_added": 1312068391,
      "_display_name": "{\"ONE_WAY_HASH\":\"b6a90f2ac480c4f06d59a06341939296ef153bb7\"}",
      "isprivate": 0,
      "description": "",
      "_size": 1439330,
      "longitude": -71.087637,
      "latitude": 42.360938,
      "datetaken": 1312068390000
    },
    {
      "bucket_id": "1506676782",
      "orientation": 180,
      "date_modified": 1312057584,
      "bucket_display_name": "{\"ONE_WAY_HASH\":\"4da9c9af9631e294961d5a16fdc681ca3d84f508\"}",
      "title": "{\"ONE_WAY_HASH\":\"cf70ec1c29cd460f4cfbeedf4ce2dd8cba207541\"}",
      "mini_thumb_magic": -1137136377,
      "_id": "140",
      "mime_type": "image/jpeg",
      "date_added": 1312068384,
      "_display_name": "{\"ONE_WAY_HASH\":\"32199908db52948be8017a1687b0040732751b89\"}",
      "isprivate": 0,
      "description": "",
      "_size": 1617194,
      "longitude": -71.087637,
      "latitude": 42.360938,
      "datetaken": 1312068383000
    }
  ]
}


```

