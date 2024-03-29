package com.napier.mohs.behaviourchangeapp.Profile;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.napier.mohs.behaviourchangeapp.Dialogs.DialogEmailChange;
import com.napier.mohs.behaviourchangeapp.Models.AccountSettings;
import com.napier.mohs.behaviourchangeapp.Models.User;
import com.napier.mohs.behaviourchangeapp.Models.UserAndAccountSettings;
import com.napier.mohs.behaviourchangeapp.R;
import com.napier.mohs.behaviourchangeapp.Share.ActivityShare;
import com.napier.mohs.behaviourchangeapp.Utils.MethodsFirebase;
import com.napier.mohs.behaviourchangeapp.Utils.SettingsUniversalImageLoader;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import es.dmoral.toasty.Toasty;

/**
 * Created by User on 6/4/2017.
 */

public class FragmentEditProfile extends Fragment implements DialogEmailChange.OnEmailConfirmListener{

    private static final String TAG = "FragmentEditProfile";

    // Firebase Stuff
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myDBRefFirebase;
    private MethodsFirebase mMethodsFirebase;

    @BindView(R.id.edittextEditDisplayName) EditText mDisplayName;
    @BindView(R.id.edittextEditUsername) EditText mUsername;
    @BindView(R.id.edittextEditWebsite) EditText mWebsite;
    @BindView(R.id.edittextEditDescription) EditText mDescription;
    @BindView(R.id.edittextEditEmail) EditText mEmail;
    @BindView(R.id.edittextEditPhoneNumber) EditText mPhoneNumber;
    @BindView(R.id.textviewEditChangeProfilePhoto) TextView mChangeProfilePhoto;
    @BindView(R.id.imageEditProfilePhoto) CircleImageView mProfilePhoto;
    @BindView(R.id.imageEditBackArrow) ImageView backArrow;
    @BindView(R.id.imageEditSaveChange) ImageView save;

    private UserAndAccountSettings mUserAndAccountSettings;
    private String userID;


    // override method for password confirm dialog
    // this appears only if user changes their email
    @Override
    public void onEmailConfirm(String password) {
        Log.d(TAG, "onPasswordConfirm: password entered: " + password);

        // Get auth credentials from the user for re-authentication. The example below shows
        // email and password credentials but there are multiple possible providers,
        // such as GoogleAuthProvider or FacebookAuthProvider.
        AuthCredential credential = EmailAuthProvider
                .getCredential(mAuth.getCurrentUser().getEmail(),password);

        // user prompted to reenter sign in credentials
        mAuth.getCurrentUser().reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                         if(task.isSuccessful()){
                            Log.d(TAG, "User re-authenticated.");
                            
                            // check email already exists
                             mAuth.fetchProvidersForEmail(mEmail.getText().toString()).addOnCompleteListener(new OnCompleteListener<ProviderQueryResult>() {
                                 @Override
                                 public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                                     if(task.isSuccessful()){
                                         try{
                                         // if size eqauls 1 we have retrieved something
                                         if(task.getResult().getProviders().size() == 1){
                                             Log.d(TAG, "onComplete: Email already taken");
                                             Toast.makeText(getActivity(), "That email is already being used by someone else", Toast.LENGTH_SHORT).show();
                                         }
                                         // if null, email is free to use
                                         else{
                                             Log.d(TAG, "onComplete: email is available ");
                                             // email is updated
                                             mAuth.getCurrentUser().updateEmail(mEmail.getText().toString())
                                                     .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                         @Override
                                                         public void onComplete(@NonNull Task<Void> task) {
                                                             if (task.isSuccessful()) {
                                                                 Log.d(TAG, "User email address is updated.");
                                                                 Toast.makeText(getActivity(), "Email has been updated successfully", Toast.LENGTH_SHORT).show();
                                                                 mMethodsFirebase.updateEmailDatabase(mEmail.getText().toString());
                                                             }
                                                         }});
                                         }
                                         }
                                         catch (NullPointerException e){
                                             Log.e(TAG, "onComplete: NullPointerException: " + e.getMessage() );
                                         }
                                     }
                                 }
                             });
                             
                        } else {
                             Log.d(TAG, "onComplete: Failed to reauthenticate");
                        }
                    }
                });

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_editprofile, container, false);
        ButterKnife.bind(this, view);
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myDBRefFirebase = mFirebaseDatabase.getReference();
        mMethodsFirebase = new MethodsFirebase(getActivity());

        setupFirebaseAuth();

        // back arrow which goes to profile activity
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: going back to profile activity. ");
                getActivity().finish(); // Have to use getActivity().finish(); as we are in fragment
            }
        });

        // saves details changed in edit profile
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: saving changes attempt ");
                saveEditSettings();
                Toasty.success(getActivity(),"Successfully Saved!", Toast.LENGTH_SHORT).show();
                //getActivity().finish(); // Have to use getActivity().finish(); as we are in fragment
            }
        });


        return view;

    }

    // gets data from widgets and uploads to db
    // Also checks that username is unique
    private void saveEditSettings() {
        final String displayname = mDisplayName.getText().toString();
        final String username = mUsername.getText().toString();
        final String web = mWebsite.getText().toString();
        final String description = mDescription.getText().toString();
        final String email = mEmail.getText().toString();
        final long phone = Long.parseLong(mPhoneNumber.getText().toString());

        // using query instead which checks username entered into text field and compare it to what was originally loaded into fragment
        Log.d(TAG, "onDataChange: current username is: " + mUserAndAccountSettings.getUser().getUsername());

        // if username has nchanged
        if (!mUserAndAccountSettings.getUser().getUsername().equals(username)) {
            checkUsernameExist(username);
        }

        //if email is changed
        if (!mUserAndAccountSettings.getUser().getEmail().equals(email)) {

            // first reauthenticate email (only needed is emails have to be verified
            // check email is registered already
            // then email is changed
            DialogEmailChange dialog = new DialogEmailChange();
            dialog.show(getFragmentManager(), getString(R.string.dialog_password_confirm));
            dialog.setTargetFragment(FragmentEditProfile.this, 1); // sets this as target fragment after dialog is opened

        }

        // if displayname is changed
        if(!mUserAndAccountSettings.getAccountsettings().getDisplay_name().equals(displayname)){
            mMethodsFirebase.updateAccountSettingsDatabase(displayname, null, null, 0);
        }

        if(!mUserAndAccountSettings.getAccountsettings().getWebsite().equals(web)){
            mMethodsFirebase.updateAccountSettingsDatabase(null, web, null, 0);
        }

        if(!mUserAndAccountSettings.getAccountsettings().getDescription().equals(description)){
            mMethodsFirebase.updateAccountSettingsDatabase(null, null, description, 0);
        }

    }



    // checks if username is in db
    // using query means cant return anything (e.g. boolean if usename already exists)
    // Forced to run this and execute methods inside the override
    private void checkUsernameExist(final String username) {
        Log.d(TAG, "checkUsernameExist: Checking if this username exists already: " + username);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        // looks for node that contains object that is being lookexd for then gets field in that object
        Query qry = ref
                .child(getString(R.string.db_name_users))
                .orderByChild(getString(R.string.username_field))
                .equalTo(username);
        qry.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // only returns datasnapshot if match is foound
                if (!dataSnapshot.exists()) {
                    // username added
                    mMethodsFirebase.updateUsernameDatabase(username);
                    Toast.makeText(getActivity(), "Username changed.", Toast.LENGTH_SHORT).show();
                }
                //loops through results
                // single snapshot as only one item from db is being returned
                for (DataSnapshot singleDataSnapshot : dataSnapshot.getChildren()) {
                    if (singleDataSnapshot.exists()) {
                        Log.d(TAG, "onDataChange: username already exists in db: " + singleDataSnapshot.getValue(User.class).getUsername());
                        Toast.makeText(getActivity(), "Username already exists.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // sets up the edit profile page with data from db
    private void setupWidgets(UserAndAccountSettings userAndAccountSettings) {
        Log.d(TAG, "setupWidgets: settings up edit profile widgets with data from firebase db ");

        // when activity starts this user settings object is set
        mUserAndAccountSettings = userAndAccountSettings;

        User user = userAndAccountSettings.getUser();
        AccountSettings accountSettings = userAndAccountSettings.getAccountsettings();

        SettingsUniversalImageLoader.setImage(accountSettings.getProfile_photo(), mProfilePhoto, null, ""); // image loader for profile photo

        // sets up widgets with db data
        mDisplayName.setText(accountSettings.getDisplay_name());
        mUsername.setText(accountSettings.getUsername());
        mWebsite.setText(accountSettings.getWebsite());
        mDescription.setText(accountSettings.getDescription());
        mEmail.setText(String.valueOf(user.getEmail()));
        mPhoneNumber.setText(String.valueOf(user.getPhone_number()));

        // when changing profile photo we are navigating to share page to access photos from gallery
        mChangeProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: profile photo is being changed from edit page ");
                Intent intent = new Intent(getActivity(), ActivityShare.class);
                // setting a flag to differentiate between sharing photos and changing profile picture
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                // finish edit profile fragment when opening gallery fragment
                getActivity().finish();
            }
        });
    }


    //------------------------FIREBASE STUFF-------------
    // Method to check if a user is signed in app

    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: firebase auth is being setup");
        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
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
        // This is always listening
        myDBRefFirebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // retrieves the user info from db
                setupWidgets(mMethodsFirebase.getUserAndAccountSettings(dataSnapshot)); // retrieves datasnapshot of user settings and sets up widgets
                // retrieves images for the user
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
