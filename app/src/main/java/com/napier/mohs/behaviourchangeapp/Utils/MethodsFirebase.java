package com.napier.mohs.behaviourchangeapp.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.napier.mohs.behaviourchangeapp.Home.ActivityHome;
import com.napier.mohs.behaviourchangeapp.Models.AccountSettings;
import com.napier.mohs.behaviourchangeapp.Models.Exercise;
import com.napier.mohs.behaviourchangeapp.Models.Goal;
import com.napier.mohs.behaviourchangeapp.Models.User;
import com.napier.mohs.behaviourchangeapp.Models.Photo;
import com.napier.mohs.behaviourchangeapp.Models.UserAndAccountSettings;
import com.napier.mohs.behaviourchangeapp.Profile.ActivityAccountSettings;
import com.napier.mohs.behaviourchangeapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import es.dmoral.toasty.Toasty;

/**
 * Created by Mohs on 17/03/2018.
 */

public class MethodsFirebase {
    private static final String TAG = "MethodsFirebase";
    private Context mContext;
    private double mPhotoProgressUpload = 0;

    // Firebase Stuff
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myDBRefFirebase;
    private StorageReference mStorageRefFirebase;

    private String userID;

    private int IMG_QUALITY = 100;// quality of bitmap image converted to bytes
    public String IMAGE_FIREBASE_STORAGE = "photos/users/"; // Directory for where images are stored in firebase storage

    public MethodsFirebase(Context context) {
        Log.d(TAG, "setupFirebaseAuth: firebase auth is being setup");
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myDBRefFirebase = mFirebaseDatabase.getReference();
        mStorageRefFirebase = FirebaseStorage.getInstance().getReference();
        mContext = context;


        if (mAuth.getCurrentUser() != null) {
            userID = mAuth.getCurrentUser().getUid();
        }
    }

    // returns number of images for a user
    public int getImgCount(DataSnapshot dataSnapshot) {
        int imgCount = 0;

        // targeting specific node so loop is faster
        for (DataSnapshot ds : dataSnapshot.child(mContext
                .getString(R.string.db_name_user_photos))
                .child(userID).getChildren()) {
            imgCount++;
        }
        return imgCount;
    }

    // void because firebase auto does async task in background when it uploads images to storage
    public void uploadPhotoNew(String photoType, final String caption,
                               final int count, final String imgURL, Bitmap bitmap) {
        Log.d(TAG, "uploadPhotoNew: uploading new photo attempt");


        // either new photo or profile photo
        // if loop for new photo
        if (photoType.equals(mContext.getString(R.string.new_photo))) {
            Log.d(TAG, "uploadPhotoNew: new photo being uploaded");

            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid(); // instead of global using this local

            // gets image count and adds '1' to 'photo' and sets as image name
            StorageReference storageReference = mStorageRefFirebase
                    .child(IMAGE_FIREBASE_STORAGE + "/" + user_id + "/photo" + (count + 1));

            // Converts img url to bitmap
            if (bitmap == null) {
                bitmap = ManageImages.retrieveBitmap(imgURL);
            }

            byte[] bytes = ManageImages.retrieveBitmapBytes(bitmap, IMG_QUALITY);

            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(bytes);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                //when image upload is successful
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    // get download photo url in storage location in firebase
                    Uri firebaseURL = taskSnapshot.getDownloadUrl();

                    Toasty.success(mContext, "Upload was successful", Toast.LENGTH_SHORT).show();

                    // add to pointers in firebase database
                    // add new photo to 'photos' and 'user_photos' nodes
                    addPhotoDatabase(caption, firebaseURL.toString());

                    // nav to main feed of app where user can see their photo
                    Intent intent = new Intent(mContext, ActivityHome.class);
                    mContext.startActivity(intent);

                }
            }).addOnFailureListener(new OnFailureListener() {
                // when image upload is failure
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: Upload failed");
                    Toasty.error(mContext, "Upload failed", Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                // constantly updates as we go
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount(); // 100 for 100%, works out percent of upload left

                    //if loop to prevent too much data being displayed
                    // toast won't print unless new progress is 15 higher than old
                    if (progress - 15 > mPhotoProgressUpload) {
                        // formatted toast string so only whole number displayed instead of decimals
                        Toast.makeText(mContext, "upload progress: " + String.format("%.0f", progress) + "%", Toast.LENGTH_SHORT).show();
                        mPhotoProgressUpload = progress;
                    }
                    Log.d(TAG, "onProgress: progress of upload: " + progress + "%");
                }
            });
        }
        // else profile pic is being uploaded
        else if (photoType.equals(mContext.getString(R.string.profile_photo))) {
            Log.d(TAG, "uploadPhotoNew: new profile photo being uploaded");


            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid(); // instead of global using this local

            StorageReference storageReference = mStorageRefFirebase
                    .child(IMAGE_FIREBASE_STORAGE + "/" + user_id + "/profile_photo"); // removed count as there is only single photo being uploaded

            // Converts img url to bitmap
            if (bitmap == null) {
                bitmap = ManageImages.retrieveBitmap(imgURL);
            }
            byte[] bytes = ManageImages.retrieveBitmapBytes(bitmap, IMG_QUALITY);

            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(bytes);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                //when image upload is successful
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    // get download photo url in storage location in firebase
                    Uri firebaseURL = taskSnapshot.getDownloadUrl();

                    Toasty.success(mContext, "Upload was successful", Toast.LENGTH_SHORT).show();

                    // add to pointers in firebase database
                    // add new photo to 'user_account_settings' nodes
                    setupProfilePhoto(firebaseURL.toString());

                    // sets viewpager so returns us back to edit profile fragment
                    // opens up edit profile fragment and skips showing account settings activity
                    ((ActivityAccountSettings) mContext).setupViewPagerFragment(
                            ((ActivityAccountSettings) mContext).adapter
                                    .getFragmentNumber(mContext.getString(R.string.fragment_edit_profile))
                    );

                }
            }).addOnFailureListener(new OnFailureListener() {
                // when image upload is failure
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: Upload failed");
                    Toasty.warning(mContext, "Upload failed", Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                // constantly updates as we go
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount(); // 100 for 100%, works out percent of upload left

                    //if loop to prevent too much data being displayed
                    // toast won't print unless new progress is 15 higher than old
                    if (progress - 15 > mPhotoProgressUpload) {
                        // formatted toast string so only whole number displayed instead of decimals
                        Toast.makeText(mContext, "upload progress: " + String.format("%.0f", progress) + "%", Toast.LENGTH_SHORT).show();
                        mPhotoProgressUpload = progress;
                    }
                    Log.d(TAG, "onProgress: progress of upload: " + progress + "%");
                }
            });


        }

    }

    // setting single parameter  to change profile photo and uploads to db
    private void setupProfilePhoto(String url) {
        Log.d(TAG, "setupProfilePhoto: new profile photo being set " + url);

        myDBRefFirebase.child(mContext.getString(R.string.db_name_user_account_settings))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(mContext.getString(R.string.profile_photo))
                .setValue(url);
    }

    // gets a time stamp of when photo is uploaded
    private String timeStampGet() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        return simpleDateFormat.format(new Date());  // returns formatted date in London timezone
    }

    // adds image to firebase db
    private void addPhotoDatabase(String caption, String url) {
        Log.d(TAG, "addPhotoDatabase: photo being added to database");
        String tags = ManipulateStrings.retrieveTags(caption); // tags set here to null initially;

        // each photo has unique id
        String photoIdKey = myDBRefFirebase.child(mContext.getString(R.string.db_name_photos)).push().getKey(); // random string
        Photo photo = new Photo();
        photo.setCaption(caption);
        photo.setDate_created(timeStampGet());
        photo.setImage_path(url);
        photo.setTags(tags);
        photo.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());
        photo.setPhoto_id(photoIdKey);

        // database insertion
        myDBRefFirebase.child(mContext.getString(R.string.db_name_user_photos))
                .child(FirebaseAuth.getInstance()
                        .getCurrentUser().getUid())
                .child(photoIdKey).setValue(photo);
        myDBRefFirebase.child(mContext.getString(R.string.db_name_photos)).child(photoIdKey).setValue(photo);
    }

    // adds goal to firebase db
    public void addGoalDatabase(String name, String weight, String current) {
        Log.d(TAG, "exerciseAddToDatabase: goals being added to database");

        // each new goal has unique id generated, push() writes data to database
        String goalIdKey = myDBRefFirebase.child(mContext.getString(R.string.db_name_goals)).push().getKey(); // random string, push means write to db

        Goal goals = new Goal();
        goals.setGoal_name(name);
        goals.setGoal_weight(weight);
        goals.setCurrent_weight(current);
        goals.setGoal_id(goalIdKey);

        //database insertion
        myDBRefFirebase.child(mContext.getString(R.string.db_name_goals))
                .child(FirebaseAuth.getInstance()
                        .getCurrentUser().getUid())
                .child(goalIdKey)
                .setValue(goals);

    }

    // adds updates goal to firebase db
    public void updateGoalDatabase(String goal_id, String name, String weight, String current) {
        Log.d(TAG, "updateGoalDatabase: goal is being updated");
        Goal goals = new Goal();
        goals.setGoal_name(name);
        goals.setGoal_weight(weight);
        goals.setCurrent_weight(current);
        goals.setGoal_id(goal_id);

        //database insertion
        myDBRefFirebase.child(mContext.getString(R.string.db_name_goals))
                .child(FirebaseAuth.getInstance()
                        .getCurrentUser().getUid())
                .child(goal_id)
                .setValue(goals);

    }
    // adds exercise to firebase db
    public void exerciseAddToDatabase(String date, String name, String weight, String reps) {
        Log.d(TAG, "exerciseAddToDatabase: exercise being added to database");

        // each exercise has unique id
        String exerciseNewKey = myDBRefFirebase.child(mContext.getString(R.string.db_name_exercises)).push().getKey(); // random string
        Exercise exercise = new Exercise();
        exercise.setExercise_name(name);
        exercise.setExercise_weight(weight);
        exercise.setExercise_reps(reps);
        exercise.setExercise_id(exerciseNewKey);

         //database insertion
        myDBRefFirebase.child(mContext.getString(R.string.db_name_exercises))
                .child(FirebaseAuth.getInstance()
                        .getCurrentUser().getUid())
                .child(date)
                .child(exerciseNewKey)
                .setValue(exercise);

        // adding current best to database
        myDBRefFirebase.child(mContext.getString(R.string.db_name_exercises))
                .child(FirebaseAuth.getInstance()
                        .getCurrentUser().getUid())
                .child("current_best")
                .child(name)
                .child(exerciseNewKey)
                //.child("weight")
                .setValue(exercise);

    }

    // updates exercise to firebase db
    public void exerciseUpdateDatabase(String exercise_id, String date, String name, String weight, String reps) {
        Log.d(TAG, "exerciseAddToDatabase: exercise being added to database");

        Exercise exercise = new Exercise();
        exercise.setExercise_name(name);
        exercise.setExercise_weight(weight);
        exercise.setExercise_reps(reps);
        exercise.setExercise_id(exercise_id);

        //database insertion
        myDBRefFirebase.child(mContext.getString(R.string.db_name_exercises))
                .child(FirebaseAuth.getInstance()
                        .getCurrentUser().getUid())
                .child(date)
                .child(exercise_id)
                .setValue(exercise);

    }

    // checks highest record so far for exercise
    public void exerciseCurrentBest(String name){

        Query query = myDBRefFirebase.child(mContext.getString(R.string.db_name_exercises))
                .child(FirebaseAuth.getInstance()
                        .getCurrentUser().getUid())
                .child("current_best")
                .child(name)
                .orderByChild("exercise_weight");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){
                for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                    String Key = childSnapshot.child("exercise_weight").getValue().toString();

                   // Toasty.normal(mContext,"Current Best " + Key,Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Current Best " + Key);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                throw databaseError.toException(); // don't swallow errors
            }
        });
    }

    // gets a time stamp in YYYY/MM/DD
    private String dateGet() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Log.d(TAG, "timeStampGet: " + simpleDateFormat.format(new Date()));
        return simpleDateFormat.format(new Date());  // returns formatted date in London timezone
    }

    // registers the given email and password to firebase db
    public void registerEmail(final String email, String password, final String username) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() { //Because not in activty can not apply context here
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information

                            // send verif email
                            sendEmailVer();
                            userID = mAuth.getCurrentUser().getUid();
                            Log.d(TAG, "createUserWithEmail:success, userID: " + userID);
                            Toasty.success(mContext, "Successfully Authenticated! \n\t\t\t\tPlease Sign In",
                                    Toast.LENGTH_SHORT).show();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toasty.error(mContext, "Failed To Authenticate! " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });

    }

    // sends verification email to user
    // took this out to allow for testing user accounts
    public void sendEmailVer() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                            } else
                                Toasty.error(mContext, "Verification email could not be sent", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }


    // this will add a user to the firebase db
    // it will include things such as website and profile pic
    // and user setting
    public void addUserDatabase(String email, String username, String description, String website, String profile_photo) {

        // Creates new user and adds to db
        // Removes spaces and adds periods to make usernames
        User user = new User(userID, 1, email, ManipulateStrings.usernameRemoveSpace(username));

        // call database ref to look for child node users, look for child node user_id and add data to db
        myDBRefFirebase.child(mContext.getString(R.string.db_name_users))
                .child(userID)
                .setValue(user);

        Log.d(TAG, "addUserDatabase: username: " + user);

        // sets up user_account_settings
        AccountSettings userAccSettings = new AccountSettings(
                description,
                username,
                0,
                0,
                0,
                profile_photo,
                ManipulateStrings.usernameRemoveSpace(username),
                website,
                userID
        );
        Log.d(TAG, "addUserDatabase: userAccSettings: " + userAccSettings);
        myDBRefFirebase.child(mContext.getString(R.string.db_name_user_account_settings))
                .child(userID)
                .setValue(userAccSettings);

    }

    // Updates the users username in firebase db
    public void updateUsernameDatabase(String username) {
        Log.d(TAG, "updateUsernameDatabase: updating the users username to: " + username);

        // updates users node
        myDBRefFirebase.child(mContext.getString(R.string.db_name_users))
                .child(userID)
                .child(mContext.getString(R.string.username_field))
                .setValue(username);

        // updates user_account_settings node
        myDBRefFirebase.child(mContext.getString(R.string.db_name_user_account_settings))
                .child(userID)
                .child(mContext.getString(R.string.username_field))
                .setValue(username);
    }

    // Updates the users email in users node firebase db
    public void updateEmailDatabase(String email) {
        Log.d(TAG, "updateUsernameDatabase: updating the users email to: " + email);

        myDBRefFirebase.child(mContext.getString(R.string.db_name_users))
                .child(userID)
                .child(mContext.getString(R.string.email_field))
                .setValue(email);
    }

    // updates other settings apart from email and username
    // These do not haave to be unique
    public void updateAccountSettingsDatabase(String display_name, String web_url, String profile_description, long phone_number) {
        Log.d(TAG, "updateUsernameDatabase: updating the users settings, displayname: " + display_name + " web: " + web_url
                + " phone: " + phone_number + " decription: " + profile_description);

        if (display_name != null) {
            myDBRefFirebase.child(mContext.getString(R.string.db_name_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.display_name_field))
                    .setValue(display_name);
        }

        if (web_url != null) {
            myDBRefFirebase.child(mContext.getString(R.string.db_name_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.website_field))
                    .setValue(web_url);
        }

        if (profile_description != null) {
            myDBRefFirebase.child(mContext.getString(R.string.db_name_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.description_field))
                    .setValue(profile_description);
        }

        if (phone_number != 0) {
            myDBRefFirebase.child(mContext.getString(R.string.db_name_users))
                    .child(userID)
                    .child(mContext.getString(R.string.phone_number_field))
                    .setValue(phone_number);
        }

    }

    // gets user account settings from firebase db for user logged in
    public UserAndAccountSettings getUserAndAccountSettings(DataSnapshot dataSnapshot) {
        Log.d(TAG, "getUserAccountSettings: getting user acc settings from firebase");

        AccountSettings accountSettings = new AccountSettings();
        User user = new User();

        // loops through all main parent nodes
        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            // this if statement handle user_account_settings
            if (ds.getKey().equals(mContext.getString(R.string.db_name_user_account_settings))) {// if key equals user_account_settings look in that node
                Log.d(TAG, "getUserAccountSettings: DataSnapshot: " + ds); // used for ddebugging

                try {
                    accountSettings.setDisplay_name(
                            ds.child(userID)
                                    .getValue(AccountSettings.class)
                                    .getDisplay_name()
                    );
                    accountSettings.setUsername(
                            ds.child(userID)
                                    .getValue(AccountSettings.class)
                                    .getUsername()
                    );
                    accountSettings.setWebsite(
                            ds.child(userID)
                                    .getValue(AccountSettings.class)
                                    .getWebsite()
                    );
                    accountSettings.setDescription(
                            ds.child(userID)
                                    .getValue(AccountSettings.class)
                                    .getDescription()
                    );
                    accountSettings.setProfile_photo(
                            ds.child(userID)
                                    .getValue(AccountSettings.class)
                                    .getProfile_photo()
                    );
                    accountSettings.setPosts(
                            ds.child(userID)
                                    .getValue(AccountSettings.class)
                                    .getPosts()
                    );
                    accountSettings.setFollowing(
                            ds.child(userID)
                                    .getValue(AccountSettings.class)
                                    .getFollowing()
                    );
                    accountSettings.setFollowers(
                            ds.child(userID)
                                    .getValue(AccountSettings.class)
                                    .getFollowers()
                    );
                    Log.d(TAG, "getUserAccountSettings: from user_account_settings: " + accountSettings.toString());
                } catch (NullPointerException e) {
                    Log.e(TAG, "getUserAccountSettings: NullPointerException: " + e.getMessage());
                }


            }

            if (ds.getKey().equals(mContext.getString(R.string.db_name_users))) {
                Log.d(TAG, "getUserAccountSettings: datasnapshot: " + ds);

                user.setUsername(
                        ds.child(userID)
                                .getValue(User.class)
                                .getUsername()
                );
                user.setEmail(
                        ds.child(userID)
                                .getValue(User.class)
                                .getEmail()
                );
                user.setPhone_number(
                        ds.child(userID)
                                .getValue(User.class)
                                .getPhone_number()
                );
                user.setUser_id(
                        ds.child(userID)
                                .getValue(User.class)
                                .getUser_id()
                );

                Log.d(TAG, "getUserAccountSettings: from users: " + user.toString());
            }
        }
        return new UserAndAccountSettings(user, accountSettings); // returns custom data model
    }
}
