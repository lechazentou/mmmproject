package fr.istic.lechazentou.fataldestination.spinner.app;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;

/**
 * Created by alban on 19/03/15.
 */
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

    private final String userName;
    private final MainActivity activity;

    private ProgressDialog mDialog;
    public DownloadImageTask(MainActivity activity, View view, String userName) {
        this.activity = activity;
        this.userName = userName;
        mDialog = ProgressDialog.show(view.getContext(),"Please wait...", "Retrieving data ...", true);
    }

    protected void onPreExecute() {

    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", "image download error");
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        activity.displayMarker(userName, result);
        //close
        mDialog.dismiss();
    }
}
