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

    // Splash screen timer
    private static int TIME_OUT = 2000;
    private GifImageView gifImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        gifImageView=(GifImageView)findViewById(R.id.gifImageView);
        //set GifImageView
        try{
            InputStream inputStream=getAssets().open("coinz3.gif");
            byte[] bytes= IOUtils.toByteArray(inputStream);
            gifImageView.setBytes(bytes);
            gifImageView.startAnimation();

        }catch(IOException ex){

        }
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent i = new Intent(SplashScreen.this, LoginScreen.class);
                startActivity(i);
                finish();
            }
        }, TIME_OUT);

    }

}


