## Hello Beacons

###  Quick Start for com.bluecats.beacons plugin 

This sample creates the default 'Hello World' Phonegap project and extends with Beacon functionality.

### Phonegap

    phonegap create HelloBeacons
    cd HelloBeacons
    phonegap plugin add https://github.com/bluecats/bluecats-phonegap.git
    cp plugins/com.bluecats.beacons/Samples/HelloBeacons/index.js www/js/index.js

Edit www/js/index.js and update the line:

    var blueCatsAppToken = 'BLUECATS-APP-TOKEN';

replacing `BLUECATS-APP-TOKEN` with your app token generated in the BlueCats console: https://app.bluecats.com/apps

If building for iOS 

    phonegap platform add ios
    phonegap build ios
    
If building for Android

    phonegap platform add android
    phonegap build android

### Cordova

If using Cordova replace the `phonegap` in the above comands with `cordova`
