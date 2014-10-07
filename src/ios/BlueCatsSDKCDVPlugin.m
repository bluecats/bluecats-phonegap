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
#import "BCEventManager.h"

@interface BlueCatsSDKCDVPlugin()<BCMicroLocationManagerDelegate, BCEventManagerDelegate>

@property NSString* updateMicroLocationCallbackId;
@property NSString* localNotificationReceivedCallbackId;
@property NSMutableDictionary* eventCallbackIds;

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
        
        [BCEventManager sharedManager].delegate = self;
        self.eventCallbackIds = [[NSMutableDictionary alloc] init];
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

-(void)monitorClosestBeaconChange:(CDVInvokedUrlCommand*)command
{
    NSDictionary* config = [command.arguments firstObject];
    NSMutableArray* filters = [[NSMutableArray alloc] init];
    [filters addObjectsFromArray:[self filtersFromConfig:config]];
    
    [filters addObject:[BCEventFilter filterApplySmoothedAccuracyOverTimeInterval:5.0f]];
    [filters addObject:[BCEventFilter filterByMinTimeIntervalBetweenTriggers:[self minimumTriggerIntervalInSecondsFromConfig:config]]];
    [filters addObject:[BCEventFilter filterByClosestBeaconChanged]];
    
    BCTrigger* trigger = [[BCTrigger alloc] initWithIdentifier:command.callbackId andFilters:filters];
    trigger.repeatCount = [self repeatCountFromConfig:config];
    
    BCEventManager* eventManager = [BCEventManager sharedManager];
    [eventManager monitorEventWithTrigger:trigger];
}

-(void)monitorEnterBeacon:(CDVInvokedUrlCommand*)command
{
    NSDictionary* config = [command.arguments firstObject];
    NSMutableArray* filters = [[NSMutableArray alloc] init];
    [filters addObjectsFromArray:[self filtersFromConfig:config]];
    
    [filters addObject:[BCEventFilter filterByMinTimeIntervalBetweenTriggers:[self minimumTriggerIntervalInSecondsFromConfig:config]]];
    [filters addObject:[BCEventFilter filterByEnteredBeaconResetAfterTimeIntervalUnmatched:[self secondsBeforeExitBeaconFromConfig:config]]];
    
    BCTrigger* trigger = [[BCTrigger alloc] initWithIdentifier:command.callbackId andFilters:filters];
    trigger.repeatCount = [self repeatCountFromConfig:config];
    
    BCEventManager* eventManager = [BCEventManager sharedManager];
    [eventManager monitorEventWithTrigger:trigger];
}

-(void)monitorExitBeacon:(CDVInvokedUrlCommand*)command
{
    NSDictionary* config = [command.arguments firstObject];
    NSMutableArray* filters = [[NSMutableArray alloc] init];
    [filters addObjectsFromArray:[self filtersFromConfig:config]];
    
    [filters addObject:[BCEventFilter filterByMinTimeIntervalBetweenTriggers:[self minimumTriggerIntervalInSecondsFromConfig:config]]];
    [filters addObject:[BCEventFilter filterByExitedBeaconAfterTimeIntervalUnmatched:[self secondsBeforeExitBeaconFromConfig:config]]];
    
    BCTrigger* trigger = [[BCTrigger alloc] initWithIdentifier:command.callbackId andFilters:filters];
    trigger.repeatCount = [self repeatCountFromConfig:config];
    
    BCEventManager* eventManager = [BCEventManager sharedManager];
    [eventManager monitorEventWithTrigger:trigger];
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

-(void)eventManager:(BCEventManager *)eventManager triggeredEvent:(BCTriggeredEvent *)triggeredEvent
{
    NSString* callbackId = triggeredEvent.event.eventIdentifier;
    if (callbackId) {
        NSMutableDictionary* dictionary = [[NSMutableDictionary alloc] init];
        
        [dictionary setObject:[triggeredEvent.filteredMicroLocation toJSONDictionary] forKey:@"filteredMicroLocation"];
        
        [self
         sendDictionary: dictionary
         forCallback: callbackId
         keepCallback: YES];
    }
}

- (NSInteger)repeatCountFromConfig:(NSDictionary*)config
{
    NSNumber* repeatCount = [config objectForKey:@"repeatCount"];
    if (repeatCount && [repeatCount integerValue] >= 0) {
        return [repeatCount integerValue];
    }
    return NSIntegerMax;
}

- (NSTimeInterval)secondsBeforeExitBeaconFromConfig:(NSDictionary*)config
{
    NSNumber* secondsBeforeExitBeacon = [config objectForKey:@"secondsBeforeExitBeacon"];
    if (secondsBeforeExitBeacon && [secondsBeforeExitBeacon floatValue] > 0.0f) {
        return [secondsBeforeExitBeacon floatValue];
    }
    return 10.0f;
}

- (NSTimeInterval)minimumTriggerIntervalInSecondsFromConfig:(NSDictionary*)config
{
    NSNumber* minimumTriggerIntervalInSeconds = [config objectForKey:@"minimumTriggerIntervalInSeconds"];
    if (minimumTriggerIntervalInSeconds && [minimumTriggerIntervalInSeconds floatValue] >= 0.0f) {
        return [minimumTriggerIntervalInSeconds floatValue];
    }
    return 0.0f;
}

- (NSArray*)filtersFromConfig:(NSDictionary*)config
{
    NSMutableArray* filters = [[NSMutableArray alloc] init];
    NSDictionary* filterSettings = [config objectForKey:@"filter"];
    
    if ([filterSettings objectForKey:@"sitesNamed"]) {
        NSArray* siteNames = [filterSettings objectForKey:@"sitesNamed"];
        if (siteNames && siteNames.count > 0) {
            [filters addObject:[BCEventFilter filterBySitesNamed:siteNames]];
        }
    }
    
    if ([filterSettings objectForKey:@"categoriesNamed"]) {
        NSArray* categoriesNamed = [filterSettings objectForKey:@"categoriesNamed"];
        if (categoriesNamed && categoriesNamed.count > 0) {
            [filters addObject:[BCEventFilter filterByCategoriesNamed:categoriesNamed]];
        }
    }
    
    if ([filterSettings objectForKey:@"minimumProximity"]) {
        NSString* minimumProximity = [filterSettings objectForKey:@"minimumProximity"];
        if ([minimumProximity isEqualToString:@"BC_PROXIMITY_IMMEDIATE"]) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityImmediate],[NSNumber numberWithInt:BCProximityNear],[NSNumber numberWithInt:BCProximityFar],[NSNumber numberWithInt:BCProximityUnknown]]]];
        } else if ([minimumProximity isEqualToString:@"BC_PROXIMITY_NEAR"]) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityNear],[NSNumber numberWithInt:BCProximityFar],[NSNumber numberWithInt:BCProximityUnknown]]]];
        } else if ([minimumProximity isEqualToString:@"BC_PROXIMITY_FAR"]) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityFar],[NSNumber numberWithInt:BCProximityUnknown]]]];
        } else if ([minimumProximity isEqualToString:@"BC_PROXIMITY_UNKNOWN"]) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityUnknown]]]];
        }
    }
    
    if ([filterSettings objectForKey:@"maximumProximity"]) {
        NSString* maximumProximity = [filterSettings objectForKey:@"maximumProximity"];
        if ([maximumProximity isEqualToString:@"BC_PROXIMITY_IMMEDIATE"]) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityImmediate]]]];
        } else if ([maximumProximity isEqualToString:@"BC_PROXIMITY_NEAR"]) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityImmediate],[NSNumber numberWithInt:BCProximityNear]]]];
        } else if ([maximumProximity isEqualToString:@"BC_PROXIMITY_FAR"]) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityImmediate],[NSNumber numberWithInt:BCProximityNear],[NSNumber numberWithInt:BCProximityFar]]]];
        } else if ([maximumProximity isEqualToString:@"BC_PROXIMITY_UNKNOWN"]) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityImmediate],[NSNumber numberWithInt:BCProximityNear],[NSNumber numberWithInt:BCProximityFar],[NSNumber numberWithInt:BCProximityUnknown]]]];
        }
    }
    
    NSNumber* minimumAccuracy = [filterSettings objectForKey:@"minimumAccuracy"];
    NSNumber* maximumAccuracy = [filterSettings objectForKey:@"maximumAccuracy"];
    if (minimumAccuracy || maximumAccuracy) {
        if ([minimumAccuracy doubleValue] <= 0.0) {
            minimumAccuracy = nil;
        }
        if ([maximumAccuracy doubleValue] <= 0.0) {
            maximumAccuracy = [NSNumber numberWithDouble:DBL_MAX];
        }
        [filters addObject:[BCEventFilter filterByAccuracyRangeFrom:[minimumAccuracy doubleValue] to:[maximumAccuracy doubleValue]]];
    }
    
    return filters;
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
