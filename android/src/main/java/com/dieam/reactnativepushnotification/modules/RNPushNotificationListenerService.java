package com.dieam.reactnativepushnotification.modules;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager.LayoutParams;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import android.os.Bundle;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Random;
import android.app.Application;

import static com.dieam.reactnativepushnotification.modules.RNPushNotification.LOG_TAG;

public class RNPushNotificationListenerService extends FirebaseMessagingService {

    private RNReceivedMessageHandler mMessageReceivedHandler;
    private FirebaseMessagingService mFirebaseServiceDelegate;

    public RNPushNotificationListenerService() {
        super();
        this.mMessageReceivedHandler = new RNReceivedMessageHandler(this);
    }

    public RNPushNotificationListenerService(FirebaseMessagingService delegate) {
        super();
        this.mFirebaseServiceDelegate = delegate;
        this.mMessageReceivedHandler = new RNReceivedMessageHandler(delegate);
    }

    @Override
    public void onNewToken(String token) {
        final String deviceToken = token;
        final FirebaseMessagingService serviceRef = (this.mFirebaseServiceDelegate == null) ? this : this.mFirebaseServiceDelegate;
        Log.d(LOG_TAG, "Refreshed token: " + deviceToken);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                // Construct and load our normal React JS code bundle
                final ReactInstanceManager mReactInstanceManager = ((ReactApplication) serviceRef.getApplication()).getReactNativeHost().getReactInstanceManager();
                ReactContext context = mReactInstanceManager.getCurrentReactContext();
                // If it's constructed, send a notification
                if (context != null) {
                    handleNewToken((ReactApplicationContext) context, deviceToken);
                } else {
                    // Otherwise wait for construction, then send the notification
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            handleNewToken((ReactApplicationContext) context, deviceToken);
                            mReactInstanceManager.removeReactInstanceEventListener(this);
                        }
                    });
                    if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                        // Construct it in the background
                        mReactInstanceManager.createReactContextInBackground();
                    }
                }
            }
        });
    }

    private void handleNewToken(ReactApplicationContext context, String token) {
        RNPushNotificationJsDelivery jsDelivery = new RNPushNotificationJsDelivery(context);

        WritableMap params = Arguments.createMap();
        params.putString("deviceToken", token);
        jsDelivery.sendEvent("remoteNotificationsRegistered", params);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        if (message.getData().get("notificationType").equals("Meeting")) {
            Context context = getApplicationContext();
            String packageName = context.getApplicationContext().getPackageName();
            Intent focusIntent = context.getPackageManager().getLaunchIntentForPackage(packageName).cloneFilter();


            focusIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK +
                    LayoutParams.FLAG_SHOW_WHEN_LOCKED +
                    LayoutParams.FLAG_DISMISS_KEYGUARD +
                    LayoutParams.FLAG_TURN_SCREEN_ON);

            getApplicationContext().startActivity(focusIntent);
        } else {
            Bundle bundle = new Bundle();
            try {
                JSONObject objPayload = new JSONObject(message.getData().get("payload"));

                bundle.putString("id", new Random(System.currentTimeMillis()).nextInt() + "");
                bundle.putString("channelId", "conversation-chat");
                bundle.putBoolean("ignoreInForeground", true);
                bundle.putString("title", message.getData().get("alert"));
                bundle.putString("message", objPayload.getString("message"));
                bundle.putString("smallIcon", "logo");

                new RNPushNotificationHelper((Application) getApplicationContext()).sendToNotificationCentre(bundle);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        mMessageReceivedHandler.handleReceivedMessage(message);
    }
}
