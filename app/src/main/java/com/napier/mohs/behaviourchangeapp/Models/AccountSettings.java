package com.napier.mohs.behaviourchangeapp.Models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Mohs on 17/03/2018.
 */

public class AccountSettings implements Parcelable{

    private String description, display_name, profile_photo, username, website, user_id;
    private long followers, following, posts;

    public AccountSettings(String description, String display_name, long followers,
                           long following, long posts, String profile_photo, String username,
                           String website, String user_id) {
        this.description = description;
        this.display_name = display_name;
        this.followers = followers;
        this.following = following;
        this.posts = posts;
        this.profile_photo = profile_photo;
        this.username = username;
        this.website = website;
        this.user_id = user_id;
    }

    public AccountSettings() {

    }

    protected AccountSettings(Parcel in) {
        description = in.readString();
        display_name = in.readString();
        followers = in.readLong();
        following = in.readLong();
        posts = in.readLong();
        profile_photo = in.readString();
        username = in.readString();
        website = in.readString();
        user_id = in.readString();
    }

    public static final Creator<AccountSettings> CREATOR = new Creator<AccountSettings>() {
        @Override
        public AccountSettings createFromParcel(Parcel in) {
            return new AccountSettings(in);
        }

        @Override
        public AccountSettings[] newArray(int size) {
            return new AccountSettings[size];
        }
    };

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public long getFollowers() {
        return followers;
    }

    public void setFollowers(long followers) {
        this.followers = followers;
    }

    public long getFollowing() {
        return following;
    }

    public void setFollowing(long following) {
        this.following = following;
    }

    public long getPosts() {
        return posts;
    }

    public void setPosts(long posts) {
        this.posts = posts;
    }

    public String getProfile_photo() {
        return profile_photo;
    }

    public void setProfile_photo(String profile_photo) {
        this.profile_photo = profile_photo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }


    @Override
    public String toString() {
        return "AccountSettings{" +
                "description='" + description + '\'' +
                ", display_name='" + display_name + '\'' +
                ", followers=" + followers +
                ", following=" + following +
                ", posts=" + posts +
                ", profile_photo='" + profile_photo + '\'' +
                ", username='" + username + '\'' +
                ", website='" + website + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(description);
        parcel.writeString(display_name);
        parcel.writeLong(followers);
        parcel.writeLong(following);
        parcel.writeLong(posts);
        parcel.writeString(profile_photo);
        parcel.writeString(username);
        parcel.writeString(website);
        parcel.writeString(user_id);
    }
}