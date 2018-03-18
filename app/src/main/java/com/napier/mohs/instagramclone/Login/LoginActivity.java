package com.napier.mohs.instagramclone.Login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.napier.mohs.instagramclone.Home.HomeActivity;
import com.napier.mohs.instagramclone.R;

/**
 * Created by Mohs on 17/03/2018.
 */

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    // Firebase Stuff
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private Context mContext;
    private ProgressBar mProgressBar;
    private EditText mEmail;
    private EditText mPassword;
    private TextView mSigningIn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(TAG, "onCreate: started login");
        mProgressBar = (ProgressBar) findViewById(R.id.progressbarLogin);
        mSigningIn = (TextView) findViewById(R.id.textviewLoginSigningIn);
        mEmail = (EditText) findViewById((R.id.edittextLoginEmail));
        mPassword = (EditText) findViewById((R.id.edittextLoginPassword));
        mContext = LoginActivity.this;

        // This is invisible till user signs in
        mProgressBar.setVisibility(View.GONE);
        mSigningIn.setVisibility(View.GONE);

        setupFirebaseAuth();
        initialiseLoggingIn();
    }


    // method to check if input fields are not null
    private boolean checkIfStringNull(String input){
        Log.d(TAG, "checkIfStringNull: checking if fields are null");

        if(input.equals("")){
            Log.d(TAG, "checkIfStringNull: fields are null");
            return true;
        } else {
            Log.d(TAG, "checkIfStringNull: fields filled");
            return false;
        }
    }


    //------------------------FIRESBASE STUFF------------
    // Button which initialises lgging in
    private void initialiseLoggingIn(){
        Button loginButton = (Button) findViewById(R.id.buttonLogin);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Attempting to lig in");

                String email = mEmail.getText().toString();
                String password = mPassword.getText().toString();

                if(checkIfStringNull(email) || checkIfStringNull(password)){
                    Toast.makeText(mContext, "You must fill out all the fields", Toast.LENGTH_SHORT).show();
                } else {
                    // If Fields are not empty, attempt a log in and progress bar shows
                    mProgressBar.setVisibility(View.VISIBLE);
                    mSigningIn.setVisibility(View.VISIBLE);

                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    // If sign in fails, display a message to the user. If sign in succeeds
                                    // the auth state listener will be notified and logic to handle the
                                    // signed in user can be handled in the listener.
                                    if (!task.isSuccessful()) {
                                        Log.w(TAG, "signInWithEmail:failed", task.getException());

                                        Toast.makeText(LoginActivity.this, getString(R.string.failed_auth),
                                                Toast.LENGTH_SHORT).show();
                                        mProgressBar.setVisibility(View.GONE);
                                        mSigningIn.setVisibility(View.GONE);
                                    }
                                    else{
                                        //try{
                                           // if(user.isEmailVerified()){
                                               // Log.d(TAG, "onComplete: success. email is verified.");
                                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                                startActivity(intent);
                                           // }else{
                                              //  Toast.makeText(mContext, "Email is not verified \n check your email inbox.", Toast.LENGTH_SHORT).show();
                                                mProgressBar.setVisibility(View.GONE);
                                                mSigningIn.setVisibility(View.GONE);
                                               // mAuth.signOut();
                                          //  }
                                        //}catch (NullPointerException e){
                                           // Log.e(TAG, "onComplete: NullPointerException: " + e.getMessage() );
                                       // }
                                    }

                                    // ...
                                }
                            });
                }

                   /* mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() { // Gives error if you don't use Login.this
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    if (task.isSuccessful()) {
                                        // Sign in success, update UI with the signed-in user's information
                                        Log.d(TAG, "signInWithEmail:success");
                                        try{
                                            // Checks if user has verified email before signing in
                                            if(user.isEmailVerified()){
                                                Log.d(TAG, "onComplete: email is verified");
                                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                                startActivity(intent);
                                            } else {
                                                Log.d(TAG, "onComplete: ");
                                                Toast.makeText(LoginActivity.this, "Email not verified \n check email inbox",
                                                        Toast.LENGTH_SHORT).show();
                                                mProgressBar.setVisibility(View.GONE);
                                                mSigningIn.setVisibility(View.GONE);
                                                mAuth.signOut();
                                            }
                                        } catch (NullPointerException e){
                                            Log.e(TAG, "onComplete: NullPointerEception: " + e.getMessage() );
                                        }
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                                        Toast.makeText(LoginActivity.this, getString((R.string.failed_auth)),
                                                Toast.LENGTH_SHORT).show();
                                        mProgressBar.setVisibility(View.GONE);
                                        mSigningIn.setVisibility(View.GONE);
                                    }

                                    // ...
                                }
                            });
                }*/
            }
        });
        TextView signUpLink = (TextView) findViewById(R.id.textviewRegisterAlreadyHaveAccount);
        signUpLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: going to register activity");
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        // navigates to home activity if user is logged in
        if(mAuth.getCurrentUser() != null){
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            // closes login activity
            finish();
        }
    }

    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: firbase auth is being setup");
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if(user != null){
                    Log.d(TAG, "onAuthStateChanged: user signed in " + user);
                } else {
                    Log.d(TAG, "onAuthStateChanged: user signed out");
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mAuthListener != null){
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}