#ifdef __OBJC__
#import <UIKit/UIKit.h>
#else
#ifndef FOUNDATION_EXPORT
#if defined(__cplusplus)
#define FOUNDATION_EXPORT extern "C"
#else
#define FOUNDATION_EXPORT extern
#endif
#endif
#endif

#import "Capacitor.h"
#import "CAPBridgedPlugin.h"
#import "CAPPlugin.h"
#import "CAPPluginCall.h"
#import "CAPPluginMethod.h"
#import "DefaultPlugins.h"
#import "Keyboard.h"

FOUNDATION_EXPORT double CapacitorVersionNumber;
FOUNDATION_EXPORT const unsigned char CapacitorVersionString[];

