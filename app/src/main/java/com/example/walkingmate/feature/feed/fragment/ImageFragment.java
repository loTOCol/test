package com.example.walkingmate.feature.feed.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.walkingmate.R;

public class ImageFragment extends Fragment {

    public Bitmap bmp;

    public ImageFragment(int num, Bitmap bmp){
        this.bmp=bmp;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_image, container, false);
        ImageView imageView=rootView.findViewById(R.id.imageview_feedfrag);
        imageView.setImageBitmap(bmp);
        return rootView;
    }

}
