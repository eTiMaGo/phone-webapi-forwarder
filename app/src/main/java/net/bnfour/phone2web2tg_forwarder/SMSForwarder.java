package net.bnfour.phone2web2tg_forwarder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.content.ContentValues.TAG;
import static android.content.Context.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

// this is hell of a mess
// but hey -- at least i got this working

public class SMSForwarder extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {

            Context appContext = context.getApplicationContext();

            SharedPreferences preferences = getDefaultSharedPreferences(appContext);

            //boolean enabled = preferences.getBoolean("sms_enabled", true);
            String endpoint = preferences.getString("api_url", "");
            String token = preferences.getString("api_token", "");

            PreferenceCheckHelper checker = new PreferenceCheckHelper(appContext);

//            if (!enabled) {
//                return;
//            }
//            if (!checker.isTokenValid(token) || !checker.isEndpointValid(endpoint)) {
//                Notifier.showNotification(appContext, appContext.getString(R.string.bad_connection_notification));
//                return;
//            }

            StringBuilder fulltext = new StringBuilder();
            String strDate = "";
            String sender = "";

            Bundle bundle = intent.getExtras();
            if (bundle.containsKey("pdus")) {

                // here we build a dict where keys are message senders
                // all pdu messages from one sender are combined to one long string
                // (taken straight from SMS Forwarder yet again)
                Map<String, String> messages = new HashMap<String, String>();

                Object[] pdus = (Object[]) bundle.get("pdus");
                for (Object pdu : pdus) {
                    SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
                    sender = msg.getOriginatingAddress();
                    fulltext.append(msg.getMessageBody());
                    long timestamp = msg.getTimestampMillis();
                    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH.mm");
                    strDate = dateFormat.format(timestamp);

                }

                String device = MainPreferencesActivity.android_id;

                String receiverPhone = "na";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    List<SubscriptionInfo> subscription = SubscriptionManager.from(appContext).getActiveSubscriptionInfoList();
                    for (int i = 0; i < subscription.size(); i++) {
                        SubscriptionInfo info = subscription.get(i);
                        Log.d(TAG, "number " + info.getNumber());
                        Log.d(TAG, "network name : " + info.getCarrierName());
                        Log.d(TAG, "country iso " + info.getCountryIso());

                        if (info.getNumber() != null) {
                            receiverPhone = info.getNumber();
                        }
                    }
                }

                Log.d("sender", sender);
                Log.d("device", device);
                Log.d("receiver", receiverPhone);
                Log.d("message", String.valueOf(fulltext));
                Log.d("time", strDate);

                String template = "id=%r|device=%d|time=%c|title=%s|text=%t";
                String toSend = template.replace("%s", sender).replace("%d", device).replace("%t", fulltext.toString()).replace("%r", receiverPhone).replace("%c", strDate);

                new WebApiSender(appContext, endpoint, token).send(toSend);
                
            }
        }
    }
}
