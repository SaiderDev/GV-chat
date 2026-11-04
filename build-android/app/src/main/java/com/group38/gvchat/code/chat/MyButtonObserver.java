
package com.group38.gvchat.code.chat;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;

import com.group38.gvchat.R;

public class MyButtonObserver implements TextWatcher {
    private final ImageView button;

    public MyButtonObserver(ImageView button) {
        this.button = button;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
        if (charSequence.toString().trim().length() > 0) {
            button.setEnabled(true);
            button.setImageResource(R.drawable.outline_send_24);
        } else {
            button.setEnabled(false);
            button.setImageResource(R.drawable.outline_send_gray_24);
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {}
}
