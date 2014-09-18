//
//  FirstBeaconChangedEventFilter.m
//  BCSalesforceTest
//
//  Created by Damien Clarke on 17/09/2014.
//
//

#import "BCFirstBeaconChangedEventFilter.h"
#import "BCEventFilterContext.h"

@interface BCFirstBeaconChangedEventFilter()

@property NSString* firstBeaconID;

@end

@implementation BCFirstBeaconChangedEventFilter

-(NSArray *)filterBeaconsForEvent:(BCEventFilterContext *)eventContext
{
    BCBeacon* firstBeacon = [eventContext.beaconsToFilter firstObject];
    
    if (!self.firstBeaconID || ![self.firstBeaconID isEqualToString:firstBeacon.beaconID]) {
        self.firstBeaconID = firstBeacon.beaconID;
        return @[firstBeacon];
    }
    
    return nil;
}

@end
