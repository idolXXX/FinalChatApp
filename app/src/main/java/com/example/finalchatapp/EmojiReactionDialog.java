package com.example.finalchatapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.example.finalchatapp.R;

public class EmojiReactionDialog extends BottomSheetDialogFragment {

    private static final String[] COMMON_EMOJIS = {"👍", "❤️", "😂", "😮", "😥", "👏", "🔥", "🎉"};
    private String messageId;
    private OnEmojiSelectedListener listener;

    public interface OnEmojiSelectedListener {
        void onEmojiSelected(String messageId, String emoji);
    }

    public static EmojiReactionDialog newInstance(String messageId) {
        EmojiReactionDialog dialog = new EmojiReactionDialog();
        Bundle args = new Bundle();
        args.putString("messageId", messageId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            messageId = getArguments().getString("messageId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_emoji_picker, container, false);

        GridLayout emojiGrid = view.findViewById(R.id.emoji_grid);

        for (String emoji : COMMON_EMOJIS) {
            TextView emojiView = new TextView(getContext());
            emojiView.setText(emoji);
            emojiView.setTextSize(24);
            emojiView.setPadding(24, 24, 24, 24);

            emojiView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEmojiSelected(messageId, emoji);
                }
                dismiss();
            });

            emojiGrid.addView(emojiView);
        }

        return view;
    }

    public void setOnEmojiSelectedListener(OnEmojiSelectedListener listener) {
        this.listener = listener;
    }
}