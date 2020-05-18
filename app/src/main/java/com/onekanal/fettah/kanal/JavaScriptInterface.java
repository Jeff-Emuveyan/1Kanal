package com.onekanal.fettah.kanal;

import android.content.Context;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Classes.User;

public class JavaScriptInterface {

    private Context ctx;
    Handler handler;
    AlertDialog.Builder alert;
    User user;
    static int count = 0;

    JavaScriptInterface(Context ctx){
        this.ctx = ctx;
        handler = new Handler();
        alert = new AlertDialog.Builder(ctx);
    }

    @JavascriptInterface
    public void showHTML(final String html)// this is the method that runs when we call webView.loadUrl(javascript...) in onPageFinished()
    {   //Although the method does not look like it is being used in the program, it actually is.
        //We get the html and we can choose to display it if we want, or search for the users name inside it and display it.

        //Now we have gotten the HTML text, we quickly get the user's name and store it in the database.
        if(userExist()){//If someone has already used the phone, overwrite the new user's name on the old one.
            user = User.findById(User.class, (long) 1); //Get the old user
            user.setUserName(getUserNameFromHTMLText(html));//Change his name
            user.save();

        }else {
            user = new User(getUserNameFromHTMLText(html));//Create a new user
            user.save();
        }


        //Now that we have a user, we can proceed to register his name as a topic so he can get notifications from Firebase:
        FirebaseMessaging.getInstance().subscribeToTopic(user.getUserName());



        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if(user.getUserName() != null && count == 0){
                    Toast.makeText(ctx, "Welcome "+user.getUserName(), Toast.LENGTH_LONG).show();// this will toast the user's name
                    count++;
                }

            }
        });

    }

    private String getUserNameFromHTMLText(String HTML){

        //Here, we try to search through the large HTML text until we get the user's name.
        //Know that if the user's name is 'dev' , then this name will be inside the HTML comment text like so:
        //<!-- androiduser2024start-dev-androiduser2024end -->
        Pattern pattern = Pattern.compile("androiduser2024start-(.*?)-androiduser2024end");
        //That code will simply search the HTML, extract anything in between <h6 class="logged-fullname"> and </h6>
        //So in this case, it will extract client112 => the user's name.

        Matcher matcher = pattern.matcher(HTML);

        String userName = null;

        while(matcher.find()){
            userName = matcher.group(1).trim(); //remove any leading or trailing space
        }

        return userName;
    }


    public boolean userExist(){

        //we simply check it a user object has already been stored in the database
        User oldUser = User.findById(User.class, (long) 1);
        if (oldUser == null) {
            //This means no one has used the app on this phone before.
            return false;
        } else {
            return true;
        }
    }



}
