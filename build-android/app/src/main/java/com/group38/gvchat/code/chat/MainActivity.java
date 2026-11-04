package com.group38.gvchat.code.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.group38.gvchat.R;
import com.group38.gvchat.code.chat.model.FriendlyMessage;
import com.group38.gvchat.code.chat.model.User;
import com.group38.gvchat.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    public static final String USERS_CHILD = "users";
    public static final String CHATS_CHILD = "chats";
    public static final String ANONYMOUS = "anonymous";

    private ActivityMainBinding binding;
    private LinearLayoutManager manager;
    private ActionBarDrawerToggle drawerToggle;

    // Firebase instance variables
    private FirebaseAuth auth;
    private FirebaseDatabase db;
    private FriendlyMessageAdapter adapter;
    private UserAdapter userAdapter;

    private String activeChatPath = MESSAGES_CHILD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth and check if the user is signed in
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        // Initialize Realtime Database
        db = FirebaseDatabase.getInstance();

        // Register/update current user profile in /users
        registerUserInDatabase();

        // Setup ActionBar & DrawerToggle
        setupNavigationDrawer();

        // Setup Chat RecyclerView
        manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        binding.messageRecyclerView.setLayoutManager(manager);

        updateChatChannel("GV Chat (Global)", MESSAGES_CHILD);

        // Setup User List in Left Drawer
        setupUserListDrawer();

        // Disable/enable send button when typing
        binding.messageEditText.addTextChangedListener(new MyButtonObserver(binding.sendButton));

        // Send button click listener
        binding.sendButton.setOnClickListener(view -> {
            String rawText = binding.messageEditText.getText().toString().trim();
            if (!rawText.isEmpty()) {
                String encryptedText = EncryptionUtil.encrypt(rawText);
                FriendlyMessage friendlyMessage = new FriendlyMessage(
                        encryptedText,
                        getUserName(),
                        getPhotoUrl(),
                        null
                );
                db.getReference().child(activeChatPath).push().setValue(friendlyMessage);
                binding.messageEditText.setText("");
            }
        });

        // Global Chat Option Click Listener
        binding.globalChatOption.setOnClickListener(v -> {
            updateChatChannel("GV Chat (Global)", MESSAGES_CHILD);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    private void registerUserInDatabase() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && db != null) {
            User user = new User(
                    currentUser.getUid(),
                    getUserName(),
                    currentUser.getEmail(),
                    getPhotoUrl()
            );
            db.getReference().child(USERS_CHILD).child(currentUser.getUid()).setValue(user)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User registered successfully in /users"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to register user in /users: " + e.getMessage(), e));
        }
    }

    private void setupNavigationDrawer() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        drawerToggle = new ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                R.string.app_name,
                R.string.app_name
        );
        binding.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    private void setupUserListDrawer() {
        DatabaseReference usersRef = db.getReference().child(USERS_CHILD);
        FirebaseRecyclerOptions<User> userOptions =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(usersRef, User.class)
                        .build();

        String currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        userAdapter = new UserAdapter(userOptions, currentUid, this::openPrivateChat);

        binding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.usersRecyclerView.setAdapter(userAdapter);
    }

    private void openPrivateChat(User recipient) {
        if (auth.getCurrentUser() == null || recipient == null || recipient.getUid() == null) return;

        String myUid = auth.getCurrentUser().getUid();
        String recipientUid = recipient.getUid();

        // Deterministic chatId derived from both UIDs sorted alphabetically
        String chatId = myUid.compareTo(recipientUid) < 0 ? myUid + "_" + recipientUid : recipientUid + "_" + myUid;
        String privatePath = CHATS_CHILD + "/" + chatId + "/" + MESSAGES_CHILD;

        String recipientName = recipient.getDisplayName() != null ? recipient.getDisplayName() : "Private Chat";
        updateChatChannel("Chat: " + recipientName, privatePath);

        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void updateChatChannel(String title, String path) {
        activeChatPath = path;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        if (adapter != null) {
            adapter.stopListening();
        }

        DatabaseReference messagesRef = db.getReference().child(activeChatPath);
        FirebaseRecyclerOptions<FriendlyMessage> options =
                new FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                        .setQuery(messagesRef, FriendlyMessage.class)
                        .build();

        adapter = new FriendlyMessageAdapter(options, getUserName());
        binding.progressBar.setVisibility(ProgressBar.INVISIBLE);
        binding.messageRecyclerView.setAdapter(adapter);

        adapter.registerAdapterDataObserver(
                new MyScrollToBottomObserver(binding.messageRecyclerView, adapter, manager)
        );

        adapter.startListening();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
        if (userAdapter != null) {
            userAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        if (userAdapter != null) {
            userAdapter.stopListening();
        }
        super.onStop();
    }

    @Override
    public void onPause() {
        if (adapter != null) {
            adapter.stopListening();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.startListening();
        }
        if (userAdapter != null) {
            userAdapter.startListening();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == R.id.sign_out_menu) {
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
