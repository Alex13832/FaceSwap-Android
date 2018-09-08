package com.alex.faceswap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.builders.Actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;


@SuppressWarnings("UnusedParameters")
public class ResultActivity extends AppCompatActivity implements AppCompatSeekBar.OnSeekBarChangeListener, View.OnLongClickListener {
    /* For storing temporary imaging. */
    private Bitmap           bitmap;
    /* For quick settings. */
    private ConstraintLayout editMenu;
    /* For doing fine tuning of result image. */
    private ConstraintLayout adjustMenu;
    /* Temporary image for editing. */
    private Bitmap           editBitmap;
    /* For showing the result */
    private ImageView        imageView;
    /* Used for adjusting gamma value in the result image */
    private AppCompatSeekBar gammaSeekBar;
    /* Used for adjusting saturation in the result image */
    private AppCompatSeekBar saturationSeekBar;
    /* Used for adjusting brightness */
    private AppCompatSeekBar brightnessSeekBar;
    /* Used for adjusting contrast */
    private AppCompatSeekBar contrastSeekBar;
    /* For setting brightness and contrast. */
    private float            brightness;
    private float            contrast;
    private Toast            infoToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // The action bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        // Update tool bar with wanted information, logo and no title.
        if (myToolbar != null) {
            myToolbar.setNavigationIcon(R.mipmap.ic_launcher_empty_background);
            myToolbar.setTitle("");
            setSupportActionBar(myToolbar);
        }

        // Get the menu with buttons, one of them opens the adjustMenu
        editMenu = (ConstraintLayout) findViewById(R.id.tuneImageView);
        if (editMenu != null) editMenu.setVisibility(View.GONE);
        // Get the menu for adjusting the result.
        adjustMenu = (ConstraintLayout) findViewById(R.id.adjustMenu);
        if (adjustMenu != null) adjustMenu.setVisibility(View.GONE);

        // Seek bars
        gammaSeekBar = (AppCompatSeekBar) findViewById(R.id.gammaSeekBar);
        saturationSeekBar = (AppCompatSeekBar) findViewById(R.id.saturationSeekbar);
        brightnessSeekBar = (AppCompatSeekBar) findViewById(R.id.brightnessSeekBar);
        contrastSeekBar = (AppCompatSeekBar) findViewById(R.id.contrastSeekbar);

        if (gammaSeekBar != null) {
            gammaSeekBar.setOnSeekBarChangeListener(this);
        }
        if (saturationSeekBar != null) {
            saturationSeekBar.setOnSeekBarChangeListener(this);
        }
        if (brightnessSeekBar != null) {
            brightnessSeekBar.setOnSeekBarChangeListener(this);
        }
        if (contrastSeekBar != null) {
            contrastSeekBar.setOnSeekBarChangeListener(this);
        }

        // Get swapped bitmap
        bitmap = GlobalBitmap.img;
        imageView = (ImageView) findViewById(R.id.imageView);

        if (bitmap != null && imageView != null) {
            imageView.setImageBitmap(bitmap);
            editBitmap = bitmap.copy(bitmap.getConfig(), true);
        }

        // Default brightness and contrast values.
        brightness = 0.0f;
        contrast = 1.0f;

        // Buttons and icons for long clicks
        ImageButton sepiaButton          = (ImageButton) findViewById(R.id.sepiaButton);
        ImageButton acceptButton         = (ImageButton) findViewById(R.id.acceptChangesButton);
        ImageButton declineChangesButton = (ImageButton) findViewById(R.id.declineChangesButton);
        ImageButton tuneImageButton      = (ImageButton) findViewById(R.id.tuneImageButton);
        ImageButton rotateLeftButton     = (ImageButton) findViewById(R.id.rotateLeftButton);
        ImageView   brightnessIcon       = (ImageView) findViewById(R.id.brightnessIcon);
        ImageView   contrastIcon         = (ImageView) findViewById(R.id.contrastIcon);
        ImageView   gammaIcon            = (ImageView) findViewById(R.id.gammaIcon);
        ImageView   saturationIcon       = (ImageView) findViewById(R.id.saturationIcon);

        if (sepiaButton != null) /* ---------- */ sepiaButton.setOnLongClickListener(this);
        if (acceptButton != null) /* --------- */ acceptButton.setOnLongClickListener(this);
        if (declineChangesButton != null) /* - */ declineChangesButton.setOnLongClickListener(this);
        if (tuneImageButton != null) /* ------ */ tuneImageButton.setOnLongClickListener(this);
        if (rotateLeftButton != null) /* ----- */ rotateLeftButton.setOnLongClickListener(this);
        if (brightnessIcon != null) /* --------*/ brightnessIcon.setOnLongClickListener(this);
        if (contrastIcon != null) /* --------- */ contrastIcon.setOnLongClickListener(this);
        if (gammaIcon != null) /* ------------ */ gammaIcon.setOnLongClickListener(this);
        if (saturationIcon != null) /* ------- */ saturationIcon.setOnLongClickListener(this);

        // Show ads
        handleAds();
    }


    /**
     * Handles ads. Shows a full screen ad if the user has swapped faces a number of times.
     * Default ad is otherwise a banner ad.
     */
    private void handleAds() {
        // Get ad counter
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_prefs_key), Context.MODE_PRIVATE);
        int               adCounter  = sharedPref.getInt("BigAdsCount", 0);

        // Show big ad and close the small ad
        if (adCounter == 5) {
            final InterstitialAd mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
            mInterstitialAd.loadAd(new AdRequest.Builder().build());

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                    }
                }
            }, 1000);

            AdMobObj adMobObj = new AdMobObj(getApplicationContext(), findViewById(R.id.constr_content_main));
            adMobObj.destroyAdView();
            adCounter = 0;
        } else {
            // If the big ad was not shown, show the small ad.
            AdMobObj adMobObj = new AdMobObj(getApplicationContext(), findViewById(R.id.constr_content_main));
            adMobObj.init();
            adCounter++;
        }

        // Save updated value for ad counter.
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("BigAdsCount", adCounter);
        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        super.onStop();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Menu bar code
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private ShareActionProvider mShareActionProvider;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (Build.VERSION.SDK_INT >= 23) {
            getMenuInflater().inflate(R.menu.menu_result, menu);

            // Locate MenuItem with ShareActionProvider
            MenuItem item = menu.findItem(R.id.menu_item_share);
            // Fetch and store ShareActionProvider
            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
            Intent intent = createShareIntent();

            if (intent != null) {
                setShareIntent(intent);
            }
        } else {
            // If api <= 22, then disable share functionality
            getMenuInflater().inflate(R.menu.menu_result_low_api, menu);
        }

        return true;
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) mShareActionProvider.setShareIntent(shareIntent);
    }

    private Intent createShareIntent() {
        try {
            // Create temp path for saving image
            File                                          cachePath = new File(getApplicationContext().getCacheDir(), "images");
            @SuppressWarnings("UnusedAssignment") boolean success   = cachePath.mkdirs();
            Log.d("ResultActivity", "cachePath " + cachePath.getAbsolutePath());
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        File imagePath = new File(getApplicationContext().getCacheDir(), "images");
        Log.d("ResultActivity", "imagePath " + imagePath.getAbsolutePath());
        File newFile    = new File(imagePath, "image.png");
        Uri  contentUri = FileProvider.getUriForFile(getApplicationContext(), "com.alex.faceswap.provider", newFile);
        Log.d("ResultActivity", "contentUri " + contentUri.toString());

        // Create intent for sharing image
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
        shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.setType("image/jpg");

        return shareIntent;
    }

    /**
     * Performs an action when an item is selected in the action bar.
     *
     * @param item selected item with id.
     * @return true if handled correctly.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("ResultActivity", "item " + item.toString());

        switch (item.getItemId()) {
            case R.id.action_save: // Save image
                saveImage();
                return true;

            case R.id.action_tune: // Show or close tune menu
                int visibility = editMenu.getVisibility();

                if (visibility == View.GONE)
                    editMenu.setVisibility(View.VISIBLE);
                else
                    editMenu.setVisibility(View.GONE);

                adjustMenu.setVisibility(View.GONE);
                return true;

            case R.id.action_help:
                Intent intent = new Intent(ResultActivity.this, HelpResultActivity.class);
                startActivity(intent);
                return true;

            case R.id.menu_item_share:
                Intent shareIntent = createShareIntent();
                startActivity(Intent.createChooser(shareIntent, getString(R.string.button_share_image_text)));
                return true;
        }

        return false;
    }

    /**
     * Opens the adjust menu.
     *
     * @param view current view.
     */
    public void onOpenAdjustViewRequested(View view) {
        switch (adjustMenu.getVisibility()) {
            case View.GONE:
                adjustMenu.setVisibility(View.VISIBLE);
                break;
            case View.VISIBLE:
                adjustMenu.setVisibility(View.GONE);
                break;
            case View.INVISIBLE:
                break;
            default:
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Saving images code
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Saves an image to the phone.
     */
    private void saveImage() {
        //String timeStamp  = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        String timeStamp  = SimpleDateFormat.getDateTimeInstance().toString();
        String mImageName = "MI_" + timeStamp + ".jpg";

        if (ImageUtils.saveImage(getContentResolver(), bitmap, mImageName)) {

            try {
                if (infoToast != null) {
                    infoToast.cancel();
                }
                infoToast = showInfoToast(getString(R.string.photo_saved));

            } catch (NullPointerException e) {
                Log.d(this.getLocalClassName(), "Could not save image.");
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Button listeners
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Request a gray scale version of the result bitmap.
     *
     * @param view the current view.
     */
    public void onBlackAndWhiteRequested(View view) {
        editBitmap = ImageUtils.toGrayScale(editBitmap);
        imageView.setImageBitmap(editBitmap);
        bitmap = editBitmap;
    }

    /**
     * Requests a left rotation of 90 degrees of the current image.
     *
     * @param view current view.
     */
    public void onRotate90Requested(View view) {
        editBitmap = ImageUtils.rotateBitmap(editBitmap, -90.0f);
        imageView.setImageBitmap(editBitmap);
        bitmap = editBitmap;
    }

    /**
     * Requests an accept for the made changes.
     *
     * @param view current view.
     */
    public void onAcceptRequest(View view) {
        bitmap = editBitmap;
        GlobalBitmap.img = bitmap;
        GlobalBitmap.edited = true;
        editMenu.setVisibility(View.GONE);
        adjustMenu.setVisibility(View.GONE);
    }

    /**
     * Clears the changes made on the result image.
     *
     * @param view current view.
     */
    public void onClearRequest(View view) {
        bitmap = GlobalBitmap.img;
        editBitmap = bitmap;
        imageView.setImageBitmap(bitmap);

        brightness = 0.0f;
        contrast = 1.0f;

        // Reset seek bar(s)
        gammaSeekBar.setProgress(50);
        saturationSeekBar.setProgress(50);
        brightnessSeekBar.setProgress(255);
        contrastSeekBar.setProgress(10);
    }

    /**
     * Sends a string to a toast when long clicking a button or an icon.
     *
     * @param view current view.
     * @return TBD.
     */
    @Override
    public boolean onLongClick(View view) {

        if (infoToast != null) infoToast.cancel();

        switch (view.getId()) {
            case R.id.sepiaButton:
                infoToast = showInfoToast(getString(R.string.cd_sepia_button));
                return true;

            case R.id.acceptChangesButton:
                infoToast = showInfoToast(getString(R.string.cd_accept_changes_button));
                return true;

            case R.id.declineChangesButton:
                infoToast = showInfoToast(getString(R.string.cd_clear_changes_button));
                return true;

            case R.id.tuneImageButton:
                infoToast = showInfoToast(getString(R.string.cd_tuning_button));
                return true;

            case R.id.rotateLeftButton:
                infoToast = showInfoToast(getString(R.string.rotate_button_image_text));
                return true;

            case R.id.brightnessIcon:
                infoToast = showInfoToast(getString(R.string.icon_brightness_icon_text));
                return true;

            case R.id.contrastIcon:
                infoToast = showInfoToast(getString(R.string.icon_contrast_icon_text));
                return true;

            case R.id.gammaIcon:
                infoToast = showInfoToast(getString(R.string.icon_gamma_icon_text));
                return true;

            case R.id.saturationIcon:
                infoToast = showInfoToast(getString(R.string.icon_saturation_icon_text));
                return true;
        }

        return false;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Image filter
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Requests gamma correction.
     *
     * @param gamma value, 0.0 <= gamma <= 2.0
     */
    private void gammaCorrection(double gamma) {
        editBitmap = bitmap;
        editBitmap = ImageUtils.gammaCorrection(editBitmap, gamma);
        imageView.setImageBitmap(editBitmap);
    }

    /**
     * Gets a saturated bitmap from ImageUtils.
     *
     * @param saturation level of saturation.
     */
    private void saturationCorrection(double saturation) {
        editBitmap = bitmap;
        editBitmap = ImageUtils.saturatedBitmap(editBitmap, (float) saturation);
        imageView.setImageBitmap(editBitmap);
    }

    /**
     * Requests a sepia filtered image.
     *
     * @param view current view.
     */
    public void onSepiaFilterRequested(View view) {
        editBitmap = ImageUtils.sepiaBitmap(editBitmap);
        imageView.setImageBitmap(editBitmap);
        bitmap = editBitmap;
    }

    /**
     * Updates the brightness and contrast.
     *
     * @param bright brightness value.
     * @param contr  contrast value
     */
    private void brightnessAndContrastUpdate(float bright, float contr) {
        editBitmap = ImageUtils.contrastAndBrightnessController(bitmap, contr, bright);
        imageView.setImageBitmap(editBitmap);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Seek bar code
    ///////////////////////////////////////////////////////////////////////////////////////////////
    /* For tracking current seekBar id, if it changes then update bitmap. */
    private int currId = R.id.brightnessSeekBar;

    /**
     * Handles when progress is changed.
     *
     * @param seekBar  current seek bar
     * @param progress current progress.
     * @param fromUser TBD.
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { /* Not used */}

    /**
     * Handles when seekBar change is started.
     *
     * @param seekBar used seekBar.
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (seekBar.getId() != currId) {
            currId = seekBar.getId();
            bitmap = editBitmap;
        }
    }

    /**
     * Handles when progress stops.
     *
     * @param seekBar used seekBar.
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        if (seekBar.getId() == R.id.gammaSeekBar) {
            // Update gamma
            int    gammaRaw = seekBar.getProgress();
            double gamma    = 2.0 * ((double) (gammaRaw) / (double) (seekBar.getMax()));

            gammaCorrection(gamma);

        } else if (seekBar.getId() == R.id.saturationSeekbar) {
            // Update saturation
            int    satRaw     = seekBar.getProgress();
            double saturation = 4.0 * ((double) (satRaw) / (double) (seekBar.getMax()));

            saturationCorrection(saturation);

        } else if (seekBar.getId() == R.id.brightnessSeekBar) {
            // Update brightness
            int brightnessRaw = seekBar.getProgress();
            brightness = (float) brightnessRaw - 255.0f;

            brightnessAndContrastUpdate(brightness, contrast);


        } else if (seekBar.getId() == R.id.contrastSeekbar) {
            // Update contrast
            int contrastRaw = seekBar.getProgress();
            contrast = (float) contrastRaw / 10.0f;

            brightnessAndContrastUpdate(brightness, contrast);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Toasts
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Shows a toast with information why the swap failed.
     *
     * @param text the messages to be displayed.
     */
    private Toast showInfoToast(CharSequence text) {
        Context     context  = getApplicationContext();
        int         duration = Toast.LENGTH_SHORT;
        final Toast toast    = Toast.makeText(context, text, duration);

        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        return toast;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // App indexing
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    @SuppressWarnings("WeakerAccess")
    public Action getIndexApiAction() {
        return Actions.newView("Result", "http://[ENTER-YOUR-URL-HERE]");
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        FirebaseUserActions.getInstance().start(getIndexApiAction());
    }

    @Override
    public void onStop() {

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        FirebaseUserActions.getInstance().end(getIndexApiAction());
        super.onStop();
    }


}