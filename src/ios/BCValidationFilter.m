//
//  BCTestFilter.m
//  BCSalesforceTest
//
//  Created by Damien Clarke on 18/09/2014.
//
//

#import "BCValidationFilter.h"
#import "BCEventFilterContext.h"

@implementation BCValidationFilter

-(NSArray *)filterBeaconsForEvent:(BCEventFilterContext *)eventContext
{
    NSMutableArray* validBeacons = [[NSMutableArray alloc] init];
    for (BCBeacon* beacon in eventContext.beaconsToFilter) {
        if (beacon.siteID) {
            [validBeacons addObject:beacon];
        }
    }
    return validBeacons;
}

@end
