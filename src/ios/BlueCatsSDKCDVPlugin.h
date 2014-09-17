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

- (void) startUpdatingMicroLocation: (CDVInvokedUrlCommand*)command;
- (void) stopUpdatingMicroLocation: (CDVInvokedUrlCommand*)command;

- (void) registerLocalNotificationReceivedCallback:(CDVInvokedUrlCommand*)command;
- (void) scheduleLocalNotification:(CDVInvokedUrlCommand *)command;
- (void) cancelAllLocalNotifications:(CDVInvokedUrlCommand *)command;

@end
