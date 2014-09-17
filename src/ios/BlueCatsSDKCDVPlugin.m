//
//  BlueCatsSDKCDVPlugin.m
//  BlueCatsSDKCDVPlugin
//
//  Created by Damien Clarke on 15/05/2014.
//  Copyright (c) 2014 Bluecats. All rights reserved.
//

#import "BlueCatsSDKCDVPlugin.h"
#import "BlueCatsSDK.h"
#import "BCMicroLocationManager.h"
#import "BCMicroLocation.h"
#import "BCCategory.h"
#import "BCAddress.h"
#import "BCMicroLocation+JSON.h"
#import "BCLocalNotificationManager.h"
#import "BCLocalNotification.h"

@interface BlueCatsSDKCDVPlugin()<BCMicroLocationManagerDelegate>

@property NSString* updateMicroLocationCallbackId;
@property NSString* localNotificationReceivedCallbackId;

@end

@implementation BlueCatsSDKCDVPlugin

-(void)pluginInitialize
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didReceiveLocalNotification:) name:CDVLocalNotification object:nil];
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

-(void)startPurringWithAppToken:(CDVInvokedUrlCommand *)command
{
    NSString* appToken = [command.arguments objectAtIndex:0];
    [BlueCatsSDK startPurringWithAppToken:appToken completion:^(BCStatus status) {
        if (![BlueCatsSDK isLocationAuthorized]) {
            [BlueCatsSDK requestAlwaysLocationAuthorization];
        }
    }];
    [self sendOkClearCallback:command.callbackId];
}

-(void)startUpdatingMicroLocation:(CDVInvokedUrlCommand *)command
{
    // Save callbackId.
    self.updateMicroLocationCallbackId = command.callbackId;
    
    BCMicroLocationManager* microLocationManager = [BCMicroLocationManager sharedManager];
    [microLocationManager startUpdatingMicroLocation];
    microLocationManager.delegate = self;
}

-(void)stopUpdatingMicroLocation:(CDVInvokedUrlCommand *)command
{
    [[BCMicroLocationManager sharedManager] stopUpdatingMicroLocation];
    [[BCMicroLocationManager sharedManager] setDelegate:nil];
    
    if (self.updateMicroLocationCallbackId)
    {
        // Clear JS scan callback if scan is in progress.
        [self sendNoResultClearCallback: self.updateMicroLocationCallbackId];
        self.updateMicroLocationCallbackId = nil;
    }
    
    [self sendOkClearCallback: command.callbackId];
}

-(void)registerLocalNotificationReceivedCallback:(CDVInvokedUrlCommand*)command
{
    self.localNotificationReceivedCallbackId = command.callbackId;
}

-(void)scheduleLocalNotification:(CDVInvokedUrlCommand *)command
{
    BCLocalNotificationManager* notificationManager = [BCLocalNotificationManager sharedManager];
    
    BCLocalNotification* notification = [[BCLocalNotification alloc] init];
    
    NSDictionary* localNotification = command.arguments[0];
    
    NSMutableArray* bcCategories = [[NSMutableArray alloc] init];
    NSArray* categories = [localNotification objectForKey:@"fireInCategories"];
    if (categories) {
        for (NSDictionary* category in categories) {
            BCCategory* bcCategory = [[BCCategory alloc] init];
            bcCategory.name = category[@"name"];
            bcCategory.categoryID = category[@"id"];
            [bcCategories addObject:bcCategory];
        }
    }
    
    NSNumber* fireAfterDelayInSeconds = localNotification[@"fireAfterDelayInSeconds"];
    if (fireAfterDelayInSeconds) {
        notification.fireAfter = [NSDate dateWithTimeIntervalSinceNow:[fireAfterDelayInSeconds intValue]];
    }
    
    notification.fireInCategories = bcCategories;
    notification.alertAction = localNotification[@"alertAction"];
    notification.alertBody = localNotification[@"alertBody"];
    notification.userInfo = localNotification[@"userInfo"];
    
    [notificationManager scheduleLocalNotification:notification];
    
    [self sendOkClearCallback: command.callbackId];
}

-(void)cancelAllLocalNotifications:(CDVInvokedUrlCommand *)command
{
    BCLocalNotificationManager* notificationManager = [BCLocalNotificationManager sharedManager];
    [notificationManager cancelAllLocalNotifications];
    
    [self sendOkClearCallback: command.callbackId];
}

-(void)microLocationManager:(BCMicroLocationManager *)microLocationManger didUpdateMicroLocations:(NSArray *)microLocations
{
    // Send back data to JS.
    if (self.updateMicroLocationCallbackId)
    {
        if (microLocationManger.microLocation) {
            NSDictionary* data = [microLocationManger.microLocation toJSONDictionary];
            
            [self
             sendDictionary: data
             forCallback: self.updateMicroLocationCallbackId
             keepCallback: YES];
        }
    }
}

- (void)didReceiveLocalNotification:(NSNotification *)notification
{
    [UIApplication sharedApplication].applicationIconBadgeNumber = 0;
    UILocalNotification* localNotification = [notification object];
    NSMutableDictionary* data = [[NSMutableDictionary alloc] init];
    [data setObject:localNotification.userInfo forKey:@"userInfo"];
    [data setObject:localNotification.alertAction forKey:@"alertAction"];
    [data setObject:localNotification.alertBody forKey:@"alertBody"];
    if (self.localNotificationReceivedCallbackId) {
        [self sendDictionary:data forCallback:self.localNotificationReceivedCallbackId keepCallback:YES];
    }
}

/**
 * Helper method.
 */
- (void) sendOkClearCallback: (NSString*)callbackId
{
    // Clear callback on the JS side.
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_OK];
    [result setKeepCallbackAsBool: NO];
    [self.commandDelegate
     sendPluginResult: result
     callbackId: callbackId];
}

/**
 * Helper method.
 */
- (void) sendOkKeepCallback: (NSString*)callbackId
{
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_OK];
    [result setKeepCallbackAsBool: YES];
    [self.commandDelegate
     sendPluginResult: result
     callbackId: callbackId];
}

/**
 * Helper method.
 * Tell Cordova to clear the callback function associated
 * with the given callback id.
 */
- (void) sendNoResultClearCallback: (NSString*)callbackId
{
    // Clear callback on the JS side.
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_NO_RESULT];
    [result setKeepCallbackAsBool: NO];
    [self.commandDelegate
     sendPluginResult: result
     callbackId: callbackId];
}

/**
 * Helper method.
 * Tell Cordova to clear the callback function associated
 * with the given callback id.
 */
- (void) sendNoResultKeepCallback: (NSString*)callbackId
{
    // Clear callback on the JS side.
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_NO_RESULT];
    [result setKeepCallbackAsBool: YES];
    [self.commandDelegate
     sendPluginResult: result
     callbackId: callbackId];
}

/**
 * Helper method.
 * Send back an error message to Cordova.
 */
- (void) sendErrorMessage: (NSString*)errorMessage
              forCallback: (NSString*)callbackId
{
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_ERROR
                               messageAsString: errorMessage];
    [self.commandDelegate
     sendPluginResult: result
     callbackId: callbackId];
}

/**
 * Helper method.
 * Send back a dictionary object to Cordova.
 */
- (void) sendDictionary: (NSDictionary*)dictionary
            forCallback: (NSString*)callbackId
           keepCallback: (BOOL) keep
{
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_OK
                               messageAsDictionary: dictionary];
    [result setKeepCallbackAsBool: keep];
    [self.commandDelegate
     sendPluginResult: result
     callbackId: callbackId];
}

/**
 * Helper method.
 * Send back an array to Cordova.
 */
- (void) sendArray: (NSArray*)array
       forCallback: (NSString*)callbackId
      keepCallback: (BOOL) keep
{
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_OK
                               messageAsArray: array];
    [result setKeepCallbackAsBool: keep];
    [self.commandDelegate
     sendPluginResult: result
     callbackId: callbackId];
}

/**
 * Helper method.
 * Send back an array to Cordova.
 */
- (void) sendData: (NSArray*)array
       forCallback: (NSString*)callbackId
      keepCallback: (BOOL) keep
{
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_OK
                               messageAsArray: array];
    [result setKeepCallbackAsBool: keep];
    [self.commandDelegate
     sendPluginResult: result
     callbackId: callbackId];
}

@end
