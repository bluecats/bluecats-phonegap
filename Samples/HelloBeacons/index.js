/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

//Update with the api token for your app from https://app.bluecats.com
var blueCatsAppToken = 'BLUECATS-APP-TOKEN';

var app = {
    // Application Constructor
    initialize: function() {
        this.bindEvents();
    },
    // Bind Event Listeners
    //
    // Bind any events that are required on startup. Common events are:
    // 'load', 'deviceready', 'offline', and 'online'.
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    // deviceready Event Handler
    //
    // The scope of 'this' is the event. In order to call the 'receivedEvent'
    // function, we must explicity call 'app.receivedEvent(...);'
    onDeviceReady: function() {
        app.receivedEvent('received');
        app.watchBeacons();
    },
    // Update DOM on a Received Event
    receivedEvent: function(event) {
        var parentElement = document.getElementById('deviceready');
        var listeningElement = parentElement.querySelector('.listening');
        var receivedElement = parentElement.querySelector('.received');

        listeningElement.setAttribute('style', 'display:none;');
        receivedElement.setAttribute('style', 'display:block;');

        if(event == 'apptokenrequired'){
            receivedElement.innerHTML = 'App token not set'
        } else if(event == 'bluecatspurring'){
            receivedElement.innerHTML = 'Looking for beacons'
        };

        console.log('Received Event: ' + event);
    },

    watchBeacons: function(){

        var watchIdForEnterBeacon,watchIdForExitBeacon,watchIdForClosestBeacon = null;
        var beaconDisplayList = null;

        if(blueCatsAppToken == 'BLUECATS-APP-TOKEN'){
            //BlueCats app token hasn't been configured
            app.receivedEvent('apptokenrequired');
            return;
        }

        var sdkOptions = {
            useLocalStorage:true
        };

        var beaconWatchOptions = {
            filter:{
                //Configure additional filters here e.g.
                //sitesName:['BlueCats HQ', 'Another Site'],
                //categoriesNamed:['Entrance'],
                //maximumAccuracy:0.5
                //etc.
            }
        };

        com.bluecats.beacons.startPurringWithAppToken(
            blueCatsAppToken,
            purringSuccess, logError, sdkOptions);

        function purringSuccess() {
            app.receivedEvent('bluecatspurring');
            watchBeaconEntryAndExit();
            watchClosestBeacon();
        }

        function watchBeaconEntryAndExit(){
            if (watchIdForEnterBeacon != null) {
                com.bluecats.beacons.clearWatch(watchIdForEnterBeacon);
            };

            if (watchIdForExitBeacon != null) {
                com.bluecats.beacons.clearWatch(watchIdForExitBeacon);
            };

            watchIdForEnterBeacon = com.bluecats.beacons.watchEnterBeacon(
                function(watchData){
                    displayBeacons('Entered', watchData);
                }, logError, beaconWatchOptions);
            watchIdForExitBeacon = com.bluecats.beacons.watchExitBeacon(
                function(watchData){
                    displayBeacons('Exited', watchData);
                }, logError, beaconWatchOptions);
        }

        function watchClosestBeacon(){
            if (watchIdForClosestBeacon != null) {
                com.bluecats.beacons.clearWatch(watchIdForClosestBeacon);
            };

            watchIdForClosestBeacon = com.bluecats.beacons.watchClosestBeaconChange(
                function(watchData){
                    displayBeacons('Closest to', watchData);
                }, logError, beaconWatchOptions);
        }

        function displayBeacons(description, watchData){
            var beacons = watchData.filteredMicroLocation.beacons;
            var beaconNames = [];

            for (var i = 0; i < beacons.length; i++) {
                beaconNames.push(beacons[i].name);
            };

            var displayText = description + ' ' + beacons.length + ' beacons: ' + beaconNames.join(',');
            console.log(displayText);

            if(!beaconDisplayList){
                var appElement = document.querySelector('.app');
                beaconDisplayList = document.createElement('ol');
                beaconDisplayList.setAttribute('id', 'beacons');
                appElement.appendChild(beaconDisplayList);
            }

            var li = document.createElement('li');
            li.appendChild(document.createTextNode(displayText));
            beaconDisplayList.appendChild(li);
        }

        function logError() {
            console.log('Error occurred watching beacons');
        }
    }
};

app.initialize();
