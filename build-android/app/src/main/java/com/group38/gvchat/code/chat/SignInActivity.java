
package com.group38.gvchat.code.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.group38.gvchat.R;
import com.group38.gvchat.databinding.ActivitySignInBinding;

import java.util.Arrays;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";

    private ActivitySignInBinding binding;

    private final ActivityResultLauncher<Intent> signIn =
            registerForActivityResult(new FirebaseAuthUIActivityResultContract(), this::onSignInResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    public void onStart() {
        super.onStart();

        // If there is no signed in user, launch FirebaseUI
        // Otherwise head to MainActivity
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setLogo(R.mipmap.ic_launcher)
                    .setAvailableProviders(Arrays.asList(
                           // new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build()
                    ))
                    .build();

            signIn.launch(signInIntent);
        } else {
            goToMainActivity();
        }
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        if (result.getResultCode() == RESULT_OK) {
            Log.d(TAG, "Sign in successful!");
            goToMainActivity();
        } else {
            Toast.makeText(
                    this,
                    "There was an error signing in",
                    Toast.LENGTH_LONG).show();

            IdpResponse response = result.getIdpResponse();
            if (response == null) {
                Log.w(TAG, "Sign in canceled");
            } else {
                Log.w(TAG, "Sign in error", response.getError());
            }
        }
    }

    private void goToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
