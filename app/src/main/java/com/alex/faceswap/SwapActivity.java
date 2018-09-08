package com.alex.faceswap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.builders.Actions;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static com.alex.faceswap.R.id.container;


public class SwapActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener, View.OnLongClickListener {
    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */

    /* Constant values */
    private static final int    CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int    RESULT_LOAD_IMAGE                   = 200;
    private static final int    CAMERA_API_LEVEL_LIMIT              = 24;
    private static final String PHOTO_FILENAME                      = "photo.jpg";

    private FloatingActionButton swapFacesButton;           /* Face swap button */
    private FaceSwap             faceSwap;                  /* Face swap alogrithms */
    private SectionsPagerAdapter mSectionsPagerAdapter;     /* Tabs for faces */
    private Bitmap               bitmap1;                   /* First bitmap */
    private Bitmap               bitmap2;                   /* Second bitmap */
    private TabLayout            tabLayout;                 /* Tabs */
    private boolean              image1Changed;             /* Flag for bitmap1 changed */
    private boolean              image2Changed;             /* Flag for bitmap2 changed */
    private Toast                infoToast;                 /* For showing information */
    private ConstraintLayout     tuneView;                  /* For editing a bitmap */
    private ViewPager            mViewPager;                /* Tabs */
    private Uri                  saveUriBitmapA;            /* Uri for chosen bitmap1 */
    private Uri                  saveUriBitmapB;            /* uri for chosen bitmap2 */
    private Toolbar              toolbar;                   /* Action bar */

    /**
     * The {@link ViewPager} that will host the section contents.
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        // Set to use only portrait mode if device has small screen (phone).
        // Tablet will use any rotation.
        if (getResources().getBoolean(R.bool.portrait_only))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        bitmap1 = null;
        bitmap2 = null;
        image1Changed = false;
        image2Changed = false;
        saveUriBitmapA = null;
        saveUriBitmapB = null;
        editBitmap = null;
        faceSwap = new FaceSwap(getApplicationContext());

        // Get swap button, hide it unit two images are selected
        swapFacesButton = (FloatingActionButton) findViewById(R.id.swap_button);
        swapFacesButton.setVisibility(View.GONE);
        tuneView = (ConstraintLayout) findViewById(R.id.tuneImageView);
        tuneView.setVisibility(View.GONE);

        // Code for shared images to this activity
        Intent intent = getIntent();
        String action = intent.getAction();
        String type   = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) fetchImage(intent);

        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                fetchMultipleImages(intent); // Handle multiple images being sent
            }
        }

        // Buttons for long clicks
        ImageButton flipButton   = (ImageButton) findViewById(R.id.flipImageHorizontallyButton);
        ImageButton acceptButton = (ImageButton) findViewById(R.id.acceptChangesButton);
        ImageButton clearButton  = (ImageButton) findViewById(R.id.declineChangesButton);

        if (flipButton != null)
            flipButton.setOnLongClickListener(this);

        if (acceptButton != null)
            acceptButton.setOnLongClickListener(this);

        if (clearButton != null)
            clearButton.setOnLongClickListener(this);


        setupTabs();

        setupToolbar();

        updateViewFirstTime();

        // Will hopefully only be checked once, after application installation
        checkPermissions();

        // Handle Firebase invite code
        handleFirebaseInvites();

        handleAds();
    }

    private void setupTabs() {
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        // Sets tab icons
        //noinspection ConstantConditions
        tabLayout.getTabAt(0).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_face_48dp, null));
        //noinspection ConstantConditions
        tabLayout.getTabAt(1).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_face_48dp, null));

        //tabLayout.setOnTabSelectedListener(this);
        tabLayout.addOnTabSelectedListener(this);
    }

    private void setupToolbar() {
        // The action bar. Set icon and remove title.
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);

        if (toolbar != null) {
            toolbar.setNavigationIcon(R.mipmap.ic_launcher_white);
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Save and restore instance state
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private static final String TAG_SAVE_BITMAP_A = "SaveBitmapA";
    private static final String TAG_SAVE_BITMAP_B = "SaveBitmapB";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(getLocalClassName(), "Hello from onSaveInstanceState");

        if (saveUriBitmapA != null && bitmap1 != null) {
            Log.d(getLocalClassName(), "Saved uri for bitmap A");
            outState.putString(TAG_SAVE_BITMAP_A, saveUriBitmapA.toString());
        }

        if (saveUriBitmapB != null && bitmap2 != null) {
            Log.d(getLocalClassName(), "Saved uri for bitmap B");
            outState.putString(TAG_SAVE_BITMAP_B, saveUriBitmapB.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String strUriBitmapA = savedInstanceState.getString(TAG_SAVE_BITMAP_A);
        String strUriBitmapB = savedInstanceState.getString(TAG_SAVE_BITMAP_B);

        Log.d(getLocalClassName(), "str uri bitmap A " + strUriBitmapA);
        Log.d(getLocalClassName(), "str uri bitmap B " + strUriBitmapB);


        if (strUriBitmapA != null) {
            saveUriBitmapA = Uri.parse(strUriBitmapA);
            Bitmap bmp1 = null;
            try {
                bmp1 = MediaStore.Images.Media.getBitmap(getContentResolver(), saveUriBitmapA);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (bmp1 != null) {
                bitmap1 = bmp1;
                if (tabLayout.getSelectedTabPosition() == 0) {
                    setBitmap(bitmap1, saveUriBitmapA);
                }
            }
        }

        if (strUriBitmapB != null) {
            saveUriBitmapB = Uri.parse(strUriBitmapB);
            Bitmap bmp2 = null;
            try {
                bmp2 = MediaStore.Images.Media.getBitmap(getContentResolver(), saveUriBitmapB);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (bmp2 != null) {
                bitmap2 = bmp2;
                if (tabLayout.getSelectedTabPosition() == 1) {
                    setBitmap(bitmap2, saveUriBitmapB);
                }
            }
        }

        if (bitmap1 != null && bitmap2 != null) {
            swapFacesButton.setVisibility(View.VISIBLE);
        } else {
            swapFacesButton.setVisibility(View.GONE);
        }

        savedInstanceState.clear();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // New intent
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Takes care of images that are sent to the application when it is already running.
     *
     * @param intent current intent.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d(this.getLocalClassName(), "On new intent requested");

        // Code for shared images to this activity
        String action = intent.getAction();
        String type   = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                fetchImage(intent);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                fetchMultipleImages(intent); // Handle multiple images being sent
            }
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Firebase invites code
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void handleFirebaseInvites() {
        // Check for App Invite invitations and launch deep-link activity if possible.
        // Requires that an Activity is registered in AndroidManifest.xml to handle
        // deep-link URLs.
        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {

                    @Override
                    public void onSuccess(PendingDynamicLinkData data) {

                        /*
                        if (data == null) {
                            return;
                        }
                        */

                        //Get the deep link
                        //Uri deepLink = data.getLink();
                        //if (deepLink.getPath().contentEquals(getString(R.string.invitation_deep_link))){
                        // Show potential toast
                        //}
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("FirebaseInvite", "getDynamicLink:onFailure", e);
                    }
                });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Code for sharing images to this application.
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Takes care of a single image that is shared to the app.
     *
     * @param intent used intent
     */
    private void fetchImage(Intent intent) {
        final Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            // Update UI to reflect image being shared
            try {

                final Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                // Must use this not ut lock UI thread
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Bitmap bmp = ImageUtils.resizeBitmap(bitmap);
                        setBitmap(bmp, imageUri);
                    }
                }, 500);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Takes care of multiple shared images. Can only take two images of course.
     *
     * @param intent used intent
     */
    private void fetchMultipleImages(Intent intent) {
        Log.d(this.getLocalClassName(), "Handling multiple images.");

        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris.size() >= 2) {
            // Update UI to reflect multiple images being shared

            try {
                int          len  = imageUris.size();
                final Uri    uri1 = imageUris.get(len - 1);
                final Uri    uri2 = imageUris.get(len - 2);
                final Bitmap bmp1 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri1);
                final Bitmap bmp2 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri2);

                // Must use this not ut lock UI thread
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        if (bmp1 != null && bmp2 != null) {

                            bitmap1 = ImageUtils.resizeBitmap(bmp1);
                            bitmap2 = ImageUtils.resizeBitmap(bmp2);

                            saveUriBitmapA = uri1;
                            saveUriBitmapB = uri2;

                            if (tabLayout == null || mSectionsPagerAdapter == null) {
                                setupTabs();
                            }

                            ((FaceSwapFragments.FaceFragmentA) mSectionsPagerAdapter.getItem(0)).setImage(bitmap1);
                            ((FaceSwapFragments.FaceFragmentB) mSectionsPagerAdapter.getItem(1)).setImage(bitmap2);

                            image1Changed = true;
                            image2Changed = true;

                            swapFacesButton.setVisibility(View.VISIBLE);
                        }
                    }
                }, 500);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Menu bar code
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private static final int REQUEST_INVITE = 1123;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Makes an action when an item is selected in the action bar.
     *
     * @param item selected item containing id.
     * @return true if handled correctly.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_help:
                Intent intent = new Intent(SwapActivity.this, HelpSwapActivity.class);
                startActivity(intent);

                return true;

            case R.id.action_switch_faces:
                switchFaces();
                return true;

            case R.id.action_swap_multiple_faces:
                multiSwapMode();
                return true;

            case R.id.action_tune_image:
                tuneImageMode();
                return true;

            case R.id.share_app:

                Intent inviteIntent = new AppInviteInvitation.IntentBuilder(getString(R.string.button_share_app_text))
                        .setMessage(getString(R.string.invitation_message) + " " + getString(R.string.app_name))
                        .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                        .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                        .build();

                startActivityForResult(inviteIntent, REQUEST_INVITE);

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    /**
     * Starts the camera for collecting image with it.
     *
     * @param view current view
     */
    @SuppressWarnings("UnusedParameters")
    public void cameraMode(View view) {
        if (checkPermissions()) {

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);


            if (Build.VERSION.SDK_INT >= CAMERA_API_LEVEL_LIMIT) {
                //////////////////////////////////////////
                // ONLY FOR APIs >= 24
                //////////////////////////////////////////

                File file = createImageFile();
                if (file != null) {
                    Uri photoURI = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", file);

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                }

            } else {
                //////////////////////////////////////////
                // ONLY FOR APIs < 24
                //////////////////////////////////////////

                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, getPhotoFileUri());

                // Avoid crash
                if (intent.resolveActivity(getPackageManager()) != null) {
                    // Start the image capture intent to take photo
                    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                }
            }

            // Avoid crash
            if (intent.resolveActivity(getPackageManager()) != null) {
                // Start the image capture intent to take photo
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        } else {
            if (infoToast != null) {
                infoToast.cancel();
            }
            infoToast = showInfoToast(getString(R.string.err_permission));
        }
    }

    /**
     * Opens the gallery.
     *
     * @param view current view
     */
    @SuppressWarnings("UnusedParameters")
    public void galleryMode(View view) {
        if (checkPermissions()) {
            // Opens photo album
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, RESULT_LOAD_IMAGE);

        } else {
            if (infoToast != null) {
                infoToast.cancel();
            }
            infoToast = showInfoToast(getString(R.string.err_permission));
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Swapping faces code
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Swaps faces
     *
     * @param view current view
     */
    @SuppressWarnings("UnusedParameters")
    public void swapMode(final View view) {

        // Checking if images were not changed --> no need to run swap algorithm again.
        if (!image1Changed && !image2Changed && GlobalBitmap.img != null && !GlobalBitmap.edited) {
            Intent intent = new Intent(SwapActivity.this, ResultActivity.class);
            startActivity(intent);
        }

        // Swap faces, selfie mode
        else if (bitmap1 != null && bitmap2 != null) {

            final Bitmap[] bmps = ImageUtils.makeEqualProps(bitmap1, bitmap2);
            GlobalBitmap.edited = false;

            if (infoToast != null) {
                infoToast.cancel();
            }
            final Toast toast = showInfoToast(getString(R.string.info_swapping));
            infoToast = toast;

            // Must have this for not to lock uithread
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    Bitmap            swappedBitmap = null;
                    FaceSwap.fsStatus status        = FaceSwap.fsStatus.FACE_NOT_FOUND_IMAGE1;
                    int               rot1          = 90;
                    int               rot2          = 90;
                    String            errMessage    = "";

                    while (status != FaceSwap.fsStatus.FACE_SWAP_OK) {
                        infoToast.show();
                        // Mysterious bug. If catch, no swapping :(
                        try {
                            status = faceSwap.selfieSwap(bmps[0], bmps[1]);
                            swappedBitmap = faceSwap.getRes();
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                            status = FaceSwap.fsStatus.FACE_SWAP_NOK;
                        }

                        switch (status) {
                            case FACE_NOT_FOUND_IMAGE1:
                                // Try to look for faces in rotated image 1
                                bmps[0] = ImageUtils.rotateBitmap(bmps[0], 90.0f);
                                Bitmap[] bms1 = ImageUtils.makeEqualProps(bmps[0], bmps[1]);
                                bmps[0] = bms1[0];
                                bmps[1] = bms1[1];

                                rot1 += 90;
                                if (rot1 == 360) {
                                    errMessage = getString(R.string.err_could_not_swap_x) + " 1";
                                    status = FaceSwap.fsStatus.FACE_SWAP_NOK;
                                }
                                break;

                            case FACE_NOT_FOUND_IMAGE2:
                                // Try to look for faces in rotated image 2
                                bmps[1] = ImageUtils.rotateBitmap(bmps[1], 90.0f);
                                Bitmap[] bms2 = ImageUtils.makeEqualProps(bmps[0], bmps[1]);
                                bmps[0] = bms2[0];
                                bmps[1] = bms2[1];

                                rot2 += 90;
                                if (rot2 == 360) {
                                    errMessage = getString(R.string.err_could_not_swap_x) + " 2";
                                    status = FaceSwap.fsStatus.FACE_SWAP_NOK;
                                }
                                break;

                            case FACE_SWAP_INSUFFICIENT_LANDMARKS_IMAGE1:
                                errMessage = getString(R.string.err_could_not_swap_x) + " 1";
                                status = FaceSwap.fsStatus.FACE_SWAP_NOK;
                                break;

                            case FACE_SWAP_INSUFFICIENT_LANDMARKS_IMAGE2:
                                errMessage = getString(R.string.err_could_not_swap_x) + " 2";
                                status = FaceSwap.fsStatus.FACE_SWAP_NOK;
                                break;

                        }

                        if (status == FaceSwap.fsStatus.FACE_SWAP_NOK) {
                            break;
                        }
                    }

                    // Error was found, display message.
                    if (status == FaceSwap.fsStatus.FACE_SWAP_NOK) {
                        toast.cancel();
                        if (infoToast != null) {
                            infoToast.cancel();
                        }
                        showInfoToast(errMessage);
                    }

                    // If no errors: proceed
                    if (status == FaceSwap.fsStatus.FACE_SWAP_OK) {

                        // If image 2 was rotated, it has to be reset
                        if (rot2 == 180) {
                            swappedBitmap = ImageUtils.rotateBitmap(swappedBitmap, 270.0f);
                        }
                        if (rot2 == 270) {
                            swappedBitmap = ImageUtils.rotateBitmap(swappedBitmap, 180.0f);
                        }

                        Bitmap dest = Bitmap.createBitmap(swappedBitmap, 0, 0, bitmap2.getWidth(), bitmap2.getHeight());

                        if (swappedBitmap != null) {
                            image1Changed = false;
                            image2Changed = false;
                            // Send result to result activity
                            Intent intent = new Intent(SwapActivity.this, ResultActivity.class);

                            GlobalBitmap.img = dest;
                            //GlobalBitmap.img = ImageUtils.drawLogotype(getApplicationContext(), dest);

                            toast.cancel();
                            startActivity(intent);
                        }
                    }
                }
            }, 500);
        }
    }

    /**
     * Makes a swap in an image with >= 2 faces.
     */
    private void multiSwapMode() {

        Bitmap swapBmp = null;

        if (tabLayout.getSelectedTabPosition() == 0 && bitmap1 != null) {
            // Swap faces in bitmap1
            swapBmp = bitmap1;

        } else if (tabLayout.getSelectedTabPosition() == 1 && bitmap2 != null) {
            // Swap faces in bitmap2
            swapBmp = bitmap2;
        }

        // OK to start swapping
        if (swapBmp != null) {

            swapBmp = ImageUtils.resizeBitmap(swapBmp);

            final Bitmap swpBmp = swapBmp;

            if (infoToast != null) {
                infoToast.cancel();
            }
            final Toast toast = showInfoToast(getString(R.string.info_swapping));
            infoToast = toast;

            // Must have this for not to lock uithread
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    // Swap faces
                    FaceSwap.fsStatus status;

                    try {
                        status = faceSwap.multiSwap(swpBmp);
                    } catch (NullPointerException e) {
                        status = FaceSwap.fsStatus.FACE_SWAP_NOK;
                        e.printStackTrace();
                    }

                    if (status == FaceSwap.fsStatus.FACE_SWAP_OK) {
                        Bitmap res = faceSwap.getRes();

                        Intent intent = new Intent(SwapActivity.this, ResultActivity.class);

                        // Draw logo only if that item is not purchased
                        GlobalBitmap.img = res;
                        //GlobalBitmap.img = ImageUtils.drawLogotype(getApplicationContext(), res);

                        toast.cancel();
                        startActivity(intent);

                    } else if (status == FaceSwap.fsStatus.FACE_SWAP_TOO_FEW_FACES) {
                        if (infoToast != null) {
                            infoToast.cancel();
                        }
                        infoToast = showInfoToast(getString(R.string.err_insufficient_faces));

                    } else if (status == FaceSwap.fsStatus.FACE_SWAP_INSUFFICIENT_LANDMARKS_IMAGE1) {
                        if (infoToast != null) {
                            infoToast.cancel();
                        }
                        infoToast = showInfoToast(getString(R.string.err_could_not_swap));
                    }

                }
            }, 500);

        } else {
            if (infoToast != null) {
                infoToast.cancel();
            }
            infoToast = showInfoToast(getString(R.string.err_no_image_present));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Code for tab listeners
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Handles if tab is selected.
     *
     * @param tab current tab.
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        FaceSwapFragments.FaceFragmentA phA = (FaceSwapFragments.FaceFragmentA) mSectionsPagerAdapter.getItem(0);
        FaceSwapFragments.FaceFragmentB phB = (FaceSwapFragments.FaceFragmentB) mSectionsPagerAdapter.getItem(1);

        editBitmap = null;

        if (tabLayout.getSelectedTabPosition() == 0 && bitmap1 != null) {
            phA.setImage(bitmap1);
            mViewPager.setCurrentItem(0);

        } else if (tabLayout.getSelectedTabPosition() == 1 && bitmap2 != null) {
            phB.setImage(bitmap2);
            mViewPager.setCurrentItem(1);
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) { /* Not used */ }

    @Override
    public void onTabReselected(TabLayout.Tab tab) { /* Not used */ }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Button click listeners for tuning
    ///////////////////////////////////////////////////////////////////////////////////////////////
    /* Used for temporary editing. */
    private Bitmap editBitmap = null;

    /**
     * Converts to gray scale image
     *
     * @param view current view
     */
    @SuppressWarnings("UnusedParameters")
    public void onBlackAndWhiteRequested(View view) {
        Bitmap bwBitmap = editBitmap;

        if (bwBitmap == null) {
            // Find out which bitmap to edit
            if (tabLayout.getSelectedTabPosition() == 0 && bitmap1 != null) {
                bwBitmap = bitmap1.copy(bitmap1.getConfig(), true);
            } else if (tabLayout.getSelectedTabPosition() == 1 && bitmap2 != null) {
                bwBitmap = bitmap2.copy(bitmap2.getConfig(), true);
            }
        }

        // Get gray scale image and update view
        if (bwBitmap != null) {
            editBitmap = ImageUtils.toGrayScale(bwBitmap);
            setEditBitmap();
        }
    }

    /**
     * Flips image horizontally
     *
     * @param view current view
     */
    @SuppressWarnings("UnusedParameters")
    public void onFlipHorizontalRequested(View view) {
        Bitmap flipBitmap = editBitmap;

        if (flipBitmap == null) {

            // Find out which bitmap to edit
            if (tabLayout.getSelectedTabPosition() == 0 && bitmap1 != null) {
                flipBitmap = bitmap1.copy(bitmap1.getConfig(), true);
            } else if (tabLayout.getSelectedTabPosition() == 1 && bitmap2 != null) {
                flipBitmap = bitmap2.copy(bitmap2.getConfig(), true);
            }
        }

        // Get flipped image and update view
        if (flipBitmap != null) {
            editBitmap = ImageUtils.flipHorizontally(flipBitmap);
            setEditBitmap();
        }
    }

    /**
     * Changes bitmap1/bitmap2 to changed image.
     *
     * @param view current view
     */
    @SuppressWarnings("UnusedParameters")
    public void onAcceptRequest(View view) {
        // Replace current selected bitmap
        if (editBitmap != null) {
            Uri uri = tabLayout.getSelectedTabPosition() == 0 ? saveUriBitmapA : saveUriBitmapB;
            setBitmap(editBitmap, uri);
        }

        // Reset
        editBitmap = null;

        // Close edit menu
        tuneView.setVisibility(View.GONE);
    }

    /**
     * Resets changes
     *
     * @param view current view
     */
    @SuppressWarnings("UnusedParameters")
    public void onClearRequest(View view) {
        // Update view with default
        if (tabLayout.getSelectedTabPosition() == 0) {
            setBitmap(bitmap1, saveUriBitmapA);
        }
        if (tabLayout.getSelectedTabPosition() == 1) {
            setBitmap(bitmap2, saveUriBitmapB);
        }

        // Reset
        editBitmap = null;

        // Close edit menu
        tuneView.setVisibility(View.GONE);
    }

    /**
     * Sets view with temporary editBitmap
     */
    private void setEditBitmap() {
        if (editBitmap != null) {

            if (tabLayout.getSelectedTabPosition() == 0) {
                ((FaceSwapFragments.FaceFragmentA) mSectionsPagerAdapter.getItem(0)).setImage(editBitmap);
            }
            if (tabLayout.getSelectedTabPosition() == 1) {
                ((FaceSwapFragments.FaceFragmentB) mSectionsPagerAdapter.getItem(1)).setImage(editBitmap);
            }
        }
    }


    /**
     * Handles when user long clicks on the buttons in the edit menu. Will show some information.
     *
     * @param v view from click.
     * @return true if handled correctly.
     */
    @Override
    public boolean onLongClick(View v) {
        if (infoToast != null) {
            infoToast.cancel();
        }

        switch (v.getId()) {
            case R.id.flipImageHorizontallyButton:
                infoToast = showInfoToast(getString(R.string.cd_flip_button));
                return true;

            case R.id.acceptChangesButton:
                infoToast = showInfoToast(getString(R.string.cd_accept_changes_button));
                return true;

            case R.id.declineChangesButton:
                infoToast = showInfoToast(getString(R.string.cd_clear_changes_button));
                return true;
        }

        return false;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // App indexing code
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    @SuppressWarnings("WeakerAccess")
    public Action getIndexApiAction() {
        return Actions.newView("Swap", "http://[ENTER-YOUR-URL-HERE]");
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


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Fragment code
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        private SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // Return a PlaceholderFragment
            if (position == 0) {
                return FaceSwapFragments.FaceFragmentA.newInstance(position);
            }

            if (position == 1) {
                return FaceSwapFragments.FaceFragmentB.newInstance(position);
            }

            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Code for handling images returned from camera or photo album
    ///////////////////////////////////////////////////////////////////////////////////////////////
    /* For path to image when using camera. */
    private String mCurrentPhotoPath;

    /**
     * Updates system when an image is chosen from source e.g. camera or file system.
     *
     * @param requestCode TBD
     * @param resultCode  TBD
     * @param data        TBD
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String path;
        Log.d("SwapActivity", "Recieving image");

        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            ///////////////////////////////////////////////////
            // Handle action if picture was taken with camera
            ///////////////////////////////////////////////////

            if (Build.VERSION.SDK_INT >= CAMERA_API_LEVEL_LIMIT) {
                //////////////////////////////////////////
                // ONLY FOR APIs >= 24
                //////////////////////////////////////////
                if (mCurrentPhotoPath == null) return;
                Uri imageUri = Uri.parse(mCurrentPhotoPath);
                if (imageUri == null) return;
                Bitmap bitmap = ImageUtils.makeBitmap(imageUri.getPath());
                bitmap = ImageUtils.rotatedExifBitmap(imageUri.getPath(), bitmap);

                setBitmap(bitmap, imageUri);
                //boolean b = file.delete();

            } else {
                //////////////////////////////////////////
                // ONLY FOR APIs < 24
                //////////////////////////////////////////
                Uri takenPhotoUri = getPhotoFileUri();
                //noinspection ConstantConditions
                path = takenPhotoUri.getPath();
                Bitmap bitmap = ImageUtils.makeBitmap(path);
                bitmap = ImageUtils.rotatedExifBitmap(path, bitmap);

                setBitmap(bitmap, takenPhotoUri);
            }
        }

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK) {
            //////////////////////////////////////////////////////////
            // Handle action if picture was taken from photo library
            //////////////////////////////////////////////////////////
            Uri    selectedImage = data.getData();
            Bitmap bitmap        = null;

            if (selectedImage.toString().startsWith("content://com.google.android.apps.photos.content")) {
                // Selected image has to be downloaded
                try {
                    InputStream is = getContentResolver().openInputStream(selectedImage);
                    if (is != null) {
                        Bitmap bitmapTemp = BitmapFactory.decodeStream(is);
                        bitmap = ImageUtils.resizeBitmap(bitmapTemp);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }


            } else {
                // Selected image is already stored in phone
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                assert cursor != null;
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                path = cursor.getString(columnIndex);
                cursor.close();

                bitmap = ImageUtils.makeBitmap(path);
                bitmap = ImageUtils.rotatedExifBitmap(path, bitmap);
            }

            if (bitmap != null) {
                setBitmap(bitmap, selectedImage);
            }
        }

        updateSwapButton();

        //////////////////////////////////////////////////////////
        // Handle invitation action
        //////////////////////////////////////////////////////////
        Log.d(this.getLocalClassName(), "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Get the invitation IDs of all sent messages
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                for (String id : ids) {
                    Log.d(this.getLocalClassName(), "onActivityResult: sent invitation " + id);
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Files and paths, mainly for camera.
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets path, to be used when using camera.
     *
     * @return TBD.
     * https://inthecheesefactory.com/blog/how-to-share-access-to-file-with-fileprovider-on-android-nougat/en
     */
    private File createImageFile() {
        // Create an image file name
        String imageFileName = "face_swap_plus_image";
        File   storageDir    = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,
                    "jpg.",
                    storageDir

            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCurrentPhotoPath = image != null ? image.getAbsolutePath() : null;
        return image;
    }

    /**
     * Returns the Uri for a photo stored on disk given the fileName
     *
     * @return uri if found.
     */
    private Uri getPhotoFileUri() {
        // Only continue if the SD Card is mounted
        if (isExternalStorageAvailable()) {
            // Get safe storage directory for photos
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FaceSwap");

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                Log.d(this.getLocalClassName(), "failed to create directory");
            }

            // Return the file target for the photo based on filename
            return Uri.fromFile(new File(mediaStorageDir.getPath() + File.separator + SwapActivity.PHOTO_FILENAME));
        }
        return null;
    }

    /**
     * Checks if storage is available.
     *
     * @return true if available.
     */
    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Update view code
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Updates swap button if both images are ok.
     */
    private void updateSwapButton() {
        if (bitmap1 != null && bitmap2 != null) {
            swapFacesButton.setVisibility(View.VISIBLE);
        } else {
            swapFacesButton.setVisibility(View.GONE);
        }
    }

    /**
     * Sets current fragment with a new bitmap.
     *
     * @param bitmap the new bitmap.
     */
    private void setBitmap(Bitmap bitmap, Uri uri) {

        if (tabLayout.getSelectedTabPosition() == 0) {
            bitmap1 = bitmap;
            saveUriBitmapA = uri;

            ((FaceSwapFragments.FaceFragmentA) mSectionsPagerAdapter.getItem(0)).setImage(bitmap1);

            image1Changed = true;
            GlobalBitmap.img = null;

        } else if (tabLayout.getSelectedTabPosition() == 1) {
            bitmap2 = bitmap;
            saveUriBitmapB = uri;

            ((FaceSwapFragments.FaceFragmentB) mSectionsPagerAdapter.getItem(1)).setImage(bitmap2);

            image2Changed = true;
            GlobalBitmap.img = null;
        }

        updateSwapButton();
    }

    /**
     * Switches bitmap 1 to 2 and vice versa.
     */
    private void switchFaces() {
        // Replaces bitmap 1 with bitmap 2 and vice versa
        if (bitmap1 != null && bitmap2 != null) {

            Bitmap tempBitmap = bitmap2;
            bitmap2 = bitmap1;
            bitmap1 = tempBitmap;

            Uri tempUri = saveUriBitmapB;
            saveUriBitmapB = saveUriBitmapA;
            saveUriBitmapA = tempUri;

            int tabIndex = tabLayout.getSelectedTabPosition();

            if (tabIndex == 0) {
                setBitmap(bitmap1, saveUriBitmapA);

            } else if (tabIndex == 1) {
                setBitmap(bitmap2, saveUriBitmapB);
            }

        } else {
            if (infoToast != null) infoToast.cancel();
            infoToast = showInfoToast(getString(R.string.err_no_image_present));
        }
    }

    /**
     * Opens the tune view.
     */
    private void tuneImageMode() {
        boolean editFlag = false;

        if (bitmap1 != null && tabLayout.getSelectedTabPosition() == 0) {
            editFlag = true;
        }
        if (bitmap2 != null && tabLayout.getSelectedTabPosition() == 1) {
            editFlag = true;
        }

        if (tuneView.getVisibility() == View.VISIBLE) {
            tuneView.setVisibility(View.GONE);

        } else if (editFlag) {
            tuneView.setVisibility(View.VISIBLE);

        } else {
            if (infoToast != null) {
                infoToast.cancel();
            }
            infoToast = showInfoToast(getString(R.string.err_no_image_present));
        }
    }

    /**
     * Shows a toast with information why the swap failed.
     *
     * @param text the messages to be displayed.
     */
    private Toast showInfoToast(CharSequence text) {
        Context context  = getApplicationContext();
        int     duration = Toast.LENGTH_LONG;

        final Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.CENTER, 0, 400);
        toast.show();

        return toast;
    }


    private boolean isInMulti     = false;
    private boolean isInLandscape = false;

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);

        if (isInMultiWindowMode) {
            Log.d(getLocalClassName(), "Layout is in MultiWindow");
            toolbar.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);

        } else {
            Log.d(getLocalClassName(), "Layout is not in MultiWindow");
            if (!isInLandscape) {
                toolbar.setVisibility(View.VISIBLE);
                tabLayout.setVisibility(View.VISIBLE);
            }
        }

        isInMulti = isInMultiWindowMode;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(getLocalClassName(), "Changed orientation");
        handleAds();

        if (!getResources().getBoolean(R.bool.show_menu_landscape) && !isInMulti) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                toolbar.setVisibility(View.GONE);
                tabLayout.setVisibility(View.GONE);
                isInLandscape = true;
            }
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && !isInMulti) {
                toolbar.setVisibility(View.VISIBLE);
                tabLayout.setVisibility(View.VISIBLE);
                isInLandscape = false;
            }
        }
    }

    private void updateViewFirstTime() {

        if (!getResources().getBoolean(R.bool.show_menu_landscape)) {
            Context context = getApplicationContext();

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                toolbar.setVisibility(View.GONE);
                tabLayout.setVisibility(View.GONE);
            }
        }
    }


    private void handleAds() {
        AdMobObj adMobObj = new AdMobObj(getApplicationContext(), findViewById(R.id.constr_content_main));
        adMobObj.init();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Permission code
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private static final int     MY_PERMISSIONS_REQUEST_READ_CONTACTS = 123;
    private              boolean granted                              = true;

    /**
     * Controls if an app has permission to use the camera and internal storage.
     *
     * @return true if permissions are ok otherwise false.
     */
    private boolean checkPermissions() {
        granted = true;

        // List the permissions
        String requests[] = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };

        for (String request : requests) {
            if (ContextCompat.checkSelfPermission(this, request) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
            }
        }

        if (granted) {
            return true;
        }

        ActivityCompat.requestPermissions(this, requests, MY_PERMISSIONS_REQUEST_READ_CONTACTS);

        return granted;
    }

    /**
     * Checks permission.
     *
     * @param requestCode  TBD
     * @param permissions  TBD
     * @param grantResults TBD
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                // permission was granted
                granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            }
        }
    }

}
