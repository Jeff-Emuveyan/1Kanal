package com.onekanal.fettah.kanal.NotificationService;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.onekanal.fettah.kanal.MainActivity;
import com.onekanal.fettah.kanal.R;

import java.util.Random;

public class MyFireBaseMessagingService extends FirebaseMessagingService {


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        //This it the code that runs when the Android system receives a notification for this particular app:
        if (remoteMessage.getData().size() > 0) {

            showNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"), remoteMessage.getData().get("url"));
        }

    }


    private void showNotification(String title, String body, String url) {

        Intent intent = new Intent(this, MainActivity.class).putExtra("msg",body).putExtra("url", url);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        final String CHANNEL_ID = "com.tweetcellfettah.fettah.tweetcell.ANDROID";

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.tweetlogo)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(body));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(new Random().nextInt(), notificationBuilder.build());
    }




    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);

    }


}
