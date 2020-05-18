package com.onekanal.fettah.kanal;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.budiyev.android.circularprogressbar.CircularProgressBar;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onekanal.fettah.BackgroundWorkers.CompressImage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    public static CircularProgressBar circularProgressBar;
    SwipeRefreshLayout mSwipeRefreshLayout;
    String currentURL;

    AlertDialog.Builder alert;

    boolean pulledToRefresh;

    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int FILECHOOSER_RESULTCODE = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    //private WebSettings webSettings;
    private static ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private static ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private Boolean doNotRefresh;
    public static boolean versionIsHigh;

    static Context c;

    // Storage Permissions variables
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        webView = (WebView) findViewById(R.id.activity_main_webview);
        circularProgressBar = (CircularProgressBar) findViewById(R.id.progressBar);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //When you swipe down to refresh

                refresh();

            }//End of onRefresh()
        });

        alert = new AlertDialog.Builder(this);

        circularProgressBar.setVisibility(View.INVISIBLE);// Hide the progressBar

        currentURL = null;
        pulledToRefresh = false;
        doNotRefresh = false;
        versionIsHigh = true; //Let's assume that the app will be run on latest version of Android
        c = this;

        verifyStoragePermissions(this);

        //First check if the android data connection is on
        if(isNetworkConnectivityGood()){// if the network connection is ok:


            // Enable Javascript
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setAllowFileAccess(true);
            webView.setWebChromeClient(new ChromeClient());
            webView.addJavascriptInterface(new JavaScriptInterface(this), "HtmlViewer");

            if (Build.VERSION.SDK_INT >= 19) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            else if(Build.VERSION.SDK_INT >=11 && Build.VERSION.SDK_INT < 19) {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);

                    //What will happen when we are trying to load a page?:

                    //Now do we show the refresh view or not when the page is loading?
                    if(pulledToRefresh){
                        mSwipeRefreshLayout.setRefreshing(true);
                    }else{
                        //circularProgressBar.setVisibility(View.VISIBLE);
                    }

                    currentURL = url;
                    doNotRefresh = false;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    //What will happen when a page has finished loading?:
                    //circularProgressBar.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setRefreshing(false);
                    pulledToRefresh = false;
                    doNotRefresh = false;

                    //Now we need to get the users name from the HTML so that we can use it as 'topic'
                    //Since we don't know which particular url/html page to target, we are going to search every page that loads:
                    webView.loadUrl("javascript:window.HtmlViewer.showHTML" +
                            "('&lt;html&gt;'+document.getElementsByTagName('html')[0].innerHTML+'&lt;/html&gt;');");
                    //Now, get the users name by searching through the html until we get the name. Go to JavaScriptInterface
                    //and see what we did.


                }


                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {

                    currentURL = url;

                    // Stay within this webview and load url
                    view.loadUrl(currentURL);
                    return true;
                }


                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);

                    //circularProgressBar.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setRefreshing(false);
                    pulledToRefresh = false;

                    //What happens when we get a loading error?:
                    /*alert.setTitle("Oops! Something went wrong");
                    alert.setMessage("Try again or Swipe down to reload");
                    alert.show();*/
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);

                    currentURL = failingUrl;
                    //circularProgressBar.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setRefreshing(false);
                    pulledToRefresh = false;

                    //What happens when we get a loading error?:
                    /*alert.setTitle("Oops! Something went wrong");
                    alert.setMessage("Try again or Swipe down to reload");
                    alert.show();*/
                }


                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                    String url = request.getUrl().toString();

                    //Toast.makeText(MainActivity.this, url, Toast.LENGTH_LONG).show();

                    if(url.contains("whatsapp")){// This is what will run when the user clicks on an item on the webview to open whatsapp

                        view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));// this line opens whatsapp

                        return true;
                    }

                    return super.shouldOverrideUrlLoading(view, request);
                }
            });


            //before loading the default url, we need to check if the notification tray already has a url.
            //We also repeat this process in the 'OnResume method'
            Bundle b= getIntent().getExtras();
            String urlFromNotification = null;
            if(b != null){
                urlFromNotification = b.getString("url");
            }

            if(urlFromNotification != null && !urlFromNotification.equals("")){
                webView.loadUrl(urlFromNotification);
            }else{
                webView.loadUrl(UrlLink.url);
            }

        }else{

            //circularProgressBar.setVisibility(View.GONE);
            alert.setTitle("Oops! Network Error");
            alert.setMessage("Your device is not properly connected to the internet. Try again");
            alert.show();

        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;
            doNotRefresh = true;

            // Check that the response is a good one
            if (resultCode == AppCompatActivity.RESULT_OK) {

                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};

                        //Now we can proceed to compress the rawImage( Note: After compressing, we automatically send the path to the web app).
                        if(results[0] != null){

                            File rawImageFile = new File(results[0].getPath());

                            new CompressImage(MainActivity.this, rawImageFile).execute();
                        }

                    }
                }else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        //we may have choosen from the gallery
                        results = new Uri[]{Uri.parse(dataString)};

                        //Now we can proceed to compress the rawImage( Note: After compressing, we automatically send the path to the web app).
                        if(results[0] != null){//because shit happens

                            //File rawImageFile = new File(getImagePath(results[0]));//Use this method for gallery
                            Uri uri = data.getData();

                            File rawImageFile = null;
                            try {
                                rawImageFile = FileUtil.from(MainActivity.this, uri);
                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, "No file found ", Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }

                            if(rawImageFile != null){

                                if(rawImageFile.exists()){

                                    new CompressImage(MainActivity.this, rawImageFile).execute();
                                }else{
                                    Toast.makeText(MainActivity.this, "No file found "+rawImageFile.getPath(), Toast.LENGTH_LONG).show();
                                }

                            }else{
                                Toast.makeText(MainActivity.this, "No file found ", Toast.LENGTH_LONG).show();
                            }

                        }
                    }
                }
            }else{//This is the code that will run if for some reason the user dose not choose any image:
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();

                //And finally send data the the webapp (But this time we are sending a null data because the user did not select anything.
                mFilePathCallback.onReceiveValue(results);//Null data is sent.
                mFilePathCallback = null;
                versionIsHigh = true;
            }




           //For older Android phones:
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == this.mUploadMessage) {
                    return;
                }
                Uri result = null;
                try {
                    if (resultCode != RESULT_OK) {
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                        result = null;

                        //And finally send data the the webapp (But this time we are sending a null data because the user did not select anything.
                        mUploadMessage.onReceiveValue(result);
                        mUploadMessage = null;
                        versionIsHigh = false;

                    } else {
                        // retrieve from the private variable if the intent is null
                        result = data == null ? mCapturedImageURI : data.getData();

                        versionIsHigh = false; //set the version for older phones

                        //Now we can proceed to compress the rawImage( Note: After compressing, we automatically send the path to the web app).
                        File rawImageFile = FileUtil.from(MainActivity.this, result);

                        new CompressImage(MainActivity.this, rawImageFile).execute();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :" + e,
                            Toast.LENGTH_LONG).show();
                }

            }
        }
        return;
    }



    @Override
    public void onBackPressed() {
        if(webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    private void refresh(){

        if(isNetworkConnectivityGood()){

            mSwipeRefreshLayout.setRefreshing(true);
            pulledToRefresh = true;

            //initially when the app starts, currentUrl is empty.
            //This means that if network fails at this early stage, we won't be able to refresh using currentUrl.
            //Our work around is to do this:
            if(currentURL == null){

                //currentURL = webView.getUrl(); This won't work because webview.load() was never called because of network failure
                // So webView.getUrl() will fail.

                //And since we know that currentURL is only null at the start of the app, we can safely reload like this:
                webView.loadUrl(UrlLink.url);

            }else{

                webView.loadUrl(currentURL);
            }
        }else{

            mSwipeRefreshLayout.setRefreshing(false);
            pulledToRefresh = false;
            alert.setTitle("Oops! Network Error");
            alert.setMessage("Your device is not properly connected to the internet. Try again");
            alert.show();
        }

    }


    private boolean isNetworkConnectivityGood(){

        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = connMgr.getActiveNetworkInfo();
        if(info != null && info.isConnected()) {// if the network connection is ok:

            return true;
        }else{

            return false;
        }

    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }


    public static void verifyStoragePermissions(AppCompatActivity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int cameraPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED || cameraPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    public String getImagePath(Uri uri) {

        Cursor cursor = null;


        try{
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();

            return cursor.getString(column_index);

        }catch(Exception e){

            return null;

        }finally {

            if(cursor != null){
                cursor.close();
            }
        }

    }


    public static void sendDataToWebApp(Uri[] results){

        if(mFilePathCallback != null){
            mFilePathCallback.onReceiveValue(results);//sending the data
            mFilePathCallback = null;
        }

    }


    //Overloaded method
    public static void sendDataToWebApp(Uri result){

        if(mUploadMessage != null){
            mUploadMessage.onReceiveValue(result);//sending the data
            mUploadMessage = null;

        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getApplicationContext().
                getSharedPreferences(getPackageName(), Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("lastUrl",webView.getUrl());
        edit.commit();   // can use edit.apply() but in this case commit is better
    }


    @Override
    protected void onResume() {
        super.onResume();

        if(doNotRefresh){
            return;
        }

        Bundle b= getIntent().getExtras();
        String urlFromNotification = null;
        if(b != null){
            urlFromNotification = b.getString("url");
        }

        if(urlFromNotification != null && !urlFromNotification.equals("")){
            webView.loadUrl(urlFromNotification);
            return;
        }

        if(webView != null) {
            SharedPreferences prefs = getApplicationContext().
                    getSharedPreferences(getPackageName(), Activity.MODE_PRIVATE);
            String s = prefs.getString("lastUrl","");
            if(!s.equals("")) {
                webView.loadUrl(s);
            }
        }
    }


    //Inner Class
    public class ChromeClient extends WebChromeClient {
        // For Android 5.0
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, FileChooserParams fileChooserParams) {
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Unable to create Image File", ex);
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }


            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }


            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

            return true;
        }


        // openFileChooser for Android 3.0+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            mUploadMessage = uploadMsg;
            // Create AndroidExampleFolder at sdcard
            // Create AndroidExampleFolder at sdcard
            File imageStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES)
                    , "AndroidExampleFolder");
            if (!imageStorageDir.exists()) {
                // Create AndroidExampleFolder at sdcard
                imageStorageDir.mkdirs();
            }
            // Create camera captured image file path and name
            File file = new File(
                    imageStorageDir + File.separator + "IMG_"
                            + String.valueOf(System.currentTimeMillis())
                            + ".jpg");
            mCapturedImageURI = Uri.fromFile(file);
            // Camera capture image intent
            final Intent captureIntent = new Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            // Create file chooser intent
            Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
            // Set camera intent to file chooser
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                    , new Parcelable[] { captureIntent });
            // On select image call onActivityResult method of activity
            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
        }
        // openFileChooser for Android < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            openFileChooser(uploadMsg, "");
        }
        //openFileChooser for other Android versions
        public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                    String acceptType,
                                    String capture) {
            openFileChooser(uploadMsg, acceptType);
        }




    }//End Inner class



}