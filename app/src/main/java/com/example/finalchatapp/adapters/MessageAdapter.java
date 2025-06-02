package com.example.finalchatapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalchatapp.EmojiReactionDialog;
import com.example.finalchatapp.R;
import com.example.finalchatapp.EmojiReactionDialog;
import com.example.finalchatapp.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_IMAGE_SENT = 3;
    private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;

    private static final String TAG = "MessageAdapter";

    private Context context;
    private List<Message> messageList;
    private String currentUserId;
    private String chatId;

    public MessageAdapter(Context context, List<Message> messageList, String currentUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }


    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        switch (viewType) {
            case VIEW_TYPE_SENT:
                view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageHolder(view);
            case VIEW_TYPE_RECEIVED:
                view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
                return new ReceivedMessageHolder(view);
            case VIEW_TYPE_IMAGE_SENT:
                view = LayoutInflater.from(context).inflate(R.layout.item_message_image_sent, parent, false);
                return new SentImageHolder(view);
            case VIEW_TYPE_IMAGE_RECEIVED:
                view = LayoutInflater.from(context).inflate(R.layout.item_message_image_received, parent, false);
                return new ReceivedImageHolder(view);
            default:
                Log.e(TAG, "Unknown view type: " + viewType);
                view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            Message message = messageList.get(position);


            switch (holder.getItemViewType()) {
                case VIEW_TYPE_SENT:
                    ((SentMessageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_RECEIVED:
                    ((ReceivedMessageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_IMAGE_SENT:
                    ((SentImageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_IMAGE_RECEIVED:
                    ((ReceivedImageHolder) holder).bind(message);
                    break;
            }


            setupReactions(holder, message);


            holder.itemView.setOnLongClickListener(v -> {
                showReactionPicker(message.getMessageId());
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "Error binding message at position " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {

            return message.getType() == Message.TYPE_IMAGE ? VIEW_TYPE_IMAGE_SENT : VIEW_TYPE_SENT;
        } else {

            return message.getType() == Message.TYPE_IMAGE ? VIEW_TYPE_IMAGE_RECEIVED : VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        LinearLayout reactionsContainer;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
            reactionsContainer = itemView.findViewById(R.id.reactions_container);
        }

        void bind(Message message) {
            messageText.setText(message.getContent());
            timeText.setText(formatTime(message.getTimestamp()));
        }
    }

    static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        LinearLayout reactionsContainer;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
            reactionsContainer = itemView.findViewById(R.id.reactions_container);
        }

        void bind(Message message) {
            messageText.setText(message.getContent());
            timeText.setText(formatTime(message.getTimestamp()));
        }
    }

    static class SentImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView timeText;
        LinearLayout reactionsContainer;

        SentImageHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_message);
            timeText = itemView.findViewById(R.id.time_text);
            reactionsContainer = itemView.findViewById(R.id.reactions_container);
        }

        void bind(Message message) {

            Glide.with(itemView.getContext())
                    .load(message.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_error_placeholder)
                    .into(imageView);

            timeText.setText(formatTime(message.getTimestamp()));


            imageView.setOnClickListener(v -> {

                Log.d(TAG, "Image clicked: " + message.getImageUrl());
            });
        }
    }

    static class ReceivedImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView timeText;
        LinearLayout reactionsContainer;

        ReceivedImageHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_message);
            timeText = itemView.findViewById(R.id.time_text);
            reactionsContainer = itemView.findViewById(R.id.reactions_container);
        }

        void bind(Message message) {

            Glide.with(itemView.getContext())
                    .load(message.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_error_placeholder)
                    .into(imageView);

            timeText.setText(formatTime(message.getTimestamp()));


            imageView.setOnClickListener(v -> {

                Log.d(TAG, "Image clicked: " + message.getImageUrl());
            });
        }
    }

    private static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }



    private void setupReactions(RecyclerView.ViewHolder holder, Message message) {
        LinearLayout reactionsContainer;


        if (holder instanceof SentMessageHolder) {
            reactionsContainer = ((SentMessageHolder) holder).reactionsContainer;
        } else if (holder instanceof ReceivedMessageHolder) {
            reactionsContainer = ((ReceivedMessageHolder) holder).reactionsContainer;
        } else if (holder instanceof SentImageHolder) {
            reactionsContainer = ((SentImageHolder) holder).reactionsContainer;
        } else if (holder instanceof ReceivedImageHolder) {
            reactionsContainer = ((ReceivedImageHolder) holder).reactionsContainer;
        } else {
            return;
        }


        reactionsContainer.removeAllViews();

        Map<String, List<String>> reactions = message.getReactions();
        if (reactions == null || reactions.isEmpty()) {
            reactionsContainer.setVisibility(View.GONE);
            return;
        }

        reactionsContainer.setVisibility(View.VISIBLE);


        for (Map.Entry<String, List<String>> entry : reactions.entrySet()) {
            String emoji = entry.getKey();
            int count = entry.getValue().size();

            TextView reactionView = new TextView(context);
            reactionView.setText(emoji + " " + count);
            reactionView.setPadding(8, 4, 8, 4);
            reactionView.setBackground(ContextCompat.getDrawable(context, R.drawable.reaction_background));
            reactionView.setTextSize(12);


            if (message.hasUserReacted(emoji, currentUserId)) {
                reactionView.setTextColor(ContextCompat.getColor(context, R.color.black));
            }


            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 8, 0);
            reactionView.setLayoutParams(params);


            reactionView.setOnClickListener(v -> {
                toggleReaction(message.getMessageId(), emoji);
            });

            reactionsContainer.addView(reactionView);
        }
    }

    private void showReactionPicker(String messageId) {
        EmojiReactionDialog dialog = EmojiReactionDialog.newInstance(messageId);
        dialog.setOnEmojiSelectedListener(this::toggleReaction);
        dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "EmojiPicker");
    }

    private void toggleReaction(String messageId, String emoji) {
        if (chatId == null) {
            Log.e(TAG, "Chat ID is not set");
            return;
        }

        // Find the message
        for (int i = 0; i < messageList.size(); i++) {
            Message message = messageList.get(i);
            if (message.getMessageId().equals(messageId)) {

                if (message.hasUserReacted(emoji, currentUserId)) {
                    message.removeReaction(emoji, currentUserId);
                } else {
                    message.addReaction(emoji, currentUserId);
                }


                notifyItemChanged(i);


                updateMessageReactionsInFirestore(messageId, message.getReactions());

                break;
            }
        }
    }

    private void updateMessageReactionsInFirestore(String messageId, Map<String, List<String>> reactions) {
        FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update("reactions", reactions)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reaction updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update reaction: " + e.getMessage());
                });
    }
}