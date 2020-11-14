/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

// This cannot be resolved because we removed the com.android.support:appcompat dependency
// import android.support.v7.app.AppCompatActivity;

// So we replace it with its AndroidX alternative
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN=1;
    public static final String FRIENDLY_MSG_LENGTH_KEY = "friendly_msg_length";

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private  Button btn;
    private static final int RC_PHOTO_PICKER = 2;

    private String mUsername;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    //Remote Config Settings
    private FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600).build();


    //mFirebaseRemoteConfig.setConfigSettingsAsync(config)






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        Log.d(TAG,"DEBUG NODE FOR ONCREATE");
        Log.i(TAG,"INFO NODE FOR ONCREATE");

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        mFirebaseAuth = FirebaseAuth.getInstance();

        mFirebaseStorage = FirebaseStorage.getInstance();
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");


        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
        //btn = findViewById(R.id.btn);

       // mMessagesDatabaseReference = FirebaseDatabase.getInstance().getReference();

        //Initialize firebase config objects;
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600).build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(FRIENDLY_MSG_LENGTH_KEY, DEFAULT_MSG_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setDefaultsAsync(defaultConfigMap);
        fetchConfig();




        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Todo: Send a message onClick
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);
                //Toast.makeText(MainActivity.this, "We clicked it", Toast.LENGTH_SHORT).show();
                Toast toast = Toast.makeText(MainActivity.this, "Clicckid", Toast.LENGTH_SHORT
                );
                toast.show();

                mMessageEditText.setText("");
            }
        });


//        mMessagesDatabaseReference.addChildEventListener(mChildEventListener);

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null){
                    //User is signed in
                    Toast.makeText(MainActivity.this, "You are now signed in to FriendLyChat!! ", Toast.LENGTH_SHORT).show();
                    onSignedInInitializer(user.getDisplayName());

                }
                else{
                    onSignedOutCleanup();
                    //User is signed out
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build()
                                            ))
                                    .build(),
                            RC_SIGN_IN);

                }

//                mPhotoPickerButton.setOnClickListener(new View.OnClickListener(){
//                    @Override
//                    public void onClick(View v) {
//                        Toast.makeText(MainActivity.this, "Clicked our img btn", Toast.LENGTH_LONG);
//                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                        intent.setType("image/jpeg");
//                        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
//                        startActivityForResult(Intent.createChooser(intent,"Complete action using"),RC_PHOTO_PICKER);
//
//
//                    }
//                });



            }
        };


        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
                Toast.makeText(MainActivity.this, "Clicked our img btn", Toast.LENGTH_LONG);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent,"Complete action using mnk"),RC_PHOTO_PICKER);

            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
//        mSendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // TODO: Send messages on click
//
//                // Clear input box
//                mMessageEditText.setText("");
//            }
//        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,"WE'VE LAUNCHED OUR DEBUG NODE");
        Log.i(TAG,"lAUNCHED INFO LOG");

       if (requestCode == RC_PHOTO_PICKER){
//            Log.d(TAG,"RC PHOTO REACHED");
//            Toast.makeText(this, "Request Code RC PHOTO REACHED", Toast.LENGTH_LONG).show();
//
//            Uri selecteedImageUri = data.getData();
//            Log.d(TAG,"Picking our image");
//            Log.d(TAG, String.valueOf(selecteedImageUri));
//
//
//            final StorageReference ref = mChatPhotosStorageReference.child(selecteedImageUri.getLastPathSegment());
//            //uploadTask  = photoRef.putFile(selecteedImageUri);
//            Log.d(TAG, String.valueOf(ref));
//
//            ref.putFile(selecteedImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                    //Toast.makeText(this, "Sent our image", Toast.LENGTH_SHORT).show();
//                    Toast.makeText(MainActivity.this, "Sent our image", Toast.LENGTH_SHORT).show();
//                    Log.d(TAG,"iMAGE SENT");
//                    //StorageReference downloadUrl = taskSnapshot.getStorage();
//                    Task<Uri> downloadUrl = ref.getDownloadUrl();
//                    Log.d(TAG,"StorageRerence below");
//                    Log.d(TAG, String.valueOf(downloadUrl));
//                    Log.d(TAG, String.valueOf(taskSnapshot));
//                    FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, taskSnapshot.getUploadSessionUri().toString());
//                    mMessagesDatabaseReference.push().setValue(friendlyMessage);
//
//
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    Toast.makeText(MainActivity.this, "Failed to send image", Toast.LENGTH_SHORT).show();
//                    Log.d(TAG,"fAILED TO SEND IMAGE for some reason ");
//
//                }
//            });
//
//
//
           Toast.makeText(this, "Sending our image logic", Toast.LENGTH_SHORT).show();
           Uri selectedImageUri = data.getData();
           //Get a reference to the file
           StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
           photoRef.putFile(selectedImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
               @Override
               public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                   Uri downloadUrl = taskSnapshot.getUploadSessionUri();
                   FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, downloadUrl.toString());
                   mMessagesDatabaseReference.push().setValue(friendlyMessage);

               }
           });

       }

        if (requestCode == RC_SIGN_IN){
            Log.d(TAG,"Reached our RC_SIGN_IN");
            if (resultCode == RESULT_OK){
                Toast.makeText(this, "Signed In", Toast.LENGTH_SHORT).show();

            }
            else if (resultCode == RESULT_CANCELED){
                Toast.makeText(this, "Sign In Cancelled", Toast.LENGTH_SHORT).show();
                finish();

            }
            else if (requestCode == RC_PHOTO_PICKER){
                Toast.makeText(this,"Inside our elif for RC Photo Picker sending image", Toast.LENGTH_LONG);
//                Toast.makeText(this,"Innner Inner request code", Toast.LENGTH_LONG);
//                Log.d(TAG,"iNNER INNER request Code");
//                Uri selecteedImageUri = data.getData();
//                Log.d(TAG,"Picking our image");
//                Log.d(TAG, String.valueOf(selecteedImageUri));
//
//
//                final StorageReference ref = mChatPhotosStorageReference.child(selecteedImageUri.getLastPathSegment());
//                //uploadTask  = photoRef.putFile(selecteedImageUri);
//
//                Task<Uri> urlTask = ref.putFile(selecteedImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
//                    @Override
//                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
//                        if (!task.isSuccessful()) {
//                            throw task.getException();
//                        }
//
//                        // Continue with the task to get the download URL
//                        return ref.getDownloadUrl();
//                    }
//                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Uri> task) {
//                        if (task.isSuccessful()) {
//                            Uri downloadUri = task.getResult();
//                            Toast.makeText(MainActivity.this,"Successfullyy sent", Toast.LENGTH_LONG);
//                        } else {
//                            // Handle failures
//                            // ...
//                            Toast.makeText(MainActivity.this,"Successfullyy sent", Toast.LENGTH_LONG);
//                        }
//                    }
//                });


                Uri selectedImageUri = data.getData();
                //Get a reference to the file
                StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
                photoRef.putFile(selectedImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUrl = taskSnapshot.getUploadSessionUri();
                        FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, downloadUrl.toString());
                        mMessagesDatabaseReference.push().setValue(friendlyMessage);

                    }
                });






            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);

        }
        detachDatabaseReadListener();
        mMessageAdapter.clear();

    }

//    @Override
//    protected void onResume(){
//        super.onResume();
//        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
//
//    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.sign_out_menu:
                //Sign out
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSignedInInitializer(String username){
        mUsername = username;
        attachDatabaseReadListener();
        //mMessagesDatabaseReference.addChildEventListener(mChildEventListener);

    }

    private void onSignedOutCleanup(){
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();

    }

    private void attachDatabaseReadListener(){
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    FriendlyMessage friendlyMessage = snapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);


                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }

            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }

    }

    private void detachDatabaseReadListener(){
        if(mChildEventListener != null){
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }

    }

    public void fetchConfig(){
        long cacheExpiration =3600;
        mFirebaseRemoteConfig.fetch(cacheExpiration).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mFirebaseRemoteConfig.fetchAndActivate();
                applyRetrievedLengthLimit();

            }
        });

    }

    public void applyRetrievedLengthLimit(){
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(FRIENDLY_MSG_LENGTH_KEY);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue()) {
        }});
        Log.d(TAG,FRIENDLY_MSG_LENGTH_KEY + " = " + friendly_msg_length);

    }

}
