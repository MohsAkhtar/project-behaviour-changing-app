package com.napier.mohs.behaviourchangeapp.Profile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.napier.mohs.behaviourchangeapp.R;
import com.napier.mohs.behaviourchangeapp.Utils.SettingsBottomNavigationViewEx;
import com.napier.mohs.behaviourchangeapp.Utils.MethodsFirebase;
import com.napier.mohs.behaviourchangeapp.Utils.AdapterSectionsStatePager;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by User on 6/4/2017.
 */

public class ActivityAccountSettings extends AppCompatActivity {

    private static final String TAG = "ActivityAccountSettings";
    private static final int ACTIVITY_NUMBER = 4;

    private Context mContext;

    public AdapterSectionsStatePager adapter;

    // Widgets
    @BindView(R.id.imageAccountBackArrow) ImageView backArrow;
    @BindView(R.id.containerViewPager)ViewPager mViewPager;
    @BindView(R.id.relLayout1) RelativeLayout mRelativeLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accountsettings);
        ButterKnife.bind(this);

        mContext = ActivityAccountSettings.this;
        Log.d(TAG, "onCreate: started.");

        setupSettings();
        setupBottomBar();
        setupFragments();
        getIntentIncoming();

        //setup the backarrow for navigating back to "ActivityProfile"
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating back to 'ActivityProfile'");
                finish();
            }
        });
    }

    private void getIntentIncoming() {
        Intent intent = getIntent();
        // checks if we have extra and only then proceeds
        if(intent.hasExtra(getString(R.string.image_selected))
                || intent.hasExtra(getString(R.string.bitmap_selected))){

            // if there is an image url attached as extra it means it was chose from the gallery or photo fragment
            Log.d(TAG, "getIntentIncoming: new image url recieved ");
            if (intent.getStringExtra(getString(R.string.return_to_fragment)).equals(getString((R.string.fragment_edit_profile)))) {

                if(intent.hasExtra(getString(R.string.image_selected))){
                    // new profile picture is set
                    MethodsFirebase methodsFirebase = new MethodsFirebase(ActivityAccountSettings.this);
                    // as profile photo caption is null
                    // uploads profile photo to firebase storage
                    methodsFirebase.uploadPhotoNew(getString(R.string.profile_photo), null,
                            0, intent.getStringExtra(getString(R.string.image_selected)), null);
                }
                else if(intent.hasExtra(getString(R.string.bitmap_selected))) {
                    // new profile picture is set
                    MethodsFirebase methodsFirebase = new MethodsFirebase(ActivityAccountSettings.this);
                    // as profile photo caption is null
                    // uploads profile photo to firebase storage
                    methodsFirebase.uploadPhotoNew(getString(R.string.profile_photo), null,
                            0, null, (Bitmap) intent.getParcelableExtra(getString(R.string.bitmap_selected)));
                }

            }
        }

        // Checks if there is an incoming intent that has an extra
        if (intent.hasExtra(getString(R.string.calling_activity))) {
            Log.d(TAG, "getIntentIncoming: recieved intent from " + getString(R.string.profile_activity));
            setupViewPagerFragment(adapter.getFragmentNumber(getString(R.string.fragment_edit_profile))); // setsViewPager to incoming intent
        }
    }

    private void setupFragments() {
        adapter = new AdapterSectionsStatePager(getSupportFragmentManager());
        adapter.addFragment(new FragmentEditProfile(), getString(R.string.fragment_edit_profile)); //fragment 0
        adapter.addFragment(new FragmentSignOut(), getString(R.string.fragment_sign_out)); //fragment 1
    }

    // method responsible for actually navigating to fragment
    public void setupViewPagerFragment(int fragmentNumber) {
        mRelativeLayout.setVisibility(View.GONE); // sets visibility of relative layout to gone (Account settings goes away and only fragment is visible)
        Log.d(TAG, "setupViewPagerFragment: nav to fragment number: ");
        mViewPager.setAdapter(adapter); // sets up adapter
        mViewPager.setCurrentItem(fragmentNumber); // navigates to fragment that I chose
    }

    private void setupSettings() {
        Log.d(TAG, "setupSettings: initializing 'Account Settings' list.");
        ListView listView = (ListView) findViewById(R.id.listviewAccountSettings);

        ArrayList<String> options = new ArrayList<>();
        options.add(getString(R.string.fragment_edit_profile)); //fragment 0
        options.add(getString(R.string.fragment_sign_out)); //fragement 1

        ArrayAdapter adapter = new ArrayAdapter(mContext, android.R.layout.simple_list_item_1, options);
        listView.setAdapter(adapter);

        // To set fragment depends on what list item we click here
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Log.d(TAG, "onItemClick: nav to fragment number: " + position);
                setupViewPagerFragment(position); // position depends on what order you added fragment so edit profile is pos 1, sign out is pos 2...
            }
        });

    }

    // setup for BottomNavigationView
    private void setupBottomBar() {
        Log.d(TAG, "bottomNavigationViewExSetup: setting up BottomNavigationView");
        BottomNavigationViewEx bottomBar = (BottomNavigationViewEx) findViewById(R.id.bottomNavViewBar);
        SettingsBottomNavigationViewEx.bottomNavigationViewExSetup(bottomBar);
        SettingsBottomNavigationViewEx.enableNavigation(mContext, this, bottomBar);
        Menu menu = bottomBar.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUMBER);
        menuItem.setChecked(true);
    }


}
















