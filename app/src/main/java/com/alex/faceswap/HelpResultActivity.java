package com.alex.faceswap;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public class HelpResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_result);

        // Get view for showing ads
        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);
        // AdMob object
        AdMobObj adMobObj = new AdMobObj(getApplicationContext(), viewGroup);
        adMobObj.init();

        // Disable share icon
        if (Build.VERSION.SDK_INT <= 22) {
            ImageView shareIcon = (ImageView) findViewById(R.id.shareIcon);
            shareIcon.setVisibility(View.GONE);
            TextView shareIconText = (TextView) findViewById(R.id.shareIconText);
            shareIconText.setVisibility(View.GONE);
        }
    }
}
