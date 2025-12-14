package com.tcwhu.app;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
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
import android.os.Build;
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
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;

interface DownloadRequestListener {
    void onFileDownloadRequested(String fileUrl, String fileName, String mimeType);
}

public class ChatWindowActivity extends AppCompatActivity
        implements ChatWindowCallbacks, ChatFileUploader.FileUploadCompletionListener, DownloadRequestListener {

    public static final String EXTRA_OTHER_USER_ID = "otherUserId";

    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;
    private List<Message> messageList;

    private EditText inputMessage;
    private ImageButton buttonSend, buttonPaperclip, buttonMic;
    private TextView textChatUsername, textChatYear, textChattingWithBanner;
    private ProgressBar chatProgressBar;
    private LinearLayout inputContainer, adminWarningActions;
    private Button buttonConfirmWarning, buttonAppealWarning;

    private TextView voiceTimerTextView, slideToCancelText;
    private LinearLayout voiceRecordingOverlay;

    private String currentUserId, otherUserId, chatId;
    private boolean isChatWithAdmin = false;
    private boolean amIStudent = true;

    private String otherUserRole = null;
    private FirebaseFirestore db;
    private static final String ADMIN_ROLE = "Super Admin";

    private ChatDataManager dataManager;
    private ChatFileUploader fileUploader;
    private ChatActionHandler actionHandler;
    private VoiceMessageController voiceMessageController;
    private static final int RECORD_PERMISSION_CODE = 101;

    private String pendingDownloadUrl, pendingDownloadName, pendingDownloadMimeType;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<String> requestDownloadPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        initializeUserAndChatIds();
        if (currentUserId == null || otherUserId == null) {
            Toast.makeText(this, "Error: User or partner ID missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_chat_window);
        initUI();

        messageList = new ArrayList<>();
        dataManager = new ChatDataManager(currentUserId, otherUserId, chatId, messageList, this);
        fileUploader = new ChatFileUploader(this, chatId, this, this);
        actionHandler = new ChatActionHandler(this, currentUserId, otherUserId, chatId, dataManager, this, fileUploader);
        messagesAdapter = new MessagesAdapter(messageList, currentUserId, actionHandler, this);

        setupRecyclerView();
        loadOtherUserProfile();

        dataManager.markChatAsRead();
        dataManager.setupAdminWarningUI(isChatWithAdmin, amIStudent);

        setupFilePickerLauncher();
        setupActionListeners();
        setupPermissionLaunchers();

        voiceMessageController = new VoiceMessageController(this, fileUploader, buttonMic, voiceTimerTextView, voiceRecordingOverlay, inputContainer, slideToCancelText);

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(onDownloadComplete, filter);
        }
    }

    private void loadOtherUserProfile() {
        if (otherUserId == null) return;
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Student otherUser = documentSnapshot.toObject(Student.class);
                        if (otherUser != null) otherUserRole = otherUser.getRole();
                    }
                    supportInvalidateOptionsMenu();
                    dataManager.loadChatPartnerDetails();
                })
                .addOnFailureListener(e -> dataManager.loadChatPartnerDetails());
    }

    private void setupPermissionLaunchers() {
        requestDownloadPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) initiateDownload(pendingDownloadUrl, pendingDownloadName, pendingDownloadMimeType);
            else showToast("Permission denied. Cannot save file.", Toast.LENGTH_LONG);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataManager != null) dataManager.cleanupListener();
        if (messagesAdapter != null) messagesAdapter.cleanup();
        if (voiceMessageController != null) voiceMessageController.cleanup();
        try { unregisterReceiver(onDownloadComplete); } catch (Exception ignored) {}
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showToast("Download completed.", Toast.LENGTH_SHORT);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (dataManager != null) dataManager.listenForMessages();
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
            if (currentUser != null) currentUserId = currentUser.getUid();
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
        if (warningTemplate != null && !warningTemplate.isEmpty()) inputMessage.setText(warningTemplate);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedFileUri = result.getData().getData();
                        if (selectedFileUri != null) fileUploader.handleFilePickerResult(selectedFileUri);
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

        buttonPaperclip.setOnClickListener(v -> fileUploader.openFilePicker(filePickerLauncher));

        buttonMic.setOnTouchListener((v, event) -> {
            if (checkRecordingPermissions() && voiceMessageController != null) {
                return voiceMessageController.onTouch(v, event);
            }
            return false;
        });

        buttonConfirmWarning.setOnClickListener(v -> {
            dataManager.sendConfirmationMessage("[User has read and acknowledged the warning]");
            adminWarningActions.setVisibility(View.GONE);
        });

        buttonAppealWarning.setOnClickListener(v -> actionHandler.showAppealDialog(adminWarningActions));
    }

    private boolean checkRecordingPermissions() {
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
            Snackbar.make(findViewById(android.R.id.content), "Permission granted.", Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Permission denied.", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!amIStudent) {
            getMenuInflater().inflate(R.menu.menu_chat_options, menu);
            return true;
        }
        if (otherUserRole == null || !otherUserRole.equals(ADMIN_ROLE)) {
            getMenuInflater().inflate(R.menu.menu_chat_options, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (amIStudent && otherUserRole != null && otherUserRole.equals(ADMIN_ROLE)) return false;

        if (id == R.id.menu_delete_conversation) {
            actionHandler.showDeleteDialog();
            return true;
        } else if (id == R.id.menu_report_user) {
            actionHandler.showReportDialog();
            return true;
        } else if (id == R.id.menu_block_user) {
            actionHandler.showBlockDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMessageListUpdated(List<Message> newMessages, boolean shouldScrollToBottom) {
        messagesAdapter.notifyDataSetChanged();
        if (shouldScrollToBottom && newMessages.size() > 0) {
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

    @Override
    public void onFileDownloadRequested(String fileUrl, String fileName, String mimeType) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            showToast("Error: File link is broken.", Toast.LENGTH_SHORT);
            return;
        }

        if (fileName == null || fileName.isEmpty()) fileName = "TCWHU_File_" + System.currentTimeMillis();

        pendingDownloadUrl = fileUrl;
        pendingDownloadName = fileName;
        pendingDownloadMimeType = mimeType;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestDownloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
        }

        initiateDownload(fileUrl, fileName, mimeType);
    }

    private void initiateDownload(String fileUrl, String fileName, String mimeType) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(fileUrl);
            DownloadManager.Request request = new DownloadManager.Request(uri);

            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            String cleanFileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
            if (!cleanFileName.contains(".")) cleanFileName += ".bin";

            request.setTitle(cleanFileName);
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cleanFileName);

            downloadManager.enqueue(request);
            showToast("Downloading " + cleanFileName + "...", Toast.LENGTH_SHORT);

        } catch (Exception e) {
            Log.e("Downloader", "Download failed: " + e.getMessage());
            showToast("Download failed.", Toast.LENGTH_LONG);
        }
    }
}