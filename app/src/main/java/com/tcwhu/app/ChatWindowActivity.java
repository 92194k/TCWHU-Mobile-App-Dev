package com.tcwhu.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.widget.Toast;



// NOTE: Placeholder classes (Message, ChatDataManager, ChatActionHandler, etc.) are assumed to exist.

public class ChatWindowActivity extends AppCompatActivity
        implements ChatWindowCallbacks, ChatFileUploader.FileUploadCompletionListener {

    public static final String EXTRA_OTHER_USER_ID = "otherUserId";

    // --- UI Components ---
    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;
    private List<Message> messageList;

    private EditText inputMessage;
    private ImageButton buttonSend, buttonPaperclip, buttonMic;
    private TextView textChatUsername, textChatYear, textChattingWithBanner;
    private ProgressBar chatProgressBar;
    private LinearLayout inputContainer;
    private LinearLayout adminWarningActions;
    private Button buttonConfirmWarning, buttonAppealWarning;

    // NEW: Voice Message UI Elements
    private TextView voiceTimerTextView;
    private LinearLayout voiceRecordingOverlay;
    private TextView slideToCancelText;

    // --- Data and Logic Components ---
    private String currentUserId;
    private String otherUserId;
    private String chatId;
    private boolean isChatWithAdmin = false;
    private boolean amIStudent = true;

    private ChatDataManager dataManager;
    private ChatFileUploader fileUploader;
    private ChatActionHandler actionHandler;

    private VoiceMessageController voiceMessageController;
    // NOTE: RECORD_AUDIO is the only required permission here. WRITE_EXTERNAL_STORAGE is deprecated.
    private static final int RECORD_PERMISSION_CODE = 101;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeUserAndChatIds();
        if (currentUserId == null || otherUserId == null) {
            Toast.makeText(this, "Error: User or partner ID missing. Exiting.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_chat_window);

        initUI();

        messageList = new ArrayList<>();

        dataManager = new ChatDataManager(currentUserId, otherUserId, chatId, messageList, this);
        actionHandler = new ChatActionHandler(this, currentUserId, otherUserId, chatId, dataManager, this);
        fileUploader = new ChatFileUploader(this, chatId, this, this);

        messagesAdapter = new MessagesAdapter(messageList, currentUserId, actionHandler);

        setupRecyclerView();


        dataManager.loadChatPartnerDetails();
        dataManager.markChatAsRead();
        dataManager.setupAdminWarningUI(isChatWithAdmin, amIStudent);

        setupFilePickerLauncher();
        setupActionListeners();

        voiceMessageController = new VoiceMessageController(
                this,
                fileUploader,
                buttonMic,
                voiceTimerTextView,
                voiceRecordingOverlay,
                inputContainer,
                slideToCancelText
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dataManager != null) {
            dataManager.listenForMessages();
        }
    }


    private void initializeUserAndChatIds() {
        String adminIdFromIntent = getIntent().getStringExtra("ADMIN_USER_ID");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (adminIdFromIntent != null && !adminIdFromIntent.isEmpty()) {
            amIStudent = false;
            isChatWithAdmin = true;
            currentUserId = adminIdFromIntent;
            otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        } else {
            amIStudent = true;
            if (currentUser == null) {
                return;
            }
            currentUserId = currentUser.getUid();
            otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        }

        if (currentUserId != null && otherUserId != null) {
            chatId = (currentUserId.compareTo(otherUserId) < 0)
                    ? currentUserId + "_" + otherUserId
                    : otherUserId + "_" + currentUserId;
        }
    }

    private void initUI() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        inputMessage = findViewById(R.id.inputMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonPaperclip = findViewById(R.id.buttonPaperclip);
        buttonMic = findViewById(R.id.buttonMic);
        chatProgressBar = findViewById(R.id.chatProgressBar);

        textChatUsername = findViewById(R.id.textChatUsername);
        textChatYear = findViewById(R.id.textChatYear);
        textChattingWithBanner = findViewById(R.id.textChattingWithBanner);

        inputContainer = findViewById(R.id.inputContainer);
        adminWarningActions = findViewById(R.id.adminWarningActions);
        buttonConfirmWarning = findViewById(R.id.buttonConfirmWarning);
        buttonAppealWarning = findViewById(R.id.buttonAppealWarning);

        voiceTimerTextView = findViewById(R.id.voiceTimerTextView);
        voiceRecordingOverlay = findViewById(R.id.voiceRecordingOverlay);
        slideToCancelText = findViewById(R.id.slideToCancelText);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String warningTemplate = getIntent().getStringExtra("WARNING_TEMPLATE");
        if (warningTemplate != null && !warningTemplate.isEmpty()) {
            inputMessage.setText(warningTemplate);
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesAdapter = new MessagesAdapter(messageList, currentUserId, actionHandler);
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedFileUri = result.getData().getData();
                        if (selectedFileUri != null) {
                            fileUploader.handleFilePickerResult(selectedFileUri);
                        }
                    }
                }
        );
    }


    private void setupActionListeners() {
        buttonSend.setOnClickListener(v -> {
            String text = inputMessage.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;

            dataManager.sendMessage("text", text, null, 0);

            inputMessage.setText("");
        });

        buttonPaperclip.setOnClickListener(v ->
                fileUploader.openFilePicker(filePickerLauncher)
        );

        // DELEGATE: Voice Recording Listener is delegated to the controller
        buttonMic.setOnTouchListener((v, event) -> {
            // Check permission, request if needed, then allow the controller to handle the touch event
            if (checkRecordingPermissions()) {
                if (voiceMessageController != null) {
                    return voiceMessageController.onTouch(v, event);
                }
            }
            return false;
        });

        buttonConfirmWarning.setOnClickListener(v -> {
            dataManager.sendConfirmationMessage("[User has read and acknowledged the warning]");
            adminWarningActions.setVisibility(View.GONE);
        });

        buttonAppealWarning.setOnClickListener(v -> actionHandler.showAppealDialog(adminWarningActions));
    }

    // --- Voice Recording Logic (CRITICAL: Single Permission Check) ---
    private boolean checkRecordingPermissions() {
        // Only need RECORD_AUDIO permission for modern Android versions and internal storage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_PERMISSION_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, re-simulate the touch event to start recording immediately
            // NOTE: This re-simulation is often complex and error-prone; a better approach is to
            // inform the user to tap the mic button again, but we attempt the easy fix here.
            Snackbar.make(findViewById(android.R.id.content), "Permission granted. Tap mic again.", Snackbar.LENGTH_SHORT).show();
            // Since we can't reliably re-simulate the touch from here, we rely on the user tapping again.
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Microphone permission denied. Cannot record voice.", Snackbar.LENGTH_LONG).show();
        }
    }

    // --- Lifecycle and Menu (Delegation) ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isChatWithAdmin) {
            getMenuInflater().inflate(R.menu.menu_chat_options, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_delete_conversation) {
            actionHandler.showDeleteDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataManager != null) {
            dataManager.cleanupListener();
        }
        if (messagesAdapter != null) {
            messagesAdapter.cleanup();
        }
        if (voiceMessageController != null) {
            voiceMessageController.cleanup();
        }
    }

    // --- ChatWindowCallbacks Implementation (Activity updates UI) ---

    @Override
    public void onMessageListUpdated(List<Message> newMessages, boolean shouldScrollToBottom) {
        messagesAdapter.notifyDataSetChanged();

        boolean atBottom = !messagesRecyclerView.canScrollVertically(1);

        if (shouldScrollToBottom || atBottom && newMessages.size() > 0) {
            messagesRecyclerView.scrollToPosition(newMessages.size() - 1);
        }
    }

    @Override
    public void onChatPartnerDetailsLoaded(String nickname, String yearLevel, String role) {
        textChatUsername.setText(nickname != null ? nickname : "Unknown");

        if (yearLevel != null && !"admin".equals(role)) {
            textChatYear.setText(yearLevel);
            textChatYear.setVisibility(View.VISIBLE);
        } else {
            textChatYear.setVisibility(View.GONE);
        }

        textChattingWithBanner.setText("You are chatting with " + (nickname != null ? nickname : "Unknown"));
    }

    @Override
    public void onProgressVisibilityChanged(int visibility) {
        chatProgressBar.setVisibility(visibility);
    }

    @Override
    public void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }

    @Override
    public void finishActivity() {
        finish();
    }

    @Override
    public void setAdminWarningActionsVisibility(int visibility) {
        adminWarningActions.setVisibility(visibility);
    }

    @Override
    public void setInputContainerVisibility(int visibility) {
        inputContainer.setVisibility(visibility);
    }

    @Override
    public void onFileUploadCompleted(String type, String content, String fileName, long durationMillis) {
        dataManager.sendMessage(type, content, fileName, durationMillis);
    }
}