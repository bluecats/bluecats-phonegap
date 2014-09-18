//
//  BCNewBeaconDiscoveredEventFilter.m
//  BCSalesforceTest
//
//  Created by Damien Clarke on 17/09/2014.
//
//

#import "BCNewBeaconDiscoveredEventFilter.h"
#import "BCEventFilterContext.h"

@interface BCNewBeaconDiscoveredEventFilter()

@property NSMutableSet* discoveredBeaconIDs;

@end

@implementation BCNewBeaconDiscoveredEventFilter

-(NSArray *)filterBeaconsForEvent:(BCEventFilterContext *)eventContext
{
    for (BCBeacon* beacon in eventContext.beaconsToFilter) {
        if (!self.discoveredBeaconIDs) {
            self.discoveredBeaconIDs = [[NSMutableSet alloc] init];
        }
        
        if (![self.discoveredBeaconIDs containsObject:beacon.beaconID]) {
            [self.discoveredBeaconIDs addObject:beacon.beaconID];
            return @[beacon];
        }
    }
    return nil;
}

@end
