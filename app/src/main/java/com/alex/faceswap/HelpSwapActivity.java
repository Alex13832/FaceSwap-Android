package com.alex.faceswap;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


public class HelpSwapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_swap);

        // Set to use only portrait mode if device has small screen (phone).
        // Tablet will use any rotation.
        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        AdMobObj adMobObj = new AdMobObj(getApplicationContext(), findViewById(R.id.constr_content_main));
        adMobObj.init();
    }
}
