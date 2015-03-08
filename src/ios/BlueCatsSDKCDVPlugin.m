//
//  BlueCatsSDKCDVPlugin.m
//  BlueCatsSDKCDVPlugin
//
//  Created by Damien Clarke on 15/05/2014.
//  Copyright (c) 2014 Bluecats. All rights reserved.
//

#import "BlueCatsSDKCDVPlugin.h"
#import "BlueCatsSDK.h"
#import "BCMicroLocation.h"
#import "BCCategory.h"
#import "BCAddress.h"
#import "BCSite.h"
#import "BCLocalNotificationManager.h"
#import "BCLocalNotification.h"
#import "BCEventManager.h"

@interface BlueCatsSDKCDVPlugin()<BCEventManagerDelegate>

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

#pragma mark Start up

-(void)startPurringWithAppToken:(CDVInvokedUrlCommand *)command
{
    NSString* appToken = [command.arguments objectAtIndex:0];
    if (command.arguments.count > 1) {
        NSDictionary* options = [self sdkOptionsFromArgument:[command.arguments objectAtIndex:1]];
        [BlueCatsSDK setOptions:options];
    }
    
    [BlueCatsSDK startPurringWithAppToken:appToken completion:^(BCStatus status) {
        if (![BlueCatsSDK isLocationAuthorized]) {
            [BlueCatsSDK requestAlwaysLocationAuthorization];
        }
        
        [BCEventManager sharedManager].delegate = self;
        self.eventCallbackIds = [[NSMutableDictionary alloc] init];
        [self sendOkClearCallback:command.callbackId];
    }];
}

#pragma mark monitor beacons

-(void)monitorMicroLocation:(CDVInvokedUrlCommand *)command
{
    NSString* eventId = [command.arguments objectAtIndex:0];
    NSObject* optionsArg = [command.arguments objectAtIndex:1];
    NSMutableArray* filters = [self filtersFromBeaconOptionsArgument:optionsArg];
    
    [filters addObject:[BCEventFilter filterByMinTimeIntervalBetweenTriggers:[self minimumTriggerIntervalInSecondsFromBeaconOptionsArgument:optionsArg]]];
    
    BCTrigger* trigger = [[BCTrigger alloc] initWithIdentifier:eventId andFilters:filters];
    trigger.repeatCount = [self repeatCountFromBeaconOptionsArgument:optionsArg];
    
    BCEventManager* eventManager = [BCEventManager sharedManager];
    [eventManager monitorEventWithTrigger:trigger];
    
    [self.eventCallbackIds setObject:command.callbackId forKey:eventId];
}

-(void)monitorClosestBeaconChange:(CDVInvokedUrlCommand*)command
{
    NSString* eventId = [command.arguments objectAtIndex:0];
    NSObject* optionsArg = [command.arguments objectAtIndex:1];
    NSMutableArray* filters = [self filtersFromBeaconOptionsArgument:optionsArg];
    
    [filters addObject:[BCEventFilter filterApplySmoothedAccuracyOverTimeInterval:5.0f]];
    [filters addObject:[BCEventFilter filterByMinTimeIntervalBetweenTriggers:[self minimumTriggerIntervalInSecondsFromBeaconOptionsArgument:optionsArg]]];
    [filters addObject:[BCEventFilter filterByClosestBeaconChanged]];
    
    BCTrigger* trigger = [[BCTrigger alloc] initWithIdentifier:eventId andFilters:filters];
    trigger.repeatCount = [self repeatCountFromBeaconOptionsArgument:optionsArg];
    
    BCEventManager* eventManager = [BCEventManager sharedManager];
    [eventManager monitorEventWithTrigger:trigger];
    
    [self.eventCallbackIds setObject:command.callbackId forKey:eventId];
}

-(void)monitorEnterBeacon:(CDVInvokedUrlCommand*)command
{
    NSString* eventId = [command.arguments objectAtIndex:0];
    NSObject* optionsArg = [command.arguments objectAtIndex:1];
    NSMutableArray* filters = [self filtersFromBeaconOptionsArgument:optionsArg];
    
    [filters addObject:[BCEventFilter filterByMinTimeIntervalBetweenTriggers:[self minimumTriggerIntervalInSecondsFromBeaconOptionsArgument:optionsArg]]];
    [filters addObject:[BCEventFilter filterByEnteredBeaconResetAfterTimeIntervalUnmatched:[self secondsBeforeExitBeaconFromBeaconOptionsArgument:optionsArg]]];
    
    BCTrigger* trigger = [[BCTrigger alloc] initWithIdentifier:eventId andFilters:filters];
    trigger.repeatCount = [self repeatCountFromBeaconOptionsArgument:optionsArg];
    
    BCEventManager* eventManager = [BCEventManager sharedManager];
    [eventManager monitorEventWithTrigger:trigger];
    
    [self.eventCallbackIds setObject:command.callbackId forKey:eventId];
}

-(void)monitorExitBeacon:(CDVInvokedUrlCommand*)command
{
    NSString* eventId = [command.arguments objectAtIndex:0];
    NSObject* optionsArg = [command.arguments objectAtIndex:1];
    NSMutableArray* filters = [self filtersFromBeaconOptionsArgument:optionsArg];
    
    [filters addObject:[BCEventFilter filterByMinTimeIntervalBetweenTriggers:[self minimumTriggerIntervalInSecondsFromBeaconOptionsArgument:optionsArg]]];
    [filters addObject:[BCEventFilter filterByExitedBeaconAfterTimeIntervalUnmatched:[self secondsBeforeExitBeaconFromBeaconOptionsArgument:optionsArg]]];
    
    BCTrigger* trigger = [[BCTrigger alloc] initWithIdentifier:eventId andFilters:filters];
    trigger.repeatCount = [self repeatCountFromBeaconOptionsArgument:optionsArg];
    
    BCEventManager* eventManager = [BCEventManager sharedManager];
    [eventManager monitorEventWithTrigger:trigger];
    
    [self.eventCallbackIds setObject:command.callbackId forKey:eventId];
}

-(void)removeMonitoredEvent:(CDVInvokedUrlCommand*)command
{
    NSString* eventId = [command.arguments objectAtIndex:0];
    [[BCEventManager sharedManager] removeMonitoredEvent:eventId];
    [self.eventCallbackIds removeObjectForKey:eventId];
}

#pragma mark Local notifications

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

#pragma mark handle triggered beacon events

-(void)eventManager:(BCEventManager *)eventManager triggeredEvent:(BCTriggeredEvent *)triggeredEvent
{
    NSString* callbackId = [self.eventCallbackIds objectForKey:triggeredEvent.event.eventIdentifier];
    if (callbackId) {
        NSMutableDictionary* dictionary = [[NSMutableDictionary alloc] init];
        NSMutableArray* beacons = [[NSMutableArray alloc] init];
        NSMutableArray* sites = [[NSMutableArray alloc] init];
        
        for (BCBeacon* beacon in triggeredEvent.filteredMicroLocation.beacons) {
            NSMutableDictionary* beaconDictionary = [[NSMutableDictionary alloc] initWithDictionary:[beacon toDictionary]];
            [beaconDictionary setObject:[self proximityToString:beacon.proximity] forKey:@"proximity"];
            [beaconDictionary setObject:[NSNumber numberWithDouble:beacon.accuracy] forKey:@"accuracy"];
            if (beacon.siteName != nil) {
                [beaconDictionary setObject:beacon.siteName forKey:@"siteName"];
            }
            if (beacon.rssi != nil) {
                [beaconDictionary setObject:beacon.rssi forKey:@"rssi"];
            }
            [beacons addObject:beaconDictionary];
        }
        for (BCSite* site in triggeredEvent.filteredMicroLocation.sites) {
            [sites addObject:[site toDictionary]];
        }
        
        NSMutableDictionary* filteredMicroLocation = [[NSMutableDictionary alloc] init];
        
        [filteredMicroLocation setObject:beacons forKey:@"beacons"];
        [filteredMicroLocation setObject:sites forKey:@"sites"];
        
        [dictionary setObject:filteredMicroLocation forKey:@"filteredMicroLocation"];
        [dictionary setObject:[NSNumber numberWithInteger:triggeredEvent.triggeredCount] forKey:@"triggeredCount"];
        
        [self
         sendDictionary: dictionary
         forCallback: callbackId
         keepCallback: YES];
    }
}

#pragma mark Plugin argument helpers

-(NSNumber*)numberFromOptionsArgument:(NSObject*)optionsArgument withKey:(NSString*)key
{
    if (![optionsArgument isKindOfClass:[NSDictionary class]]) {
        return nil;
    }
    
    NSDictionary* options = (NSDictionary*)optionsArgument;
    
    if (options && [options objectForKey:key] && [[options objectForKey:key] isKindOfClass:[NSNumber class]]) {
        return [options objectForKey:key];
    }
    return nil;
}

- (NSInteger)repeatCountFromBeaconOptionsArgument:(NSObject*)optionsArgument
{
    NSNumber* repeatCount = [self numberFromOptionsArgument:optionsArgument withKey:@"repeatCount"];
    if (repeatCount && [repeatCount integerValue] >= 0) {
        return [repeatCount integerValue];
    }
    return NSIntegerMax;
}

- (NSTimeInterval)secondsBeforeExitBeaconFromBeaconOptionsArgument:(NSObject*)optionsArgument
{
    NSNumber* secondsBeforeExitBeacon = [self numberFromOptionsArgument:optionsArgument withKey:@"secondsBeforeExitBeacon"];
    if (secondsBeforeExitBeacon && [secondsBeforeExitBeacon floatValue] > 0.0f) {
        return [secondsBeforeExitBeacon floatValue];
    }
    return 5.0f;
}

- (NSTimeInterval)minimumTriggerIntervalInSecondsFromBeaconOptionsArgument:(NSObject*)optionsArgument
{
    NSNumber* minimumTriggerIntervalInSeconds = [self numberFromOptionsArgument:optionsArgument withKey:@"minimumTriggerIntervalInSeconds"];
    if (minimumTriggerIntervalInSeconds && [minimumTriggerIntervalInSeconds floatValue] >= 0.0f) {
        return [minimumTriggerIntervalInSeconds floatValue];
    }
    return 0.0f;
}

- (NSMutableArray*)filtersFromBeaconOptionsArgument:(NSObject*)argument
{
    NSMutableArray* filters = [[NSMutableArray alloc] init];
    
    if (![argument isKindOfClass:[NSDictionary class]]) {
        return filters;
    }
    NSDictionary* options = (NSDictionary*)argument;
    
    if (![options objectForKey:@"filter"] || ![[options objectForKey:@"filter"] isKindOfClass:[NSDictionary class]]) {
        return filters;
    }
    
    NSDictionary* filterSettings = [options objectForKey:@"filter"];
    
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
        BCProximity minimumProximity = [self proximityKeyToNativeValue:[filterSettings objectForKey:@"minimumProximity"]];
        if (minimumProximity == BCProximityImmediate) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityImmediate],[NSNumber numberWithInt:BCProximityNear],[NSNumber numberWithInt:BCProximityFar],[NSNumber numberWithInt:BCProximityUnknown]]]];
        } else if (minimumProximity == BCProximityNear) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityNear],[NSNumber numberWithInt:BCProximityFar],[NSNumber numberWithInt:BCProximityUnknown]]]];
        } else if (minimumProximity == BCProximityFar) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityFar],[NSNumber numberWithInt:BCProximityUnknown]]]];
        } else if (minimumProximity == BCProximityUnknown) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityUnknown]]]];
        }
    }
    
    if ([filterSettings objectForKey:@"maximumProximity"]) {
        BCProximity maximumProximity = [self proximityKeyToNativeValue:[filterSettings objectForKey:@"maximumProximity"]];
        
        if (maximumProximity == BCProximityImmediate) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityImmediate]]]];
        } else if (maximumProximity == BCProximityNear) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityImmediate],[NSNumber numberWithInt:BCProximityNear]]]];
        } else if (maximumProximity == BCProximityFar) {
            [filters addObject:[BCEventFilter filterByProximities:@[[NSNumber numberWithInt:BCProximityImmediate],[NSNumber numberWithInt:BCProximityNear],[NSNumber numberWithInt:BCProximityFar]]]];
        } else if (maximumProximity == BCProximityUnknown) {
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

-(NSDictionary*)sdkOptionsFromArgument:(NSObject*)argument
{
    NSMutableDictionary* optionsDictionary = [[NSMutableDictionary alloc] init];
    
    if (!argument || ![argument isKindOfClass:[NSDictionary class]]) {
        return optionsDictionary;
    }
    NSDictionary* argumentDictionary = (NSDictionary*)argument;
    
    for (NSString* key in [argumentDictionary allKeys]) {
        NSString* nativeKey = [self sdkOptionForKey:key];
        if (nativeKey && [[argumentDictionary objectForKey:key] isKindOfClass:[NSNumber class]]) {
            [optionsDictionary setObject:[argumentDictionary objectForKey:key] forKey:nativeKey];
        }
    }
    return optionsDictionary;
}

-(NSString*)sdkOptionForKey:(NSString*)sdkOption
{
    if ([sdkOption isEqualToString:@"useStageApi"]) {
        return BCOptionUseStageApi;
    } else if ([sdkOption isEqualToString:@"trackBeaconVisits"]) {
        return BCOptionTrackBeaconVisits;
    } else if ([sdkOption isEqualToString:@"monitorBlueCatsRegionOnStartup"]) {
        return BCOptionMonitorBlueCatsRegionOnStartup;
    } else if ([sdkOption isEqualToString:@"monitorAllAvailableRegionsOnStartup"]) {
        return BCOptionMonitorAllAvailableRegionsOnStartup;
    } else if ([sdkOption isEqualToString:@"useEnergySaverScanStrategy"]) {
        return BCOptionUseEnergySaverScanStrategy;
    } else if ([sdkOption isEqualToString:@"crowdSourceBeaconUpdates"]) {
        return BCOptionCrowdSourceBeaconUpdates;
    } else if ([sdkOption isEqualToString:@"useLocalStorage"]) {
        return BCOptionUseLocalStorage;
    } else if ([sdkOption isEqualToString:@"cacheAllBeaconsForApp"]) {
        return BCOptionCacheAllBeaconsForApp;
    } else if ([sdkOption isEqualToString:@"discoverBeaconsNearby"]) {
        return BCOptionDiscoverBeaconsNearby;
    } else if ([sdkOption isEqualToString:@"cacheRefreshTimeIntervalInSeconds"]) {
        return BCOptionCacheRefreshTimeIntervalInSeconds;
    }
    return nil;
}

-(BCProximity)proximityKeyToNativeValue:(NSString*)proximityKey
{
    if ([proximityKey isEqualToString:@"BC_PROXIMITY_IMMEDIATE"]) {
        return BCProximityImmediate;
    } else if ([proximityKey isEqualToString:@"BC_PROXIMITY_NEAR"]) {
        return BCProximityNear;
    } else if ([proximityKey isEqualToString:@"BC_PROXIMITY_FAR"]) {
        return BCProximityFar;
    }
    return BCProximityUnknown;
}

-(NSString*)proximityToString:(BCProximity)proximity
{
    if (proximity == BCProximityImmediate) {
        return @"BC_PROXIMITY_IMMEDIATE";
    } else if (proximity == BCProximityNear) {
        return @"BC_PROXIMITY_NEAR";
    } else if (proximity == BCProximityFar) {
        return @"BC_PROXIMITY_FAR";
    }
    return @"BC_PROXIMITY_UNKNOWN";
}

#pragma mark Plugin response helpers

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
