package com.alex.faceswap;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by alexander on 2017-06-29.
 */

@SuppressWarnings("DefaultFileTemplate")
class FaceSwapFragments {
    /* Objects to display images in. */
    private static ImageView image1;
    private static ImageView image2;


    public static class FaceFragmentA extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String    ARG_SECTION_NUMBER = "section_number";
        final                ImageView image              = image1;

        public FaceFragmentA() {
        }

        public void setImage(Bitmap bm) {
            try {
                image.setImageBitmap(bm);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static FaceFragmentA newInstance(int sectionNumber) {
            FaceFragmentA fragment = new FaceFragmentA();
            Bundle        args     = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_selfie_swap, container, false);
            image1 = rootView.findViewById(R.id.image);
            setRetainInstance(true);
            return rootView;
        }
    }

    public static class FaceFragmentB extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String    ARG_SECTION_NUMBER = "section_number";
        final                ImageView image              = image2;

        public FaceFragmentB() {
        }

        public void setImage(Bitmap bm) {
            try {
                image.setImageBitmap(bm);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static FaceFragmentB newInstance(int sectionNumber) {
            FaceFragmentB fragment = new FaceFragmentB();
            Bundle        args     = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_selfie_swap, container, false);
            image2 = rootView.findViewById(R.id.image);
            setRetainInstance(true);
            return rootView;
        }
    }

}
