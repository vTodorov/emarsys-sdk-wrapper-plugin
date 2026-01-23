#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(EmarsysSDKCustomPlugin, "EmarsysSDKCustom",
          
           CAP_PLUGIN_METHOD(echo, CAPPluginReturnPromise);
           
           CAP_PLUGIN_METHOD(requestPermissions, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(register, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(setContact, CAPPluginReturnPromise);
           
           CAP_PLUGIN_METHOD(getUUID, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(setContact, CAPPluginReturnPromise);

           CAP_PLUGIN_METHOD(clearContact, CAPPluginReturnPromise);

           CAP_PLUGIN_METHOD(checkPermissions, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(requestPermissions, CAPPluginReturnPromise);
           
           CAP_PLUGIN_METHOD(getPushToken, CAPPluginReturnPromise);

           CAP_PLUGIN_METHOD(trackEvent, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(trackCart, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(trackItemView, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(trackCategoryView, CAPPluginReturnPromise);
)
