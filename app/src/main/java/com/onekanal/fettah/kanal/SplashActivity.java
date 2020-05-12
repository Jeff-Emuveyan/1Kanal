package com.onekanal.fettah.kanal;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        createNotificationChannel();// create a notification channel.

        splashScreenTime();

    }

    public void splashScreenTime(){

        Thread logoTimer = new Thread(){

            public void run(){
                startActivity(new Intent("android.intent.action.HOME"));

                /*try{


                    *//*int logoTimer =0;
                    while(logoTimer<2500){//this means show the splash screen for 2.5 seconds delay
                        sleep(100);
                        logoTimer = logoTimer+100;
                    }*//*

                    startActivity(new Intent("android.intent.action.HOME"));
                }catch(InterruptedException e){

                }finally{
                    finish();
                }*/
            }
        };

        logoTimer.start();

    }



    private void createNotificationChannel(){ //If you don't call this method, you notifications will only show on older versions of android phones.

        final String CHANNEL_ID = "com.tweetcellfettah.fettah.tweetcell.ANDROID";//Our Notification in MyFireBaseService will use this same id to get the channel.
        final String CHANNEL_NAME = "1Kanal channel";

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription("Message from 1Kanal");
            channel.enableVibration(true);
            channel.enableLights(true);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            //Toast.makeText(SplashActivity.this, "New Phone", Toast.LENGTH_LONG).show();
        }
    }

}
