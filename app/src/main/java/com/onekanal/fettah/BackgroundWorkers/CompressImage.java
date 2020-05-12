package com.onekanal.fettah.BackgroundWorkers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.iceteck.silicompressorr.SiliCompressor;
import com.snatik.storage.Storage;
import com.onekanal.fettah.kanal.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import id.zelory.compressor.Compressor;

public class CompressImage extends AsyncTask<String, Void, String> {

    private File rawImageFile;
    private File appFolder = null;
    private Context c;
    private Bitmap compressedBitmap;
    Handler handler;
    String destinationPath;//where we will like the copied image file to be.
    File copiedImageFile;
    double rawImageFileSize;
    private AlertDialog.Builder alert;
    Uri[] results = null;

    public CompressImage(Context c, File rawImageFile) {
        this.c = c;
        this.rawImageFile = rawImageFile;
    }

    @Override
    protected String doInBackground(String... strings) {

        if(rawImageFile != null && rawImageFile.exists()){//just a safety check

            //lets just get its size
            rawImageFileSize = rawImageFile.length();
            rawImageFileSize = (rawImageFileSize/1024);//in kilobytes.


            if(rawImageFileSize > 1600) {
                Log.e("MESSAGE::", "Image will be compressed");

                //Now, make a copy of this raw image file
                Storage storage = new Storage(c); //A github library

                //where we will like the copied image file to be.
                if (phoneHasSDcard()) {
                    destinationPath = Environment.getExternalStorageDirectory() + "/kanalsd/" + rawImageFile.getName();
                } else {
                    destinationPath = c.getFilesDir() + "/kanalsd/" + rawImageFile.getName();
                }


                if (storage.copy(rawImageFile.getPath(), destinationPath)) {//if we are able to copy the imageFile:

                    //Time to compress
                    copiedImageFile = new File(destinationPath);

                    String filePath = copiedImageFile.getPath();
                    compressedBitmap = BitmapFactory.decodeFile(filePath);

                    rawImageFileSize = rawImageFile.length();
                    rawImageFileSize = (rawImageFileSize/1024);//in kilobytes.
                    toastMessage("size after compress 1 : "+rawImageFileSize);

                    if ((copiedImageFile.exists()) && (rawImageFileSize > 1600)) {

                        try {

                            compressedBitmap = new Compressor(c).compressToBitmap(copiedImageFile);//overwrite the copied image there.


                            rawImageFileSize = rawImageFile.length();
                            rawImageFileSize = (rawImageFileSize/1024);//in kilobytes.
                            toastMessage("size after compress 2 : "+rawImageFileSize);

                            //Now, we need to know if the compressedBitmap is not too large:
                            checkIfImageIsStillTooLarge();
                            //checkIfImageIsStillTooLarge();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Thats all

                    } else {
                        toastMessage("Error: Failed to copy");
                    }

                } else {
                    toastMessage("Error: Unable to copy file");
                }

            }else{
                Log.e("MESSAGE::", "Image is < 400kb large");
            }
        }else{
            toastMessage("Error: File not found "+rawImageFile.exists());
        }

        return null;
    }



    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        handler = new Handler();

        toastMessage("Please wait...");
        MainActivity.circularProgressBar.setVisibility(View.VISIBLE);

        //Before compression starts, we want the screen to be kinda locked

        //Now we create a folder to store the new compressed image because we don't want to overwrite.
        createFolder();
    }



    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        //Remove the lock from the screen

        File outFile = convertCompressedBitmapToFile();

        if(outFile != null) {//this will only happen when the image was actually > 400kb:

            double bytes = outFile.length();//this returns the file size of the song in bytes.
            double kilobytes = (bytes / 1024);//in kilobytes.

            //toastMessage("Final Size is "+String.valueOf(kilobytes));


            if (MainActivity.versionIsHigh) {

                Uri[] results = new Uri[]{Uri.fromFile(outFile)};
                MainActivity.sendDataToWebApp(results);
            } else {
                Uri result = Uri.fromFile(outFile);
                MainActivity.sendDataToWebApp(result);
            }

            toastMessage("Done");
            MainActivity.circularProgressBar.setVisibility(View.INVISIBLE);

        }else{
            //send the image back:
            if (MainActivity.versionIsHigh) {

                Uri[] results = new Uri[]{Uri.fromFile(rawImageFile)};
                MainActivity.sendDataToWebApp(results);
            } else {
                Uri result = Uri.fromFile(rawImageFile);
                MainActivity.sendDataToWebApp(result);
            }

            toastMessage("Done");
            MainActivity.circularProgressBar.setVisibility(View.INVISIBLE);
        }
    }



    private void createFolder() {

        if (phoneHasSDcard()) {//if true, then create the folder in the memory card
            appFolder = new File(Environment.getExternalStorageDirectory(), "kanalsd");

            if(!appFolder.exists()){
                appFolder.mkdirs();
            }


        } else {//if scdard is absent or if the phone supports sdcard but the scdard was removed:
            appFolder = new File(c.getFilesDir()+"/kanalsd");
            if(!appFolder.exists()){
                appFolder.mkdirs();
            }


        }
    }


    private boolean phoneHasSDcard() {

        Storage storage = new Storage(c);

        if(storage.isDirectoryExists(storage.getExternalStorageDirectory())){
            return true;
        }else{
            return false;
        }


    }

    void toastMessage(final String message){

        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(c, message, Toast.LENGTH_SHORT).show();
            }
        });
    }



    private File convertCompressedBitmapToFile(){

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        //if the image was below 400kb 'compressedBitmap' will be null because it was never used. So:

        if(compressedBitmap != null) {
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

            File f;

            if (phoneHasSDcard()) {
                f = new File(Environment.getExternalStorageDirectory() + "/kanalsd/" + rawImageFile.getName());
            } else {
                f = new File(c.getFilesDir() + "/kanalsd/" + rawImageFile.getName());
            }

            FileOutputStream fo = null;
            try {
                fo = new FileOutputStream(f);

                fo.write(bytes.toByteArray());

                fo.flush();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return f;

        }
        return null;
    }


    private void checkIfImageIsStillTooLarge(){

        //This method will continue to compress the compressedBitmap until it is below 400kb

        double bytes = convertCompressedBitmapToFile().length();//this returns the file size of the bitmap in bytes.
        double kilobytes = (bytes/1024);//in kilobytes.

        int kilobytesToInt = (int)kilobytes;

        if(kilobytesToInt > 1600){//if the size is still over 400kb, compress it again.

            //toastMessage("compressing again, 1st compressed size = "+kilobytesToInt);
            try {
                //compressedBitmap = null;
                compressedBitmap =  SiliCompressor.with(c).getCompressBitmap(copiedImageFile.getPath());//overwrite the previous compressed image there.

            } catch (IOException e) {
                toastMessage("Error: IOException");
                e.printStackTrace();
            }
        }else{
            //toastMessage("Size is ok");
        }


    }
}
