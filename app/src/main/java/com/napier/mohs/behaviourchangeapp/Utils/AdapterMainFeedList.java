package com.napier.mohs.behaviourchangeapp.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.napier.mohs.behaviourchangeapp.Home.ActivityHome;
import com.napier.mohs.behaviourchangeapp.Models.AccountSettings;
import com.napier.mohs.behaviourchangeapp.Models.Comment;
import com.napier.mohs.behaviourchangeapp.Models.Like;
import com.napier.mohs.behaviourchangeapp.Models.Photo;
import com.napier.mohs.behaviourchangeapp.Models.User;
import com.napier.mohs.behaviourchangeapp.Profile.ActivityProfile;
import com.napier.mohs.behaviourchangeapp.R;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Mohs on 21/03/2018.
 */

public class AdapterMainFeedList extends ArrayAdapter<Photo> {
    private static final String TAG = "AdapterMainFeedList";

    private LayoutInflater mLayoutInflater;
    private int mLayoutResource;
    private Context mContext;
    private DatabaseReference mDatabaseReference;
    private String currentUsername = "";

    public AdapterMainFeedList(@NonNull Context context, int resource, @NonNull List<Photo> objects) {
        super(context, resource, objects);
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayoutResource = resource;
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        this.mContext = context;
    }

    // for pagination of posts on main feed when user scrolls through posts
    public interface OnPostsLoadMoreListener {
        void onPostsLoadMore();
    }
    OnPostsLoadMoreListener mOnPostsLoadMoreListener;

    static class ViewHolder{

        @BindView(R.id.textviewMainFeedComments) TextView comments;
        @BindView(R.id.textviewMainFeedUsername) TextView username;
        @BindView(R.id.textviewMainFeedTimeStamp) TextView timestamp;
        @BindView(R.id.textviewMainFeedCaption) TextView caption;
        @BindView(R.id.textviewMainFeedLikes) TextView likes;
        @BindView(R.id.imageMainFeedPostPicture) ImagesSquaredView imagePost;
        @BindView(R.id.imageMainFeedProfile) CircleImageView imageProfile;
        @BindView(R.id.imageMainFeedStarYellow) ImageView yellowStar;
        @BindView(R.id.imageMainFeedStarHollow) ImageView hollowStar;
        @BindView(R.id.imageMainFeedSpeechBubble) ImageView speechBubble;
        @BindView(R.id.progressbarMainFeed) ProgressBar mProgressBar;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view); // Butterknife For ViewHolder Pattern
        }

        // we are saving specific things for each post in this view holder
        AccountSettings mAccountSettings = new AccountSettings();
        User mUser = new User();
        StringBuilder stringBuilderUsers;
        String stringLikes;
        boolean likedByCurrentUser;
        Star star;
        GestureDetector mGestureDetector;
        Photo mPhoto;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final ViewHolder viewHolder;

        if(convertView == null){
            convertView = mLayoutInflater.inflate(mLayoutResource, parent, false);
            viewHolder = new ViewHolder(convertView);

            viewHolder.star = new Star(viewHolder.yellowStar, viewHolder.hollowStar);
            viewHolder.mPhoto = getItem(position);
            viewHolder.mGestureDetector = new GestureDetector(mContext, new GestureListener(viewHolder));
            viewHolder.stringBuilderUsers = new StringBuilder();

            // set tag  on the convert view
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // retrieves  current users username for checking likes string
        retrieveUsernameCurrent();
        getLikesAsString(viewHolder);

        // list for all comments of photo
        List<Comment> commentsList = getItem(position).getComments();
        viewHolder.comments.setText("View all " + commentsList.size() + " comments"); // sets comment text field with how many comments there are for photo

        // when we press comments text field we start intent to comments
        viewHolder.comments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: opening comment thread for photo: " + getItem(position).getPhoto_id());
                // begins wrapping bundle with photo at position and user account settings particular to that view
                ((ActivityHome)mContext).onSelectedCommentThread(getItem(position),
                        mContext.getString(R.string.calling_activity));

                // hide the layout when we go to comments thread so view pager is hidden want to display frame layout of comments
                ((ActivityHome)mContext).layoutHomeHide();
            }
        });

        // sets timestamp for photo
        String differenceTimeStamp = getDateTimeStampDifference(getItem(position));
        if (!differenceTimeStamp.equals("0")){
            viewHolder.timestamp.setText((differenceTimeStamp + " DAYS AGO")); // if there is a difference in timestamp
        } else {
            viewHolder.timestamp.setText("TODAY");
        }

        // sets caption of photo
        String stringCaption = getItem(position).getCaption();
        viewHolder.caption.setText(stringCaption);

        // sets profile image
        final ImageLoader imageLoader = ImageLoader.getInstance();
       // imageLoader.displayImage(getItem(position).getImage_path(), viewHolder.imagePost);

        imageLoader.displayImage(getItem(position).getImage_path(), viewHolder.imagePost, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageURL, View view) {
                if(viewHolder.mProgressBar != null){
                    viewHolder.mProgressBar.setVisibility(View.VISIBLE); // This is when loading has started so want progress bar visible
                }
            }

            @Override
            public void onLoadingFailed(String imageURL, View view, FailReason failReason) {
                if(viewHolder.mProgressBar != null){
                    viewHolder.mProgressBar.setVisibility(View.GONE); // This is when loading has failed so want progress bar invisible
                }
            }

            @Override
            public void onLoadingComplete(String imageURL, View view, Bitmap loadedImage) {
                if(viewHolder.mProgressBar != null){
                    viewHolder.mProgressBar.setVisibility(View.GONE); // This is when loading has completed so want progress bar invisible
                }
            }

            @Override
            public void onLoadingCancelled(String imageURL, View view) {
                if(viewHolder.mProgressBar != null){
                    viewHolder.mProgressBar.setVisibility(View.GONE); // This is when loading has canceled so want progress bar invisible
                }
            }
        });

        // gets username and profile image for the post
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        // points to user id and retrieves their photos instead of going through db of all  photos
        Query query = databaseReference
                .child(mContext.getString(R.string.db_name_user_account_settings)) // looks in user_account_settings node
                .orderByChild(mContext.getString(R.string.user_id_field)) // looks for user_id field
                .equalTo(getItem(position).getUser_id()); // gets photo and user id attached to the phoot

        Log.d(TAG, "getPostDetails: query: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener(){


            // retrieve details
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleDataSnapshot : dataSnapshot.getChildren()){
                    Log.d(TAG, "onDataChange: found user: " + singleDataSnapshot.getValue(AccountSettings.class).getUsername() );

                    // sets username of post owner in text field in view holder
                    viewHolder.username.setText(singleDataSnapshot.getValue(AccountSettings.class).getUsername());
                    // on click listener so we can navigate to that users profile
                    viewHolder.username.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "onClick: going to the profile of user: " + viewHolder.username);
                            Intent intent = new Intent(mContext, ActivityProfile.class); // intent to nav to profile activity
                            intent.putExtra(mContext.getString(R.string.calling_activity),
                                    mContext.getString(R.string.home_activity));
                            intent.putExtra(mContext.getString(R.string.user_extra), viewHolder.mUser); // another extra for passing user object
                            mContext.startActivity(intent);
                        }
                    });


                    //sets profile pic of post owner
                    final ImageLoader imageLoader = ImageLoader.getInstance();
                    imageLoader.displayImage(singleDataSnapshot.getValue(AccountSettings.class).getProfile_photo(), viewHolder.imageProfile);
                    // on click listener to navigate to users profile when their picture is clicked
                    viewHolder.imageProfile.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "onClick: going to the profile of user: " + viewHolder.username);
                            Intent intent = new Intent(mContext, ActivityProfile.class); // intent to nav to profile activity
                            intent.putExtra(mContext.getString(R.string.calling_activity),
                                    mContext.getString(R.string.home_activity));
                            intent.putExtra(mContext.getString(R.string.user_extra), viewHolder.mUser); // another extra for passing user object
                            mContext.startActivity(intent);
                        }
                    });

                    viewHolder.mAccountSettings = singleDataSnapshot.getValue(AccountSettings.class); // getting settings
                    // onclick listener to go to comments thread
                    viewHolder.comments.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((ActivityHome)mContext).onSelectedCommentThread(getItem(position),
                                    mContext.getString(R.string.calling_activity));

                            // hide the layout when we go to comments thread so view pager is hidden want to display frame layout of comments
                            ((ActivityHome)mContext).layoutHomeHide();
                        }
                    });

                    viewHolder.speechBubble.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((ActivityHome)mContext).onSelectedCommentThread(getItem(position),
                                    mContext.getString(R.string.calling_activity));

                            // hide the layout when we go to comments thread so view pager is hidden want to display frame layout of comments
                            ((ActivityHome)mContext).layoutHomeHide();
                        }
                    });

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: query has been cancelled");
            }
        });

        // retrieves user object
        // points to user id and retrieves their photos instead of going through db of all  photos
        Query userQuery = mDatabaseReference
                .child(mContext.getString(R.string.db_name_users)) // looks in user_account_settings node
                .orderByChild(mContext.getString(R.string.user_id_field)) // looks for user_id field
                .equalTo(getItem(position).getUser_id()); // checks if we have a match to the current users user id

        Log.d(TAG, "getPostDetails: query: " + getItem(position).getUser_id().toString());
        userQuery.addListenerForSingleValueEvent(new ValueEventListener(){

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleDataSnapshot : dataSnapshot.getChildren()){
                    Log.d(TAG, "onDataChange: user found " + singleDataSnapshot.getValue(User.class).getUsername());

                    viewHolder.mUser = singleDataSnapshot.getValue(User.class);

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: query has been cancelled");
            }
        });

        // if we've reached end of list in adapter
        if(listEndReached(position)){ // pass position of adapter to see if we've reached end of list
            loadDataMore();
        }

        return convertView;
    }

    // for detecting when youve reached end of list
    private boolean listEndReached(int position){
        return position == getCount() - 1;

    }

    // check if position reached was end then load more data
    private void loadDataMore(){
        try{
            mOnPostsLoadMoreListener = (OnPostsLoadMoreListener) getContext(); // instantiate listener
        } catch(ClassCastException e){
            Log.e(TAG, "loadDataMore: ClassCastException: " + e.getMessage() );
        }

        try{
            mOnPostsLoadMoreListener.onPostsLoadMore();
        } catch(NullPointerException e){
            Log.e(TAG, "loadDataMore: NullPointerException: " + e.getMessage() );
        }
    }

    // gets the username of the current user
    private void retrieveUsernameCurrent(){
        Log.d(TAG, "retrieveUsernameCurrent: retrieving the current users account settings");

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        // points to user id and retrieves their photos instead of going through db of all  photos
        Query query = databaseReference
                .child(mContext.getString(R.string.db_name_users)) // looks in user_account_settings node
                .orderByChild(mContext.getString(R.string.user_id_field)) // looks for user_id field
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid()); // checks if we have a match to the current users user id

        Log.d(TAG, "getPostDetails: query: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener(){


            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleDataSnapshot : dataSnapshot.getChildren()){
                    currentUsername = singleDataSnapshot.getValue(AccountSettings.class).getUsername(); // gets user name for current user
                    Log.d(TAG, "onDataChange: User: " + singleDataSnapshot.getValue(AccountSettings.class).getUsername() );

                }
                //setupWidgets();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: query has been cancelled");
            }
        });
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener{
        ViewHolder mViewHolder;
        // we dont have global photo so we create a default constructor and pass the view holder
        public GestureListener(ViewHolder viewHolder){
            mViewHolder = viewHolder;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown: ");
            // Test to see if star was working
            // mStar.likeToggle();
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            Query query = databaseReference
                    .child(mContext.getString(R.string.db_name_photos)) // looks in photos node
                    .child(mViewHolder.mPhoto.getPhoto_id()) // looks for photo_id of photo
                    .child(mContext.getString(R.string.likes_field)); // checks likes field
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot singleDataSnapshot : dataSnapshot.getChildren()){

                        String keyID = singleDataSnapshot.getKey();
                        //  user liked photo already
                        if(mViewHolder.likedByCurrentUser &&
                                singleDataSnapshot.getValue(Like.class).getUser_id()
                                        .equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){ // this makes sure we are removing a like for the current user only

                            // removes like from photos node
                            mDatabaseReference.child(mContext.getString(R.string.db_name_photos))
                                    .child(mViewHolder.mPhoto.getPhoto_id())
                                    .child(mContext.getString(R.string.likes_field))
                                    .child(keyID) // gets key of id
                                    .removeValue();

                            // removes like from user photos node
                            mDatabaseReference.child(mContext.getString(R.string.db_name_user_photos))
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid()) //current users user_id
                                    .child(mViewHolder.mPhoto.getPhoto_id())
                                    .child(mContext.getString(R.string.likes_field))
                                    .child(keyID) // gets key of id
                                    .removeValue();

                            mViewHolder.star.likeToggle();
                            getLikesAsString(mViewHolder);
                        }
                        // user has not liked photo
                        else if(!mViewHolder.likedByCurrentUser){
                            // add a like to db
                            addLikeToPost(mViewHolder);
                            Log.d(TAG, "onDataChange: new like added");
                            break;
                        }

                    }

                    // checks if datasnapshot does not exist
                    if(!dataSnapshot.exists()){
                        // new like is added to db
                        Log.d(TAG, "onDataChange: datasnapshot did not exist, new like added");
                        addLikeToPost(mViewHolder);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap: ");



            return true;
        }
    }

    // need to pass viewholder to reference the objects
    private void addLikeToPost(final ViewHolder viewHolder){
        Log.d(TAG, "addLikeToPost: adding a new like");

        String newLikeID = mDatabaseReference.push().getKey(); // creates a new key
        Like like = new Like();
        like.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

        // adds like to photos node
        mDatabaseReference.child(mContext.getString(R.string.db_name_photos))
                .child(viewHolder.mPhoto.getPhoto_id())
                .child(mContext.getString(R.string.likes_field))
                .child(newLikeID) // gets key of id
                .setValue(like);

        // adds like to user photos node
        mDatabaseReference.child(mContext.getString(R.string.db_name_user_photos))
                .child(viewHolder.mPhoto.getUser_id()) //current users user_id
                .child(viewHolder.mPhoto.getPhoto_id())
                .child(mContext.getString(R.string.likes_field))
                .child(newLikeID) // gets key of id
                .setValue(like);

        viewHolder.star.likeToggle();
        getLikesAsString(viewHolder);
    }

    // gets likes for post and displays as string
    private void getLikesAsString(final ViewHolder viewHolder){
        Log.d(TAG, "getLikesAsString: getting likes");

        try{

        // Test to see if star was working
        // mStar.likeToggle();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        // query that checks if the photo has any likes
        Query query = databaseReference
                .child(mContext.getString(R.string.db_name_photos)) // looks in photos node
                .child(viewHolder.mPhoto.getPhoto_id()) // looks for photo_id of photo
                .child(mContext.getString(R.string.likes_field)); // checks likes field
        query.addListenerForSingleValueEvent(new ValueEventListener() {


            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                viewHolder.stringBuilderUsers = new StringBuilder();
                for (DataSnapshot singleDataSnapshot : dataSnapshot.getChildren()) {
                    // user liked photo already

                    // user has not liked photo
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    // query that checks if the photo has any likes
                    Query query = databaseReference
                            .child(mContext.getString(R.string.db_name_users)) // looks in users node
                            .orderByChild(mContext.getString((R.string.user_id_field))) // looks for particular user_id
                            .equalTo(singleDataSnapshot.getValue(Like.class).getUser_id()); // checks likes field
                    query.addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) { // this is only called if a like is found
                            // if we we find anyything we want to append those found strings
                            for (DataSnapshot singleDataSnapshot : dataSnapshot.getChildren()) {
                                // user liked photo already we loop and add user to string builder
                                Log.d(TAG, "onDataChange: like has been found " + singleDataSnapshot.getValue(User.class).getUsername());
                                // user is appended to string builder
                                viewHolder.stringBuilderUsers.append(singleDataSnapshot.getValue(User.class).getUsername());
                                // append comma to make it easier to handle data
                                viewHolder.stringBuilderUsers.append(",");

                            }
                            // when loop done we have to split users where we have comma
                            String[] splitUsersByComma = viewHolder.stringBuilderUsers.toString().split(",");

                            //deteermine if current user has liked photo or not
                            if(viewHolder.stringBuilderUsers.toString().contains(currentUsername + ",")){ // needs comma otherwise there is a problem when multiple users like it
                                // means user has liked photo
                                viewHolder.likedByCurrentUser = true;
                            } else {
                                viewHolder.likedByCurrentUser = false;
                            }

                            int length = splitUsersByComma.length;
                            // different cases for number of users who liked photo determines how like comment is displayed
                            if(length == 1){ // only one user likes photo etc.
                                viewHolder.stringLikes = "Liked by " + splitUsersByComma[0];
                            } else if (length == 2){
                                viewHolder.stringLikes = "Liked by " + splitUsersByComma[0] +
                                        " and " + splitUsersByComma[1];
                            }else if (length == 3){
                                viewHolder.stringLikes = "Liked by " + splitUsersByComma[0] +
                                        ", " + splitUsersByComma[1] +
                                        " and " + splitUsersByComma[2];
                            }else if (length == 4){
                                viewHolder.stringLikes = "Liked by " + splitUsersByComma[0] +
                                        ", " + splitUsersByComma[1] +
                                        ", " + splitUsersByComma[2] +
                                        " and " + splitUsersByComma[3];
                            }else if (length > 4){
                                viewHolder.stringLikes = "Liked by " + splitUsersByComma[0] +
                                        ", " + splitUsersByComma[1] +
                                        ", " + splitUsersByComma[2] +
                                        " and " +  (splitUsersByComma.length - 3) + " others"; // -3 as we have already 3 users displayed
                            }
                            //likes string set up here
                            setupStringLikes(viewHolder, viewHolder.stringLikes);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
                // if no users have liked photo
                if(!dataSnapshot.exists()){
                    viewHolder.stringLikes = "";
                    viewHolder.likedByCurrentUser = false;
                    //likes string set up here
                    setupStringLikes(viewHolder, viewHolder.stringLikes);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        } catch (NullPointerException e){
            Log.e(TAG, "getLikesAsString: NullPointerException e" + e.getMessage() );
            viewHolder.stringLikes = "";
            viewHolder.likedByCurrentUser = false;
            //likes string set up here
        }
    }

    // sets up the strings of like of a photo
    private void setupStringLikes(final ViewHolder viewHolder, String likesString){
        Log.d(TAG, "setupStringLikes: string of likes: " + viewHolder.stringLikes);

        // if photo is like by current user
        if(viewHolder.likedByCurrentUser){
            Log.d(TAG, "setupStringLikes: current user likes photo");
            viewHolder.hollowStar.setVisibility(View.GONE);
            viewHolder.yellowStar.setVisibility(View.VISIBLE);
            viewHolder.yellowStar.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return viewHolder.mGestureDetector.onTouchEvent(motionEvent);
                }
            });
        } else { // if photo is not liked by current user
            Log.d(TAG, "setupStringLikes: current user does not like photo");
            viewHolder.hollowStar.setVisibility(View.VISIBLE);
            viewHolder.yellowStar.setVisibility(View.GONE);
            viewHolder.hollowStar.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return viewHolder.mGestureDetector.onTouchEvent(motionEvent);
                }
            });
        }

        viewHolder.likes.setText(likesString); // sets up the likes string here
    }

    // returns date string that shows how many days ago the post was made
    private String getDateTimeStampDifference(Photo photo){
        Log.d(TAG, "getDateTimeStampDifference: retrieving date timestamp");

        String difference = "";
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        Date timestamp;
        Date today = calendar.getTime();
        simpleDateFormat.format(today); // need to format date object and convert to string
        final String timestampPhoto = photo.getDate_created();
        try{
            timestamp = simpleDateFormat.parse(timestampPhoto);
            difference = String.valueOf(Math.round(((today.getTime() -  timestamp.getTime()) / 1000 / 60 / 60 / 24))); // works out number of days, getTime() converts string to long
            Log.d(TAG, "getDateTimeStampDifference: timestamp: " + difference);
        } catch(ParseException e){
            Log.e(TAG, "getDateTimeStampDifference: ParseException" + e.getMessage() );
            difference = "0";
        }
        return difference;
    }

}
