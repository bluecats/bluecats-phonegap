//
//  BCNewBeaconDiscoveredEventFilter.h
//  BCSalesforceTest
//
//  Created by Damien Clarke on 17/09/2014.
//
//

#import <Foundation/Foundation.h>
#import "BCEventFilter.h"

@interface BCNewBeaconDiscoveredEventFilter : NSObject<BCEventFilter>

@property (nonatomic, assign) NSTimeInterval minTimeBeforeExit;

@end
