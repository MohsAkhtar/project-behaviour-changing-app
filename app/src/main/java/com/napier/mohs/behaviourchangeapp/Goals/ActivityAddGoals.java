package com.napier.mohs.behaviourchangeapp.Goals;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.napier.mohs.behaviourchangeapp.R;
import com.napier.mohs.behaviourchangeapp.Utils.FirebaseMethods;

import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;

/**
 * Created by Mohs on 24/03/2018.
 */

public class ActivityAddGoals extends AppCompatActivity{
    private static final String TAG = "ActivityAddGoals";

    // Firebase Stuff
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myDBRefFirebase;
    private FirebaseMethods mFirebaseMethods;


    private Context mContext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals_add);
        ButterKnife.bind(this);
        Log.d(TAG, "onCreateView: Starting add goals");

        mContext = ActivityAddGoals.this; // keeps context constant

        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myDBRefFirebase = mFirebaseDatabase.getReference();
        mFirebaseMethods = new FirebaseMethods(mContext);
        setupFirebaseAuth();


    }

    // formats number two decimal places
    private static DecimalFormat REAL_FORMATTER = new DecimalFormat("0.##");

    private double numberWeight;

    @BindView(R.id.edittextAddGoalWeight) EditText goalWeight;

    @OnClick(R.id.btnIncreaseWeightAddGoal)
    public void increaseWeight(){
        numberWeight += 2.5;
        String stringWeight = Double.toString(numberWeight);
        goalWeight.setText(stringWeight);
    }

    @OnClick(R.id.btnDecreaseWeightAddGoal)
    public void decreaseWeight(){
        numberWeight -= 2.5;
        if(numberWeight<0){
            numberWeight=0;
        }
        String stringReps = Double.toString(numberWeight);
        goalWeight.setText(stringReps);
    }



    // saves the entered details to the firebase database
    @OnClick(R.id.btnSaveAddGoal)
    public void addEntryToDB() {
        numberWeight = Double.parseDouble(goalWeight.getText().toString());

        // format these to two decimal places
        // set double sto string TODO change this to doubles or longs for firebase
        String weight = String.valueOf(REAL_FORMATTER.format(numberWeight));

        String name = "Test Bicep Curl";
        String current = "2";

        Log.d(TAG, "addEntryToDB: Attempting add Entry " + weight + "kg");
        if(TextUtils.isEmpty(weight)){
            Toasty.error(mContext, "Please Fill Out All Fields", Toast.LENGTH_SHORT).show();
        } else {

            Log.d(TAG, "onClick: navigating back to previous activity");

            mFirebaseMethods.goalAddToDatabase(name, weight, current);
            goalWeight.getText().clear();
            Toasty.success(mContext, "Success!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(mContext, ActivityGoals.class);
            startActivity(intent);
            finish();
        }
    }

    // clears the edit text fields
    @OnClick(R.id.btnClearAddGoal)
    public void clearTextFields(){
        goalWeight.getText().clear();

        numberWeight = 0;

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

