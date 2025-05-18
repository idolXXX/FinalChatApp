package com.example.finalchatapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalchatapp.R;
import com.example.finalchatapp.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_IMAGE_SENT = 3;
    private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;

    private static final String TAG = "MessageAdapter";

    private Context context;
    private List<Message> messageList;
    private String currentUserId;

    public MessageAdapter(Context context, List<Message> messageList, String currentUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
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
            // Current user's messages
            return message.getType() == Message.TYPE_IMAGE ? VIEW_TYPE_IMAGE_SENT : VIEW_TYPE_SENT;
        } else {
            // Other user's messages
            return message.getType() == Message.TYPE_IMAGE ? VIEW_TYPE_IMAGE_RECEIVED : VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
        }

        void bind(Message message) {
            messageText.setText(message.getContent());
            timeText.setText(formatTime(message.getTimestamp()));
        }
    }

    static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
        }

        void bind(Message message) {
            messageText.setText(message.getContent());
            timeText.setText(formatTime(message.getTimestamp()));
        }
    }

    // New view holder for sent image messages
    static class SentImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView timeText;

        SentImageHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_message);
            timeText = itemView.findViewById(R.id.time_text);
        }

        void bind(Message message) {
            // Load image with Glide
            Glide.with(itemView.getContext())
                    .load(message.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_error_placeholder)
                    .into(imageView);

            timeText.setText(formatTime(message.getTimestamp()));

            // Optional: Set click listener for full-screen view
            imageView.setOnClickListener(v -> {
                // You could implement full-screen image viewing here
                Log.d(TAG, "Image clicked: " + message.getImageUrl());
            });
        }
    }

    // New view holder for received image messages
    static class ReceivedImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView timeText;

        ReceivedImageHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_message);
            timeText = itemView.findViewById(R.id.time_text);
        }

        void bind(Message message) {
            // Load image with Glide
            Glide.with(itemView.getContext())
                    .load(message.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_error_placeholder)
                    .into(imageView);

            timeText.setText(formatTime(message.getTimestamp()));

            // Optional: Set click listener for full-screen view
            imageView.setOnClickListener(v -> {
                // You could implement full-screen image viewing here
                Log.d(TAG, "Image clicked: " + message.getImageUrl());
            });
        }
    }

    private static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}