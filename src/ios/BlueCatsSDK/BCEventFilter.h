//
//  BCEventFilter.h
//  BlueCatsSDK
//
//  Created by Damien Clarke on 1/07/2014.
//  Copyright (c) 2014 BlueCats. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "BCBeacon.h"

@class BCEvent,BCEventFilterContext;

@protocol BCEventFilter <NSObject>

- (NSArray *) filterBeaconsForEvent:(BCEventFilterContext *)eventContext;

@optional

- (BOOL) shouldApplyFilterWhenNoBeaconsRemaining;

@end

@protocol BCEventFilterDefinition <NSObject>

- (NSString *) filterKey;

@optional

- (NSDictionary *) params;

@end

@protocol BCEventFilterResolver <NSObject>

- (id<BCEventFilter>) resolveFilterFromDefinition:(id<BCEventFilterDefinition>)filterDefinition;

@end

@interface BCEventFilter : NSObject <BCEventFilter>

@property (copy) NSArray* (^FilterBeaconsForEventBlock)(BCEventFilterContext*, NSMutableDictionary*);
@property (copy) BOOL (^ShouldApplyFilterWhenNoBeaconsRemainingBlock)(void);

+(id<BCEventFilter>)filterByPredicate:(NSPredicate*)predicate;
+(id<BCEventFilter>)filterByProximity:(BCProximity)proximity;
+(id<BCEventFilter>)filterByProximities:(NSArray*)proximities;
+(id<BCEventFilter>)filterByCategoriesNamed:(NSArray *)categoryNames;
+(id<BCEventFilter>)filterByCategoriesWithIDs:(NSArray *)categoryIDs;
+(id<BCEventFilter>)filterBySitesNamed:(NSArray *)siteNames;
+(id<BCEventFilter>)filterBySitesWithIDs:(NSArray *)siteIDs;
+(id<BCEventFilter>)filterBySitesWithPredicate:(NSPredicate*)sitePredicate;
+(id<BCEventFilter>)filterByMinTimeIntervalMatched:(NSTimeInterval)minTimeIntervalMatched allowingMaxTimeIntervalNotMatched:(NSTimeInterval)maxTimeIntervalNotMatched;
+(id<BCEventFilter>)filterByMinTimeIntervalBetweenMatches:(NSTimeInterval)minTimeIntervalBetweenMatches;
+(id<BCEventFilter>)filterByMinTimeIntervalBetweenTriggers:(NSTimeInterval)minTimeIntervalBetweenTriggers;
+(id<BCEventFilter>)filterByClosestBeacon;
+(id<BCEventFilter>)filterApplySmoothedAccuracyOverTimeInterval:(NSTimeInterval)timeInterval;
+(id<BCEventFilter>)filterApplySmoothedRSSIOverTimeInterval:(NSTimeInterval)timeInterval;
+(id<BCEventFilter>)filterByExitedBeaconAfterTimeInterval:(NSTimeInterval)minTimeBeforeExit;
+(id<BCEventFilter>)filterByLastExitedBeaconAfterTimeInterval:(NSTimeInterval)minTimeBeforeExit;

@end


