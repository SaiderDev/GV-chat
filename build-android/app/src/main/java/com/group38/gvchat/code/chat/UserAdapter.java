package com.group38.gvchat.code.chat;

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
import com.group38.gvchat.R;
import com.group38.gvchat.code.chat.model.User;

public class UserAdapter extends FirebaseRecyclerAdapter<User, UserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private final OnUserClickListener listener;
    private final String currentUid;

    public UserAdapter(@NonNull FirebaseRecyclerOptions<User> options, String currentUid, OnUserClickListener listener) {
        super(options);
        this.currentUid = currentUid;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull User model) {
        boolean isCurrentUser = model.getUid() != null && model.getUid().equals(currentUid);
        holder.bind(model, listener, isCurrentUser);
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarImageView;
        private final TextView nameTextView;
        private final TextView emailTextView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.userAvatarImageView);
            nameTextView = itemView.findViewById(R.id.userNameTextView);
            emailTextView = itemView.findViewById(R.id.userEmailTextView);
        }

        public void bind(User user, OnUserClickListener listener, boolean isCurrentUser) {
            String name = user.getDisplayName() != null ? user.getDisplayName() : "User";
            if (isCurrentUser) {
                nameTextView.setText(name + " (You)");
            } else {
                nameTextView.setText(name);
            }
            emailTextView.setText(user.getEmail() != null ? user.getEmail() : "");

            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getPhotoUrl())
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_account_circle_black_36dp)
                        .error(R.drawable.ic_account_circle_black_36dp)
                        .into(avatarImageView);
            } else {
                avatarImageView.setImageResource(R.drawable.ic_account_circle_black_36dp);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}
