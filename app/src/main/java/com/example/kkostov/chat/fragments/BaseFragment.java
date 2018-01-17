package com.example.kkostov.chat.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

import com.example.kkostov.chat.activities.MainActivity;
import com.example.kkostov.chat.fragments.ChatFragment.MainFragmentListener;


public class BaseFragment extends Fragment {

    protected  MainFragmentListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            if (activity instanceof MainActivity) {
                mListener = (MainActivity) activity;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MainActivity");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            if (context instanceof MainActivity) {
                mListener = (MainActivity) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement MainActivity");
        }
    }




}
