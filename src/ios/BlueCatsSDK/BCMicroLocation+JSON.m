//
//  BCMicroLocation+JSON.m
//
//  Created by Damien Clarke on 27/05/2014.
//
//

#import "BCMicroLocation+JSON.h"
#import "BCCategory.h"
#import "BCSite.h"
#import "RKObjectMapping.h"
#import "RKObjectMappingOperationDataSource.h"
#import "RKMappingOperation.h"
#import "RKMIMETypeSerialization.h"
#import "RKRelationshipMapping.h"

@implementation BCMicroLocation (JSON)

-(NSDictionary *)toJSONDictionary
{
    // describe category
    RKObjectMapping* categoryMapping = [RKObjectMapping mappingForClass:[BCCategory class]];
    [categoryMapping addAttributeMappingsFromDictionary:@{
                                                          @"id": @"categoryID",
                                                          @"name": @"name"
                                                          }];
    // describe beacon
    RKObjectMapping* beaconMapping = [RKObjectMapping mappingForClass:[BCBeacon class]];
    [beaconMapping addAttributeMappingsFromDictionary:@{
                                                        @"id": @"beaconID",
                                                        @"name": @"name",
                                                        @"teamID": @"teamID",
                                                        @"siteID": @"siteID",
                                                        @"siteName": @"siteName",
                                                        @"proximityUUID": @"proximityUUIDString",
                                                        @"major": @"major",
                                                        @"minor": @"minor",
                                                        @"bluetoothAddress": @"bluetoothAddress",
                                                        @"message": @"message",
                                                        @"version": @"version",
                                                        @"pendingVersion": @"pendingVersion",
                                                        @"measuredPowerAt1Meter" : @"measuredPowerAt1Meter",
                                                        @"accuracy": @"accuracy",
                                                        @"rssi": @"rssi",
                                                        @"proximity": @"proximity"
                                                        }];
    
    [beaconMapping addPropertyMapping:[RKRelationshipMapping relationshipMappingFromKeyPath:@"categories"
                                                                                  toKeyPath:@"categories"
                                                                                withMapping:categoryMapping]];
    
    // describe site
    RKObjectMapping* siteMapping = [RKObjectMapping mappingForClass:[BCSite class]];
    [siteMapping addAttributeMappingsFromDictionary:@{
                                                      @"id": @"siteID",
                                                      @"name": @"name",
                                                      @"greeting": @"greeting",
                                                      @"teamID": @"teamID",
                                                      @"beaconCount": @"beaconCount"
                                                      }];
    
    // describe micro location
    RKObjectMapping* microLocationMapping = [RKObjectMapping mappingForClass:[BCMicroLocation class]];
    
    [microLocationMapping addPropertyMapping:[RKRelationshipMapping relationshipMappingFromKeyPath:@"sites" toKeyPath:@"sites" withMapping:siteMapping]];
    
    
    NSMutableDictionary *jsonDict = [NSMutableDictionary dictionary];
    RKObjectMappingOperationDataSource *dataSource = [RKObjectMappingOperationDataSource new];
    RKMappingOperation *operation = [[RKMappingOperation alloc] initWithSourceObject:self
                                                                   destinationObject:jsonDict
                                                                             mapping:[microLocationMapping inverseMapping]];
    
    operation.dataSource = dataSource;
    
    NSError *error = nil;
    [operation performMapping:&error];
    
//    NSMutableDictionary* beaconsForSiteDict = [[NSMutableDictionary alloc] init];
//    for (NSString* siteID in [self.beaconsForSiteID allKeys]) {
//        
//        NSMutableArray* beacons = [[NSMutableArray alloc] init];
//        
//        for (BCBeacon* beacon in self.beaconsForSiteID[siteID]) {
//            
//            NSMutableDictionary *beaconDict = [NSMutableDictionary dictionary];
//            RKObjectMappingOperationDataSource *beaconDataSource = [RKObjectMappingOperationDataSource new];
//            RKMappingOperation *beaconOperation = [[RKMappingOperation alloc] initWithSourceObject:beacon
//                                                                                 destinationObject:beaconDict
//                                                                                           mapping:[beaconMapping inverseMapping]];
//            
//            beaconOperation.dataSource = beaconDataSource;
//            [beaconOperation performMapping:&error];
//            [self updateBeaconProximityWithStringRepresentation:beaconDict];
//            [beacons addObject:beaconDict];
//        }
//        [beaconsForSiteDict setObject:beacons forKey:siteID];
//    }
    NSMutableArray* beacons = [[NSMutableArray alloc] init];
    for (BCBeacon* beacon in self.beacons) {
        
        NSMutableDictionary *beaconDict = [NSMutableDictionary dictionary];
        RKObjectMappingOperationDataSource *beaconDataSource = [RKObjectMappingOperationDataSource new];
        RKMappingOperation *beaconOperation = [[RKMappingOperation alloc] initWithSourceObject:beacon
                                                                             destinationObject:beaconDict
                                                                                       mapping:[beaconMapping inverseMapping]];
        
        beaconOperation.dataSource = beaconDataSource;
        [beaconOperation performMapping:&error];
        [self updateBeaconProximityWithStringRepresentation:beaconDict];
        [beacons addObject:beaconDict];
    }

    [jsonDict setObject:beacons forKey:@"beacons"];
    
    return jsonDict;
}

-(void)updateBeaconProximityWithStringRepresentation:(NSMutableDictionary*)beaconDictionary
{
    NSNumber* proximityNum = [beaconDictionary objectForKey:@"proximity"];
    if (proximityNum) {
        NSString* proximityString = @"BC_PROXIMITY_UNKNOWN";
        switch ([proximityNum intValue]) {
            case 1:
                proximityString = @"BC_PROXIMITY_IMMEDIATE";
                break;
            case 2:
                proximityString = @"BC_PROXIMITY_NEAR";
                break;
            case 3:
                proximityString = @"BC_PROXIMITY_FAR";
                break;
            default:
                break;
        }
        [beaconDictionary setObject:proximityString forKey:@"proximity"];
    }
}

@end
