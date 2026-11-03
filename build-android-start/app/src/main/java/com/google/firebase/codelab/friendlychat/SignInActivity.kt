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
package com.google.firebase.codelab.friendlychat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.codelab.friendlychat.databinding.ActivitySignInBinding
import com.google.firebase.ktx.Firebase

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding

    // ActivityResultLauncher
    private val signInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(FirebaseAuthUIActivityResultContract(), this::onSignInResult)

    // Firebase instance variables
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This codelab uses View Binding
        // See: https://developer.android.com/topic/libraries/view-binding
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize FirebaseAuth
        // if (BuildConfig.DEBUG) {
        //     FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
        // }
        auth = Firebase.auth
    }

    public override fun onStart() {
        super.onStart()

        // If there is no signed in user, launch FirebaseUI
        // Otherwise head to MainActivity
        if (auth.currentUser == null) {
            signIn()
        } else {
            goToMainActivity()
        }
    }

    private fun signIn() {
        val providers = mutableListOf<AuthUI.IdpConfig>(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.AnonymousBuilder().build()
        )

        val webClientIdResId = resources.getIdentifier("default_web_client_id", "string", packageName)
        if (webClientIdResId != 0) {
            providers.add(AuthUI.IdpConfig.GoogleBuilder().build())
        }

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setLogo(R.mipmap.ic_launcher)
            .setAvailableProviders(providers)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Sign in successful!")
            goToMainActivity()
        } else {
            Toast.makeText(
                this,
                "There was an error signing in",
                Toast.LENGTH_LONG).show()

            val response = result.idpResponse
            if (response == null) {
                Log.w(TAG, "Sign in canceled")
            } else {
                Log.w(TAG, "Sign in error", response.error)
            }
        }
    }
    //Takes you to the Main Activity page
    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "SignInActivity"
    }
}
