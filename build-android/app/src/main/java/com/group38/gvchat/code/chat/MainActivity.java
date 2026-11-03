/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.group38.gvchat.code.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.group38.gvchat.R;
import com.group38.gvchat.code.chat.model.FriendlyMessage;
import com.group38.gvchat.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    public static final String ANONYMOUS = "anonymous";
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";

    private ActivityMainBinding binding;
    private LinearLayoutManager manager;

    // Firebase instance variables
    private FirebaseAuth auth;
    private FirebaseDatabase db;
    private FriendlyMessageAdapter adapter;

    private final ActivityResultLauncher<String[]> openDocument =
            registerForActivityResult(new MyOpenDocumentContract(), uri -> {
                if (uri != null) {
                    onImageSelected(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This codelab uses View Binding
        // See: https://developer.android.com/topic/libraries/view-binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth and check if the user is signed in
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        // Initialize Realtime Database
        db = FirebaseDatabase.getInstance();
        DatabaseReference messagesRef = db.getReference().child(MESSAGES_CHILD);

        // The FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        // See: https://github.com/firebase/FirebaseUI-Android
        FirebaseRecyclerOptions<FriendlyMessage> options =
                new FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                        .setQuery(messagesRef, FriendlyMessage.class)
                        .build();
        adapter = new FriendlyMessageAdapter(options, getUserName());
        binding.progressBar.setVisibility(ProgressBar.INVISIBLE);
        manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        binding.messageRecyclerView.setLayoutManager(manager);
        binding.messageRecyclerView.setAdapter(adapter);

        // Scroll down when a new message arrives
        // See MyScrollToBottomObserver for details
        adapter.registerAdapterDataObserver(
                new MyScrollToBottomObserver(binding.messageRecyclerView, adapter, manager)
        );

        // Disable the send button when there's no text in the input field
        // See MyButtonObserver for details
        binding.messageEditText.addTextChangedListener(new MyButtonObserver(binding.sendButton));

        // When the send button is clicked, send a text message
        binding.sendButton.setOnClickListener(view -> {
            FriendlyMessage friendlyMessage = new FriendlyMessage(
                    binding.messageEditText.getText().toString(),
                    getUserName(),
                    getPhotoUrl(),
                    null
            );
            db.getReference().child(MESSAGES_CHILD).push().setValue(friendlyMessage);
            binding.messageEditText.setText("");
        });

        // When the image button is clicked, launch the image picker
        binding.addMessageImageView.setOnClickListener(view ->
                openDocument.launch(new String[]{"image/*"})
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        if (auth.getCurrentUser() == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
    }

    @Override
    public void onPause() {
        adapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.startListening();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sign_out_menu) {
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onImageSelected(Uri uri) {
        Log.d(TAG, "Uri: " + uri);
        FirebaseUser user = auth.getCurrentUser();
        FriendlyMessage tempMessage = new FriendlyMessage(null, getUserName(), getPhotoUrl(), LOADING_IMAGE_URL);
        db.getReference()
                .child(MESSAGES_CHILD)
                .push()
                .setValue(
                        tempMessage,
                        (databaseError, databaseReference) -> {
                            if (databaseError != null) {
                                Log.w(
                                        TAG, "Unable to write message to database.",
                                        databaseError.toException()
                                );
                                return;
                            }

                            // Build a StorageReference and then upload the file
                            String key = databaseReference.getKey();
                            String lastSegment = uri.getLastPathSegment();
                            if (key != null && lastSegment != null && user != null) {
                                StorageReference storageReference = FirebaseStorage.getInstance()
                                        .getReference(user.getUid())
                                        .child(key)
                                        .child(lastSegment);
                                putImageInStorage(storageReference, uri, key);
                            }
                        });
    }

    private void putImageInStorage(StorageReference storageReference, Uri uri, String key) {
        // First upload the image to Cloud Storage
        storageReference.putFile(uri)
                .addOnSuccessListener(
                        this,
                        taskSnapshot -> {
                            // After the image loads, get a public downloadUrl for the image
                            // and add it to the message.
                            if (taskSnapshot.getMetadata() != null && taskSnapshot.getMetadata().getReference() != null) {
                                taskSnapshot.getMetadata().getReference().getDownloadUrl()
                                        .addOnSuccessListener(downloadUri -> {
                                            FriendlyMessage friendlyMessage =
                                                    new FriendlyMessage(null, getUserName(), getPhotoUrl(), downloadUri.toString());
                                            db.getReference()
                                                    .child(MESSAGES_CHILD)
                                                    .child(key)
                                                    .setValue(friendlyMessage);
                                        });
                            }
                        }
                )
                .addOnFailureListener(this, e ->
                        Log.w(
                                TAG,
                                "Image upload task was unsuccessful.",
                                e
                        )
                );
    }

    private void signOut() {
        AuthUI.getInstance().signOut(this);
        startActivity(new Intent(this, SignInActivity.class));
        finish();
    }

    private String getPhotoUrl() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getPhotoUrl() != null) {
            return user.getPhotoUrl().toString();
        }
        return null;
    }

    private String getUserName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            return user.getDisplayName();
        }
        return ANONYMOUS;
    }
}
