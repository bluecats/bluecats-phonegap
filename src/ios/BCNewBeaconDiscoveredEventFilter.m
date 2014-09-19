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

@property (nonatomic, strong) NSMutableDictionary* beaconIDLastMatched;

@end

@implementation BCNewBeaconDiscoveredEventFilter

-(NSMutableDictionary *)beaconIDLastMatched
{
    if (!_beaconIDLastMatched) {
        _beaconIDLastMatched = [[NSMutableDictionary alloc] init];
    }
    return _beaconIDLastMatched;
}

-(NSTimeInterval)minTimeBeforeExit
{
    if (_minTimeBeforeExit < 1) {
        _minTimeBeforeExit = 5.0f;
    }
    return _minTimeBeforeExit;
}

-(NSArray *)filterBeaconsForEvent:(BCEventFilterContext *)eventContext
{
    if (!self.beaconIDLastMatched) {
        self.beaconIDLastMatched = [[NSMutableDictionary alloc] init];
    }
    NSMutableArray* discoveredBeacons = [[NSMutableArray alloc] init];
    
    NSDate* matchDate = [NSDate date];
    for (BCBeacon* beacon in eventContext.beaconsToFilter) {
        if (beacon.accuracy == -1) {
            continue;
        }
        
        NSDate* beaconLastMatched = [self.beaconIDLastMatched objectForKey:beacon.beaconID];
        if (!beaconLastMatched || [matchDate timeIntervalSinceDate:beaconLastMatched] > self.minTimeBeforeExit) {
            [discoveredBeacons addObject:beacon];
        }
        
        [self.beaconIDLastMatched setObject:matchDate forKey:beacon.beaconID];
    }
    
    return discoveredBeacons;
}

@end
