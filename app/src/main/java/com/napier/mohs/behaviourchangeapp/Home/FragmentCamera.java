package com.napier.mohs.behaviourchangeapp.Home;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.napier.mohs.behaviourchangeapp.R;

/**
 * Created by Mohs on 15/03/2018.
 */

public class FragmentCamera extends Fragment {
    private static final String TAG = "FragmentCamera";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homecamera, container, false);

        return view;
    }
}
