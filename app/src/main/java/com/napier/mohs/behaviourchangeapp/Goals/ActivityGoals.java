package com.napier.mohs.behaviourchangeapp.Goals;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.napier.mohs.behaviourchangeapp.Models.Goal;
import com.napier.mohs.behaviourchangeapp.R;
import com.napier.mohs.behaviourchangeapp.Utils.AdapterGoalList;
import com.napier.mohs.behaviourchangeapp.Utils.SettingsBottomNavigationViewEx;

import java.util.ArrayList;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Mohs on 24/03/2018.
 */

public class ActivityGoals extends AppCompatActivity {
    private static final String TAG = "ActivityGoals";

    private Context mContext = ActivityGoals.this;
    // Firebase Stuff
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myDBRefFirebase;

    private static final int ACTIVITY_NUMBER = 1;

    // database queries
    @BindString(R.string.db_name_goals)
    String db_goals;

    // widgets
    @BindView(R.id.listviewGoals)
    ListView mListView;
    @BindView(R.id.textviewGoalAdd)
    TextView addgoal;
    @BindView(R.id.imageGoalAdd)
    ImageView imgAddGoal;
    @BindView(R.id.textviewGoalNone)
    TextView goalText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);
        ButterKnife.bind(this);
        Log.d(TAG, "onCreate: started diary activity");

        mAuth = FirebaseAuth.getInstance();

        mFirebaseDatabase = FirebaseDatabase.getInstance(); // sets up database instance
        myDBRefFirebase = mFirebaseDatabase.getReference(); // sets up a reference to that instance

        setupFirebaseAuth();


        addgoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, ActivityGoalExerciseList.class);
                startActivity(intent);
            }
        });

        imgAddGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, ActivityGoalExerciseList.class);
                startActivity(intent);
            }
        });

        queryDB();
        bottomNavbarSetup();

    }


    // check to see if the edit or delete buttons have been clicked, boolean true means it has
    boolean delete, edit;
    int position; // position in arraylist that has been clicked to bring up context menu

    @Override
    public void onCreateContextMenu(android.view.ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_delete_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        position = info.position;
        switch (item.getItemId()) {
            case R.id.edit:
                Log.d(TAG, "onContextItemSelected: edit pressed");
                edit = true;
                queryDB();
                return true;

            case R.id.delete:
                Log.d(TAG, "onContextItemSelected: delete pressed");
                delete = true;
                queryDB();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }






    // setup of bottom navbar
    private void bottomNavbarSetup(){
        Log.d(TAG, "bottomNavbarSetup: setting up BottomNavigationView");
        BottomNavigationViewEx bottomNavigationViewEx = (BottomNavigationViewEx) findViewById(R.id.bottomNavViewBar);
        SettingsBottomNavigationViewEx.bottomNavigationViewExSetup(bottomNavigationViewEx);
        SettingsBottomNavigationViewEx.enableNavigation(mContext, this, bottomNavigationViewEx);
        Menu menu = bottomNavigationViewEx.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUMBER );
        menuItem.setChecked(true);
    }

    // queries database to see what is in goals node currently
    private void queryDB() {

        final ArrayList<Goal> goalArrayList = new ArrayList<Goal>();
        final ArrayList<String> keyList = new ArrayList<>();
        Query query = myDBRefFirebase
                .child(db_goals) // looks in goals node
                .child(FirebaseAuth.getInstance() // looks in current user node
                        .getCurrentUser().getUid()); // looks in date chosen in calendar

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot singleDataSnapshot : dataSnapshot.getChildren()) {
                    keyList.add(singleDataSnapshot.getKey()); // keylist is for deleting from firebase database
                    Goal goal = new Goal();
                    goal.setGoal_id(singleDataSnapshot.getValue(Goal.class).getGoal_id().toString());
                    goal.setGoal_name(singleDataSnapshot.getValue(Goal.class).getGoal_name().toString());
                    goal.setGoal_weight(singleDataSnapshot.getValue(Goal.class).getGoal_weight().toString());
                    goal.setCurrent_weight(singleDataSnapshot.getValue(Goal.class).getCurrent_weight().toString());
                    goalArrayList.add(goal); //adds the data to this array list
                    Log.d(TAG, "onDataChange: looping");


                }
                Log.d(TAG, "onDataChange: number of loops " + goalArrayList.size());
                final AdapterGoalList adapter = new AdapterGoalList(mContext, R.layout.listitem_goals, goalArrayList);
                mListView.setAdapter(adapter); // arraylist is adapted to the list view

                registerForContextMenu(mListView);
                if(keyList.size()>0){
                    goalText.setText("");
                }
                // if delete is true the item from list is deleted
                if(delete == true){
                    goalArrayList.remove(position);
                    adapter.notifyDataSetChanged();
                    //new code below
                    myDBRefFirebase
                            .child(db_goals) // looks in goals node
                            .child(FirebaseAuth.getInstance()
                                    .getCurrentUser().getUid())
                            .child(keyList.get(position)).removeValue();
                    keyList.remove(position);
                    delete = false;

                }

                // if edit is true
                if(edit == true){
                    String goal_id  =goalArrayList.get(position).getGoal_id();
                    String name = goalArrayList.get(position).getGoal_name();
                    String weight = goalArrayList.get(position).getGoal_weight();
                    Log.d(TAG, "onDataChange: " + goal_id);
                    Log.d(TAG, "onDataChange: " + weight);
                    Log.d(TAG, "onDataChange: " + name);

                    // When edit is clicked edit diary is opened
                    Log.d(TAG, "onClick: clicked edit diary button");

                    Intent intent = new Intent(mContext, ActivityEditGoals.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("goal_id", goal_id);
                    bundle.putString( "name",name);
                    bundle.putString("weight",weight);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    edit = false;
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

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


