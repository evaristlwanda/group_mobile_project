package com.example.myapplication;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private static final String CHAT_COLLECTION = "chats";
    private static final String MESSAGES_COLLECTION = "messages";

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private TextInputEditText messageEditText;

    private FirebaseFirestore firestore;
    private String currentUserId;
    private String recipientUserId; // The ID of the user you're chatting with

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Get the current user's ID
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            // Handle the case where the user is not logged in
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize UI elements
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        TextInputLayout messageInputLayout = view.findViewById(R.id.messageInputLayout);
        messageEditText = view.findViewById(R.id.messageEditText);
        MaterialButton sendButton = view.findViewById(R.id.sendButton);

        // Set up RecyclerView
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecyclerView.setAdapter(chatAdapter);

        // Get the recipient's user ID (replace with how you get it in your app)
        recipientUserId = "RECIPIENT_USER_ID"; // Replace with the actual recipient's ID

        // Set up the send button click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        // Listen for new messages
        listenForMessages();

        return view;
    }

    private void sendMessage() {
        String messageText = Objects.requireNonNull(messageEditText.getText()).toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Create a new message object
        ChatMessage message = new ChatMessage(currentUserId, recipientUserId, messageText, new Date());

        // Get a reference to the chat document (create it if it doesn't exist)
        DocumentReference chatRef = firestore.collection(CHAT_COLLECTION).document(getChatId(currentUserId, recipientUserId));

        // Add the message to the "messages" sub_collection
        chatRef.collection(MESSAGES_COLLECTION)
                .add(message)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Message sent successfully");
                        messageEditText.setText(""); // Clear the input field
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error sending message", e);
                        Toast.makeText(getContext(), "Error sending message", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void listenForMessages() {
        // Get a reference to the chat document
        DocumentReference chatRef = firestore.collection(CHAT_COLLECTION).document(getChatId(currentUserId, recipientUserId));

        // Listen for changes in the "messages" sub_collection
        chatRef.collection(MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot document = dc.getDocument();
                            ChatMessage message = document.toObject(ChatMessage.class);
                            switch (dc.getType()) {
                                case ADDED:
                                    Log.d(TAG, "New message: " + message.getMessage());
                                    chatMessages.add(message);
                                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1); // Scroll to the bottom
                                    break;
                                case MODIFIED:
                                    Log.d(TAG, "Modified message: " + message.getMessage());
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "Removed message: " + message.getMessage());
                                    break;
                            }
                        }
                    }
                });
    }

    // Helper method to generate a unique chat ID based on two user IDs
    private String getChatId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    // ChatMessage data class
    public static class ChatMessage {
        private String senderId;
        private String receiverId;
        private String message;
        private Date timestamp;

        public ChatMessage() {
            // Required empty constructor for Firestore
        }

        public ChatMessage(String senderId, String receiverId, String message, Date timestamp) {
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getSenderId() {
            return senderId;
        }

        public String getReceiverId() {
            return receiverId;
        }

        public String getMessage() {
            return message;
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    // ChatAdapter class
    public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

        private final List<ChatMessage> messages;

        public ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.bind(message);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        public class ChatViewHolder extends RecyclerView.ViewHolder {
            private final TextView senderTextView;
            private final TextView messageTextView;
            private final TextView timeTextView;
            public ChatViewHolder(View itemView) {
                super(itemView);
                senderTextView = itemView.findViewById(R.id.senderTextView);
                messageTextView = itemView.findViewById(R.id.messageTextView);
                timeTextView = itemView.findViewById(R.id.timeTextView);
            }

            public void bind(ChatMessage message) {
                // Determine if the message is sent or received
                boolean isSent = message.getSenderId().equals(currentUserId);

                // Set the sender's email or "You" if it's the current user
                if (isSent) {
                    senderTextView.setText("You");
                } else {
                    // Fetch sender's email from Firestore (you might need a separate collection for users)
                    fetchSenderEmail(message.getSenderId(), senderTextView);
                }

                messageTextView.setText(message.getMessage());

                // Format and set the timestamp
                CharSequence formattedTime = DateFormat.format("hh:mm a", message.getTimestamp());
                timeTextView.setText(formattedTime);

                // Align the message to the right if sent, left if received
                if (isSent) {
                    // Align to the right (you might need to adjust the layout in item_chat_message.xml)
                    messageTextView.setBackgroundResource(R.drawable.rounded_message_background_sent);
                    senderTextView.setVisibility(View.GONE);
                    timeTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                    ((ViewGroup.MarginLayoutParams) messageTextView.getLayoutParams()).setMarginEnd(0);
                    ((ViewGroup.MarginLayoutParams) messageTextView.getLayoutParams()).setMarginStart(100);
                    ((ViewGroup.MarginLayoutParams) timeTextView.getLayoutParams()).setMarginEnd(0);
                    ((ViewGroup.MarginLayoutParams) timeTextView.getLayoutParams()).setMarginStart(100);

                } else {
                    // Align to the left
                    messageTextView.setBackgroundResource(R.drawable.rounded_message_background);
                    senderTextView.setVisibility(View.VISIBLE);
                    timeTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    ((ViewGroup.MarginLayoutParams) messageTextView.getLayoutParams()).setMarginStart(0);
                    ((ViewGroup.MarginLayoutParams) messageTextView.getLayoutParams()).setMarginEnd(100);
                    ((ViewGroup.MarginLayoutParams) timeTextView.getLayoutParams()).setMarginStart(0);
                    ((ViewGroup.MarginLayoutParams) timeTextView.getLayoutParams()).setMarginEnd(100);
                }
            }
        }
    }
    private void fetchSenderEmail(String senderId, TextView senderTextView) {
        firestore.collection("users").document(senderId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String senderEmail = documentSnapshot.getString("email");
                            senderTextView.setText(senderEmail);
                        } else {
                            senderTextView.setText(R.string.unknown_sender);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error fetching sender email", e);
                        senderTextView.setText(R.string.unknown_sender);
                    }
                });
    }
}