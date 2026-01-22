package com.lla.emarsyssdk;

import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.emarsys.Emarsys;
import com.emarsys.config.EmarsysConfig;
import com.emarsys.inapp.ui.InlineInAppView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.emarsys.predict.api.model.PredictCartItem;
import com.getcapacitor.Bridge;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginHandle;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@CapacitorPlugin(name = "EmarsysSDKCustom", permissions = @Permission(strings = {}, alias = "receive"))
public class EmarsysSDKCustomPlugin extends Plugin {
    public EmarsysConfig config;
    public String emarsysDeviceInformationConfig;
    private EmarsysSDKCustom implementation = new EmarsysSDKCustom();
    InlineInAppView inlineInAppView;

    // to mimick firebase
    public EmarsysPushNotification emarsysPushNotification;
    public NotificationManager notificationManager;
    public static Bridge staticBridge = null;
    public static RemoteMessage lastMessage = null;
    private NotificationChannelManager notificationChannelManager;

    @Override
    public void load() {
        String mobileEngageApplicationCode = getConfig().getString("mobileEngageApplicationCode");
        String merchantId = getConfig().getString("merchantId");

        notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        emarsysPushNotification = new EmarsysPushNotification();

        config =
            new EmarsysConfig.Builder()
                .application(this.getActivity().getApplication()) //
                .applicationCode(mobileEngageApplicationCode)
                .merchantId(merchantId)
                .enableVerboseConsoleLogging()
                .build();

        Log.d("Emarsys", config.toString());
        Emarsys.setup(config);

        this.loadInappHandler();
        this.loadPushHandler();
        this.loadGetAppEventHandler();

        this.emarsysDeviceInformationConfig = Emarsys.getConfig().getHardwareId();

        inlineInAppView = new InlineInAppView(getContext());
        inlineInAppView.loadInApp("login");

        staticBridge = this.bridge;
        if (lastMessage != null) {
            fireNotification(lastMessage);
            lastMessage = null;
        }

        notificationChannelManager = new NotificationChannelManager(getActivity(), notificationManager, getConfig());
    }

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }

    // setPushTokenFirebase
    @PluginMethod
    public void setPushTokenFirebase(PluginCall call) {
        String value = call.getString("value");
        System.out.println("get initialization 1 " + value);

        JSObject ret = new JSObject();
        ret.put("value", implementation.initializeEmarsys(value));
        System.out.println("after ret  put value ");

        emarsysPushNotification.onNewToken(value);

        call.resolve(ret);
    }

    @PluginMethod
    public void setContact(PluginCall call) {
        Integer contactFieldId = call.getInt("contactFieldId");
        String contactFieldValue = call.getString("contactFieldValue");

        if (contactFieldId == null || contactFieldValue == null) {
            call.reject("Missing params");
            return;
        }

        Emarsys.setContact(contactFieldId, contactFieldValue );
        call.resolve();
    }

    //Use the clearContact method to remove the device details from the contact record,
    //for example, if the user signs out of the app,
    //and they should not receive personalised messages. The CompletionListener is optional.
    @PluginMethod
    public void clearContact(PluginCall call) {
        Emarsys.clearContact();
        call.resolve();
    }

    @PluginMethod
    public void getDeviceInformation(PluginCall call) {
        String value = this.emarsysDeviceInformationConfig;

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void trackEvent(PluginCall call) {
        String eventName = call.getString("eventName");
        String eventAttributes = call.getString("eventAttributes");
        ObjectMapper mapper = new ObjectMapper();

        try {
            Map<String, String> mappedEventAtributes = mapper.readValue(eventAttributes, Map.class);
            JSObject ret = new JSObject();
            assert eventName != null;
            Emarsys.trackCustomEvent(eventName, mappedEventAtributes);

            call.resolve(ret);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PluginMethod
    public void trackCart(PluginCall call) {
        try {
            JSArray itemsArray = call.getArray("items");

            if (itemsArray == null) {
                itemsArray = new JSArray();
            }

            List<PredictCartItem> cartItems = new ArrayList<>();

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject item = itemsArray.getJSONObject(i);

                String itemId = item.getString("item");
                int quantity = item.getInt("quantity");
                double price = item.getDouble("price");

                cartItems.add(
                        new PredictCartItem(itemId, price, quantity)
                );
            }

            Emarsys.getPredict().trackCart(cartItems);

            call.resolve();

        } catch (Exception e) {
            e.printStackTrace();
            call.reject("Track cart failed", e);
        }
    }

    @PluginMethod
    public void loadTheInapp(PluginCall call) {
        String inAppName = call.getString("inAppName");

        InlineInAppView inlineInAppView = new InlineInAppView(getContext());
        inlineInAppView.loadInApp(inAppName);

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    public static JSObject fromJSONObject(JSONObject obj) throws JSONException {
        Iterator<String> keysIter = obj.keys();
        List<String> keys = new ArrayList<>();
        while (keysIter.hasNext()) {
            keys.add(keysIter.next());
        }

        return new JSObject(obj, keys.toArray(new String[keys.size()]));
    }

    //    this will only listen Deeplink or AppEvent
    public void loadPushHandler() {
        Emarsys.getPush().setNotificationEventHandler((context, s, jsonObject) -> {
            try {
                if (s.equals("DeepLink")) {
                    URL url = new URL(jsonObject.getString("url"));
                    JSObject dataJson = new JSObject();
                    dataJson.put("actionType", s);
                    dataJson.put("url", url);
                    dataJson.put("pushType", "pushNotification");
                    dataJson.put("device", "android");

                    notifyListeners("EmarsysPushDeepLink", dataJson);
                }else{
                    JSObject dataJson = this.fromJSONObject(jsonObject);

                    dataJson.put("actionType", "AppEvent");
                    dataJson.put("pushType", "pushNotification");
                    dataJson.put("device", "android");
                    dataJson.put("eventName", s);

                    notifyListeners("EmarsysPushApplicationEvent", dataJson);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void loadGetAppEventHandler() {
        Emarsys.getOnEventAction().setOnEventActionEventHandler((context, s, jsonObject) -> {
            JSObject dataJson = new JSObject();
            try {
                if (s.equals("inline")) {
                    String title = jsonObject.getString("title");
                    String body = jsonObject.getString("body");
                    String shouldShow = jsonObject.getString("shouldShow");
                    String status = jsonObject.getString("status");
                    String icon = jsonObject.getString("icon");


                    dataJson.put("actionType", "OnEventAction");
                    dataJson.put("device", "android");
                    dataJson.put("eventName", s);
                    dataJson.put("title", title);
                    dataJson.put("shouldShow", shouldShow);
                    dataJson.put("status", status);
                    dataJson.put("icon", icon);
                    dataJson.put("body", body);
                    dataJson.put("stringifiedData", jsonObject.toString());
                }else{
                    dataJson.put("actionType", "OnEventAction");
                    dataJson.put("device", "android");
                    dataJson.put("eventName", s);
                    dataJson.put("stringifiedData", jsonObject.toString());
                }

                notifyListeners("EmarsysInAppApplicationEvent", dataJson);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void loadInappHandler() {
        Emarsys.getInApp().setEventHandler((context, s, jsonObject) -> {
            try {
                if (s.equals("DeepLink")) {
                    URL url = new URL(jsonObject.getString("url"));
                    JSObject dataJson = new JSObject();
                    dataJson.put("actionType", s);
                    dataJson.put("url", url);
                    dataJson.put("pushType", "inapp");
                    dataJson.put("device", "android");

                    notifyListeners("EmarsysInAppDeepLink", dataJson);
                }else{
                    JSObject dataJson = this.fromJSONObject(jsonObject);

                    dataJson.put("actionType", "AppEvent");
                    dataJson.put("pushType", "inapp");
                    dataJson.put("device", "android");
                    dataJson.put("eventName", s);

                    notifyListeners("EmarsysInAppApplicationEvent", dataJson);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    //  -------------------- firebase thingy
    public static void sendRemoteMessage(RemoteMessage remoteMessage) {
        EmarsysSDKCustomPlugin pushPlugin = EmarsysSDKCustomPlugin.getPushNotificationsInstance();

        if (pushPlugin != null) {
            pushPlugin.fireNotification(remoteMessage);
        } else {
            lastMessage = remoteMessage;
        }
    }

    public void fireNotification(RemoteMessage remoteMessage) {
        JSObject remoteMessageData = new JSObject();

        JSObject data = new JSObject();
        remoteMessageData.put("id", remoteMessage.getMessageId());

        for (String key : remoteMessage.getData().keySet()) {
            Object value = remoteMessage.getData().get(key);
            data.put(key, value);
        }
        remoteMessageData.put("data", data);

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification != null) {
            String title = notification.getTitle();
            String body = notification.getBody();
            String[] presentation = getConfig().getArray("presentationOptions");

            if (presentation != null) {
                if (Arrays.asList(presentation).contains("alert")) {
                    Bundle bundle = null;
                    try {
                        ApplicationInfo applicationInfo = getContext()
                                .getPackageManager()
                                .getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
                        bundle = applicationInfo.metaData;
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    int pushIcon = android.R.drawable.ic_dialog_info;

                    if (bundle != null && bundle.getInt("com.google.firebase.messaging.default_notification_icon") != 0) {
                        pushIcon = bundle.getInt("com.google.firebase.messaging.default_notification_icon");
                    }
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(
                            getContext(),
                            NotificationChannelManager.FOREGROUND_NOTIFICATION_CHANNEL_ID
                    )
                            .setSmallIcon(pushIcon)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                    notificationManager.notify(0, builder.build());
                }
            }
            remoteMessageData.put("title", title);
            remoteMessageData.put("body", body);
            remoteMessageData.put("click_action", notification.getClickAction());

            Uri link = notification.getLink();
            if (link != null) {
                remoteMessageData.put("link", link.toString());
            }
        }

        notifyListeners("EmarsysPushNotificationReceived", remoteMessageData, true);
    }

    public static EmarsysSDKCustomPlugin getPushNotificationsInstance() {
        if (staticBridge != null && staticBridge.getWebView() != null) {
            PluginHandle handle = staticBridge.getPlugin("EmarsysSDKCustom");
            if (handle == null) {
                return null;
            }
            return (EmarsysSDKCustomPlugin) handle.getInstance();
        }
        return null;
    }
}
