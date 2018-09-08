package com.alex.faceswap;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

/**
 * Created by alexander on 2017-08-04.
 */

@SuppressWarnings("DefaultFileTemplate")
class AdMobObj {
    private AdView adView;
    private final Context context;
    private final View    view;

    AdMobObj(final Context context, View view) {
        this.context = context;
        this.view = view;
    }

    void init() {
        MobileAds.initialize(context, context.getString(R.string.admob_test_id));
        adView = view.findViewById(R.id.adView);
        // TODO: Remove test device before release
        final AdRequest adRequest = new AdRequest.Builder().addTestDevice("A5DEC613E62176312AE917D114AF77B4").build();

        adView.loadAd(adRequest);
        adView.setVisibility(View.GONE);


        // Create AdListener
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.i("Ads", "onAdLoaded");

                adView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.i("Ads", "onAdFailedToLoad");
                adView.setVisibility(View.GONE);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Log.i("Ads", "onAdOpened");
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
                Log.i("Ads", "onAdLeftApplication");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
                Log.i("Ads", "onAdClosed");
            }
        });

    }

    void destroyAdView() {
        if (adView != null) {
            adView.destroy();
            adView.setVisibility(View.GONE);
        }
    }

}
