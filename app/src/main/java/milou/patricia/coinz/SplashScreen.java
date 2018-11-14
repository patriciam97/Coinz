package milou.patricia.coinz;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.felipecsl.gifimageview.library.GifImageView;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        GifImageView gifImageView = findViewById(R.id.gifImageView);
        //set GifImageView
        try{
            InputStream inputStream=getAssets().open("giflogo2.gif");
            byte[] bytes= IOUtils.toByteArray(inputStream);
            gifImageView.setBytes(bytes);
            gifImageView.startAnimation();

        }catch(IOException ignored){

        }
        //after a while the Login Screen Activity start.
        int TIME_OUT = 1000;
        new Handler().postDelayed(() -> {
            Intent i = new Intent(SplashScreen.this, LoginScreen.class);
            startActivity(i);
            finish();
        }, TIME_OUT);

    }

}


