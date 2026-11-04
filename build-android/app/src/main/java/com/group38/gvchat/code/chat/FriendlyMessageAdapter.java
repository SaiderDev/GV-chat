
package com.group38.gvchat.code.chat;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.group38.gvchat.R;
import com.group38.gvchat.code.chat.model.FriendlyMessage;
import com.group38.gvchat.databinding.ImageMessageBinding;
import com.group38.gvchat.databinding.MessageBinding;

// The FirebaseRecyclerAdapter class and options come from the FirebaseUI library
// See: https://github.com/firebase/FirebaseUI-Android
public class FriendlyMessageAdapter extends FirebaseRecyclerAdapter<FriendlyMessage, RecyclerView.ViewHolder> {
    public static final String TAG = "MessageAdapter";
    public static final int VIEW_TYPE_TEXT = 1;
    public static final int VIEW_TYPE_IMAGE = 2;

    private final String currentUserName;

    public FriendlyMessageAdapter(@NonNull FirebaseRecyclerOptions<FriendlyMessage> options, String currentUserName) {
        super(options);
        this.currentUserName = currentUserName;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_TEXT) {
            View view = inflater.inflate(R.layout.message, parent, false);
            MessageBinding binding = MessageBinding.bind(view);
            return new MessageViewHolder(binding);
        } else {
            View view = inflater.inflate(R.layout.image_message, parent, false);
            ImageMessageBinding binding = ImageMessageBinding.bind(view);
            return new ImageMessageViewHolder(binding);
        }
    }

    @Override
    protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull FriendlyMessage model) {
        if (getItem(position).getText() != null) {
            ((MessageViewHolder) holder).bind(model);
        } else {
            ((ImageMessageViewHolder) holder).bind(model);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).getText() != null) {
            return VIEW_TYPE_TEXT;
        } else {
            return VIEW_TYPE_IMAGE;
        }
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        private final MessageBinding binding;

        public MessageViewHolder(MessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FriendlyMessage item) {
            String decryptedText = EncryptionUtil.decrypt(item.getText());
            binding.messageTextView.setText(decryptedText);
            setTextColor(item.getName(), binding.messageTextView);

            binding.messengerTextView.setText(item.getName() != null ? item.getName() : MainActivity.ANONYMOUS);
            if (item.getPhotoUrl() != null) {
                loadImageIntoView(binding.messengerImageView, item.getPhotoUrl(), true);
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp);
            }
        }

        private void setTextColor(String userName, TextView textView) {
            if (userName != null && !MainActivity.ANONYMOUS.equals(userName) && userName.equals(currentUserName)) {
                textView.setBackgroundResource(R.drawable.rounded_message_blue);
                textView.setTextColor(Color.WHITE);
            } else {
                textView.setBackgroundResource(R.drawable.rounded_message_gray);
                textView.setTextColor(Color.BLACK);
            }
        }
    }

    public class ImageMessageViewHolder extends RecyclerView.ViewHolder {
        private final ImageMessageBinding binding;

        public ImageMessageViewHolder(ImageMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FriendlyMessage item) {
            if (item.getImageUrl() != null) {
                loadImageIntoView(binding.messageImageView, item.getImageUrl(), false);
            }

            binding.messengerTextView.setText(item.getName() != null ? item.getName() : MainActivity.ANONYMOUS);
            if (item.getPhotoUrl() != null) {
                loadImageIntoView(binding.messengerImageView, item.getPhotoUrl(), true);
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp);
            }
        }
    }

    private void loadImageIntoView(ImageView view, String url, boolean isCircular) {
        if (url.startsWith("gs://")) {
            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(url);
            storageReference.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        loadWithGlide(view, downloadUrl, isCircular);
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "Getting download url was not successful.", e));
        } else {
            loadWithGlide(view, url, isCircular);
        }
    }

    private void loadWithGlide(ImageView view, String url, boolean isCircular) {
        if (isCircular) {
            Glide.with(view.getContext())
                    .load(url)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_account_circle_black_36dp)
                    .error(R.drawable.ic_account_circle_black_36dp)
                    .into(view);
        } else {
            Glide.with(view.getContext())
                    .load(url)
                    .placeholder(R.drawable.rounded_message_gray)
                    .error(R.drawable.rounded_message_gray)
                    .into(view);
        }
    }
}
