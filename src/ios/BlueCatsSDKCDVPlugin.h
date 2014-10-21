//
//  BlueCatsSDKCDVPlugin.h
//  BlueCatsSDKCDVPlugin
//
//  Created by Damien Clarke on 15/05/2014.
//  Copyright (c) 2014 Bluecats. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>

@interface BlueCatsSDKCDVPlugin : CDVPlugin

// Public Cordova API.
- (void) startPurringWithAppToken: (CDVInvokedUrlCommand*)command;

- (void) monitorMicroLocation:(CDVInvokedUrlCommand *)command;
- (void) monitorClosestBeaconChange:(CDVInvokedUrlCommand*)command;
- (void) monitorEnterBeacon:(CDVInvokedUrlCommand*)command;
- (void) monitorExitBeacon:(CDVInvokedUrlCommand*)command;
- (void) removeMonitoredEvent:(CDVInvokedUrlCommand*)command;

- (void) registerLocalNotificationReceivedCallback:(CDVInvokedUrlCommand*)command;
- (void) scheduleLocalNotification:(CDVInvokedUrlCommand *)command;
- (void) cancelAllLocalNotifications:(CDVInvokedUrlCommand *)command;

@end
