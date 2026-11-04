
package com.group38.gvchat.code.chat;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.activity.result.contract.ActivityResultContracts;

/**
 * See:
 * https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.OpenDocument
 */
public class MyOpenDocumentContract extends ActivityResultContracts.OpenDocument {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull String[] input) {
        Intent intent = super.createIntent(context, input);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }
}
