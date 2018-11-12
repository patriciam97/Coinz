package milou.patricia.coinz;


import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;


import java.io.InputStreamReader;

import java.lang.ref.WeakReference;
import java.net.URL;


public class DownloadFileTask extends AsyncTask<String,Void, String> {
    //public AsyncResponse delegate = null;
    private WeakReference<MainActivity> activityReference;

    // only retain a weak reference to the activity
    DownloadFileTask(MainActivity context) {
        activityReference = new WeakReference<>(context);
    }
    @Override
    protected String doInBackground(String... urls) {

        try {
            return loadFileFromNetwork(urls[0]);
             } catch (IOException e) {
                   return "Unable to load content. Check your network connection";
              }
        }

    private String loadFileFromNetwork(String urlString) throws IOException {
        return readStream(downloadUrl(new URL(urlString)));
    }


    @NonNull
    private String readStream(InputStream stream)throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String text="",line;
        while ((line = reader.readLine()) != null)
        {
            text=text+"\n"+line;
        }
        return text;
    }

    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);
        MainActivity activity = activityReference.get();
        //once downloading is complete sent the reuslt to the function
        //"processFinish" at Main Activity

        activity.processFinish(result);

    }


    // Given a string representation of a URL, sets up a connection and gets an input stream.
    private InputStream downloadUrl(URL url) throws IOException {
        return url.openStream();
    }
}
