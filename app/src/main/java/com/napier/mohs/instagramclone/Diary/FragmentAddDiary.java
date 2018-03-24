package com.napier.mohs.instagramclone.Diary;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.napier.mohs.instagramclone.Home.ActivityHome;
import com.napier.mohs.instagramclone.R;
import com.napier.mohs.instagramclone.Utils.FirebaseMethods;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;

/**
 * Created by Mohs on 24/03/2018.
 */

public class FragmentAddDiary extends Fragment{
    private static final String TAG = "FragmentAddDiary";

    // Firebase Stuff
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myDBRefFirebase;
    private FirebaseMethods mFirebaseMethods;


    private Context mContext;


    @BindView(R.id.edittextAddDiaryName)
    EditText addName;

    @BindView(R.id.edittextAddUnit)
    EditText addUnit;

    @BindView(R.id.buttonAddEntry)
    Button addEntry1;

    String dateIntent;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adddiary, container, false);
        ButterKnife.bind(this,view);
        Log.d(TAG, "onCreateView: Starting add diary fragment");

        mContext = getActivity(); // keeps context constant

        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myDBRefFirebase = mFirebaseDatabase.getReference();
        mFirebaseMethods = new FirebaseMethods(mContext);
        ///addEntryToDB();
        setupFirebaseAuth();
        getFromBundleDate();

        //Back pressed Logic for fragment
       view.setFocusableInTouchMode(true);
       view.requestFocus();
       view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        getActivity().finish();
                        Intent intent = new Intent(getActivity(), ActivityDiary.class);
                        startActivity(intent);

                        return true;
                    }
                }
                return false;
            }
        });

        return view;
    }

    // TODO Replace hard coded strings in bundle
    // gets from the bundle the photo from the profile activity interface
    private String getFromBundleDate() {
        Log.d(TAG, "getFromBundleDate: " + getArguments());

        Bundle bundle = this.getArguments();
        // if bundle is not null we actually have received something
        if (bundle != null) {
            Log.d(TAG, "getFromBundleCallingActivity: recieved from calling activity " + bundle.getString("date"));
            Bundle b = getActivity().getIntent().getExtras();
            dateIntent = b.getString("date");
            Log.d(TAG, "getFromBundleDate: date = " + dateIntent);
            return bundle.getString("dateIntent");
        } else {
            Log.d(TAG, "getActivityNumberFromBundle: No Calling Activity recieved");
            Toasty.warning(mContext, "No Date Recieved", Toast.LENGTH_SHORT).show();
            return null;
        }

    }


    @OnClick(R.id.buttonAddEntry)
    public void addEntryToDB() {
        String name = addName.getText().toString();
        String unit = addUnit.getText().toString();
        String date = dateIntent;

        Log.d(TAG, "addEntryToDB: Attempting add Entry " + name + ", " + ", " + unit + ", " + date);
        if(TextUtils.isEmpty(name) || TextUtils.isEmpty(unit)){
            Toasty.error(mContext, "Please Fill Out All Fields", Toast.LENGTH_SHORT).show();
        } else {

            Log.d(TAG, "onClick: navigating back to previous activity");

            mFirebaseMethods.exerciseAddToDatabase(date, name, unit);
            addName.getText().clear();
            addUnit.getText().clear();
            Toasty.success(mContext, "Success!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(mContext, ActivityDiary.class);
            startActivity(intent);
            getActivity().getFragmentManager().popBackStack();
        }
    }



    //------------------------FIREBASE STUFF------------
    // Method to check if a user is signed in app

    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: firbase auth is being setup");
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();


                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged: user signed in " + user);
                } else {
                    Log.d(TAG, "onAuthStateChanged: user signed out");
                }
            }
        };

        // allows to get datasnapshot and allows to read or write to db
        myDBRefFirebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        FirebaseUser user = mAuth.getCurrentUser();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}