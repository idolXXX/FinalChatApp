package com.example.finalchatapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalchatapp.ChatActivity;
import com.example.finalchatapp.R;
import com.example.finalchatapp.models.ChatPreview;
import com.example.finalchatapp.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ViewHolder> {

    private Context context;
    private List<ChatPreview> chatPreviews;
    private FirebaseFirestore db;

    public ChatsAdapter(Context context, List<ChatPreview> chatPreviews) {
        this.context = context;
        this.chatPreviews = chatPreviews;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatPreview chatPreview = chatPreviews.get(position);

        // Load other user info
        db.collection("users").document(chatPreview.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        holder.usernameText.setText(user.getUsername());
                        // Load profile image with Glide if available
                        // For now, we'll use the default profile image
                    }
                });

        // Set last message and time
        holder.lastMessageText.setText(chatPreview.getLastMessageContent());
        holder.timeText.setText(formatTime(chatPreview.getLastMessageTimestamp()));

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("userId", chatPreview.getUserId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatPreviews.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView usernameText, lastMessageText, timeText;

        ViewHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            usernameText = itemView.findViewById(R.id.username_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            timeText = itemView.findViewById(R.id.time_text);
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}