package com.alex.faceswap;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;


/**
 * Created by alexander on 2017-06-25.
 * <p>
 * Utilities for bitmap operations such as loading a bitmap from path (string) and doing basic image
 * processing operations such as resizing images and correct gamma.
 */

@SuppressWarnings("DefaultFileTemplate")
class ImageUtils {
    private final static int MAXIMUM_IMAGE_SIZE = 1200;

    /**
     * Returns a resized version of bm.
     *
     * @param bm input image to resize.
     * @return resized bitmap bm.
     */
    static Bitmap resizeBitmap(Bitmap bm) {

        int h = bm.getHeight();
        int w = bm.getWidth();

        if (h == w) {
            h = MAXIMUM_IMAGE_SIZE;
            w = MAXIMUM_IMAGE_SIZE;

        } else if (h > w) {
            float ratio = (float) w / (float) h;
            h = MAXIMUM_IMAGE_SIZE;
            w = (int) (MAXIMUM_IMAGE_SIZE * ratio);

        } else {
            float ratio = (float) h / w;
            w = MAXIMUM_IMAGE_SIZE;
            h = (int) (MAXIMUM_IMAGE_SIZE * ratio);
        }

        return Bitmap.createScaledBitmap(bm, w, h, true);
    }

    /**
     * Returns a bitmap of right size proportions.
     *
     * @param path is the path to the image.
     * @return a bitmap from path.
     */
    static Bitmap makeBitmap(String path) {
        Bitmap bm1 = BitmapFactory.decodeFile(path);
        return resizeBitmap(bm1);
    }

    /**
     * Adjusts the input images for equal size, but does not change image scaling. It pads the images
     * with zeros to get equal size.
     *
     * @param bitmap1 first image to pad with zeros.
     * @param bitmap2 second image to pad with zeros.
     * @return Adjusted images.
     */
    static Bitmap[] makeEqualProps(Bitmap bitmap1, Bitmap bitmap2) {
        // Make the images of equal size, otherwise nasty errors.
        int maxW = bitmap1.getWidth() > bitmap2.getWidth() ? bitmap1.getWidth() : bitmap2.getWidth();
        int maxH = bitmap1.getHeight() > bitmap2.getHeight() ? bitmap1.getHeight() : bitmap2.getHeight();

        Bitmap.Config conf     = Bitmap.Config.ARGB_8888;
        Bitmap        bmp1Temp = Bitmap.createBitmap(maxW, maxH, conf);
        Bitmap        bmp2Temp = Bitmap.createBitmap(maxW, maxH, conf);

        // Padding images with zeros so they will get same size. Otherwise reading outside of
        // memory and will generate nasty exceptions.
        Bitmap[] bmps = new Bitmap[2];
        bmps[0] = ImageUtils.overlay(bmp1Temp, bitmap1);
        bmps[1] = ImageUtils.overlay(bmp2Temp, bitmap2);

        return bmps;
    }

    /**
     * Paste bmp2 over bmp1. The latter shall be zeros.
     *
     * @param bmp1 over bmp2.
     * @param bmp2 under bmp1.
     */
    private static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas    = new Canvas(bmOverlay);

        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, new Matrix(), null);
        return bmOverlay;
    }

    /**
     * Rotates an input image.
     *
     * @param source input image.
     * @param angle  the negative angle.
     * @return rotated image.
     */
    static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Flips image horizontally.
     *
     * @param src input image.
     * @return flipped image.
     */
    static Bitmap flipHorizontally(Bitmap src) {
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
    }

    /**
     * Returns a gray scale version of the input bitmap.
     *
     * @param bmpOriginal the image to convert.
     * @return gray scale version.
     */
    static Bitmap toGrayScale(Bitmap bmpOriginal) {
        int         height       = bmpOriginal.getHeight();
        int         width        = bmpOriginal.getWidth();
        Bitmap      bmpGrayScale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas      c            = new Canvas(bmpGrayScale);
        Paint       paint        = new Paint();
        ColorMatrix cm           = new ColorMatrix();

        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);

        return bmpGrayScale;
    }

    /**
     * Makes gamma correction on an input bitmap. Uses OpenCV.
     *
     * @param bm    input image.
     * @param gamma gamma value, 0. <= gamma <= 2.0
     * @return gamma corrected bitmap.
     */
    static Bitmap gammaCorrection(Bitmap bm, double gamma) {

        Mat im = new Mat();
        bitmapToMat(bm, im);

        Mat lut = new Mat(1, 256, CvType.CV_8UC1);
        lut.setTo(new Scalar(0));
        Imgproc.cvtColor(im, im, Imgproc.COLOR_BGRA2BGR);


        for (int i = 0; i < 256; i++)
            lut.put(0, i, Math.pow(1.0 * i / 255, 1 / gamma) * 255);

        Core.LUT(im, lut, im);

        Imgproc.cvtColor(im, im, Imgproc.COLOR_BGR2BGRA);

        Bitmap res = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888);
        matToBitmap(im, res);

        return res;
    }

    /**
     * Returns a sepia filtered version of src.
     *
     * @param src source image.
     * @return sepia filtered image.
     */
    static Bitmap sepiaBitmap(Bitmap src) {

        ColorMatrix colorMatrix_Sepia = new ColorMatrix();
        colorMatrix_Sepia.setSaturation(0);

        ColorMatrix colorScale = new ColorMatrix();
        colorScale.setScale(1, 1, 0.8f, 1);
        colorMatrix_Sepia.postConcat(colorScale);

        ColorFilter ColorFilter_Sepia = new ColorMatrixColorFilter(colorMatrix_Sepia);
        Bitmap      bitmap            = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas      canvas            = new Canvas(bitmap);
        Paint       paint             = new Paint();

        paint.setColorFilter(ColorFilter_Sepia);
        canvas.drawBitmap(src, 0, 0, paint);

        return bitmap;
    }

    /**
     * Updates the saturation of the input bitmap.
     *
     * @param src        input bitmap.
     * @param saturation the new saturation value.
     * @return bitmap with updated saturation.
     */
    static Bitmap saturatedBitmap(Bitmap src, float saturation) {

        if (saturation < 0.0) {
            return src;
        }

        int w = src.getWidth();
        int h = src.getHeight();

        Bitmap      bitmapResult = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas      canvasResult = new Canvas(bitmapResult);
        Paint       paint        = new Paint();
        ColorMatrix colorMatrix  = new ColorMatrix();

        colorMatrix.setSaturation(saturation);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvasResult.drawBitmap(src, 0, 0, paint);

        return bitmapResult;
    }

    /**
     * Adjusts the brightness and contrast in the input bitmap.
     *
     * @param bitmap     input bitmap.
     * @param contrast   the new contrast value.
     * @param brightness the new brightness value.
     * @return bitmap with contrast and brightness changes.
     */
    static Bitmap contrastAndBrightnessController(Bitmap bitmap, float contrast, float brightness) {

        ColorMatrix cmatrix = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap ret    = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(ret);
        Paint  paint  = new Paint();

        paint.setColorFilter(new ColorMatrixColorFilter(cmatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return ret;
    }

    /**
     * Draws a logotype on the input bitmap.
     *
     * @param context input context.
     * @param bitmap  the bitmap to draw stamp on.
     * @return a new bitmap with stamp on.
     */
    static Bitmap drawLogotype(Context context, Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();

        Bitmap waterBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.water_stamp);
        double maxH        = (double) h * (double) w * 0.00005f;
        double ratio       = (double) (waterBitmap.getWidth()) / (double) (waterBitmap.getHeight());
        double maxW        = maxH * ratio;

        // Extra protection, should never happen. Just so stamp is not wider or higher than the
        // main bitmap used.
        while (maxW > w) {
            maxW *= 0.9;
            maxH *= 0.9;
        }

        while (maxH > h) {
            maxW *= 0.9;
            maxH *= 0.9;
        }

        waterBitmap = Bitmap.createScaledBitmap(waterBitmap, (int) (maxW), (int) (maxH), true);

        return pasteOnBitmap(bitmap, waterBitmap);
    }

    /**
     * Draws a logotype on a bitmap.
     * https://stackoverflow.com/questions/8119121/how-to-draw-the-small-logo-type-image-at-right-bottom-corner-of-the-canvas
     *
     * @param mainImage the bitmap to draw the logotype on.
     * @param logoImage the logotype
     * @return a new bitmap with logotype.
     */
    private static Bitmap pasteOnBitmap(Bitmap mainImage, Bitmap logoImage) {
        int width  = mainImage.getWidth();
        int height = mainImage.getHeight();

        Bitmap finalImage = Bitmap.createBitmap(width, height, mainImage.getConfig());
        Canvas canvas     = new Canvas(finalImage);

        canvas.drawBitmap(mainImage, 0, 0, null);
        canvas.drawBitmap(logoImage, 0, canvas.getHeight() - logoImage.getHeight(), null);

        return finalImage;
    }

    /**
     * Save picture to phone.
     *
     * @param bm      bitmap to save
     * @param imgName image name as string
     * @return true if picture was saved.
     */
     static boolean saveImage(ContentResolver resolver, Bitmap bm, String imgName) {
        OutputStream fOut;
        String       strDirectory = Environment.getExternalStorageDirectory().toString();

        File f = new File(strDirectory, imgName);
        try {
            fOut = new FileOutputStream(f);

            /* Compress image */
            bm.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.flush();
            fOut.close();

            /* Update image to gallery */
            MediaStore.Images.Media.insertImage(resolver, f.getAbsolutePath(), f.getName(), f.getName());

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static Bitmap rotatedExifBitmap(String path, Bitmap bitmap) {
        Log.d("ImageUtils", "Load EXIF");
        ExifInterface ei = null;
        try {
            ei = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("ImageUtils", "Failed to load EXIF");
            return bitmap;
        }

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap = null;
        switch(orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = ImageUtils.rotateBitmap(bitmap, (float) 90.0);
                Log.d("ImageUtils", "Rotated 90 degrees");
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = ImageUtils.rotateBitmap(bitmap, (float) 180.0);
                Log.d("ImageUtils", "Rotated 180 degrees");
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = ImageUtils.rotateBitmap(bitmap, (float) 270.0);
                Log.d("ImageUtils", "Rotated 270 degrees");
                break;

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                Log.d("ImageUtils", "Rotated 0 degrees");
                rotatedBitmap = bitmap;
        }

        return rotatedBitmap;
    }


}
