package com.tcwhu.app;

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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

// NOTE: Placeholder classes (Message, Chat, ReportsManagementActivity, MessagesAdapter,
// ChatActionHandler, ChatDataManager, ChatFileUploader, ChatWindowCallbacks) are assumed to exist.

public class ChatWindowActivity extends AppCompatActivity
        implements ChatWindowCallbacks, ChatFileUploader.FileUploadCompletionListener {

    public static final String EXTRA_OTHER_USER_ID = "otherUserId";

    // --- UI Components ---
    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;
    private List<Message> messageList;

    private EditText inputMessage;
    private ImageButton buttonSend, buttonPaperclip;
    private TextView textChatUsername, textChatYear, textChattingWithBanner;
    private ProgressBar chatProgressBar;
    private LinearLayout inputContainer;
    private LinearLayout adminWarningActions;
    private Button buttonConfirmWarning, buttonAppealWarning;

    // --- Data and Logic Components ---
    private String currentUserId;
    private String otherUserId;
    private String chatId;
    private boolean isChatWithAdmin = false;
    private boolean amIStudent = true;

    private ChatDataManager dataManager;
    private ChatFileUploader fileUploader;
    private ChatActionHandler actionHandler;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);

        // --- 1. Basic Initialization (Reduced) ---
        initializeUserAndChatIds();
        if (currentUserId == null || otherUserId == null) {
            Toast.makeText(this, "Error: Setup failed.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- 2. Initialize UI Components ---
        initUI();

        // --- 3. Initialize Helpers and Managers (Dependency Injection) ---
        messageList = new ArrayList<>();

        dataManager = new ChatDataManager(currentUserId, otherUserId, chatId, messageList, this);
        actionHandler = new ChatActionHandler(this, currentUserId, otherUserId, chatId, dataManager, this);
        // Initialize File Uploader, passing 'this' as the FileUploadCompletionListener
        fileUploader = new ChatFileUploader(this, chatId, this, this);

        messagesAdapter = new MessagesAdapter(messageList, currentUserId, actionHandler);

        setupRecyclerView();


        // --- 4. Load Data (Initial Setup) ---
        dataManager.loadChatPartnerDetails();
        dataManager.markChatAsRead();
        dataManager.setupAdminWarningUI(isChatWithAdmin, amIStudent);

        // --- 5. Setup Listeners (Delegating Logic) ---
        setupFilePickerLauncher(); // MUST be set up before setupActionListeners
        setupActionListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dataManager != null) {
            dataManager.listenForMessages();
        }
    }


    // --- Initialization Helpers ---
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
            // NOTE: Assumes ReportsManagementActivity.ADMIN_USER_ID exists
            // isChatWithAdmin = otherUserId != null && otherUserId.equals(ReportsManagementActivity.ADMIN_USER_ID);
        }

        if (currentUserId != null && otherUserId != null) {
            chatId = (currentUserId.compareTo(otherUserId) < 0)
                    ? currentUserId + "_" + otherUserId
                    : otherUserId + "_" + currentUserId;
        }
    }

    private void initUI() {
        // Assume findViewByIds are correctly pointing to R.id...
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        inputMessage = findViewById(R.id.inputMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonPaperclip = findViewById(R.id.buttonPaperclip);
        // findViewById(R.id.buttonMic); // If this exists
        chatProgressBar = findViewById(R.id.chatProgressBar);

        textChatUsername = findViewById(R.id.textChatUsername);
        textChatYear = findViewById(R.id.textChatYear);
        textChattingWithBanner = findViewById(R.id.textChattingWithBanner);

        inputContainer = findViewById(R.id.inputContainer);
        adminWarningActions = findViewById(R.id.adminWarningActions);
        buttonConfirmWarning = findViewById(R.id.buttonConfirmWarning);
        buttonAppealWarning = findViewById(R.id.buttonAppealWarning);

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
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    // NEW: Centralized file picker setup
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

            dataManager.sendMessage("text", text, null);

            inputMessage.setText("");
        });

        // ACTION: Trigger file selection using the launcher
        buttonPaperclip.setOnClickListener(v ->
                fileUploader.openFilePicker(filePickerLauncher)
        );

        buttonConfirmWarning.setOnClickListener(v -> {
            dataManager.sendConfirmationMessage("[User has read and acknowledged the warning]");
            adminWarningActions.setVisibility(View.GONE);
        });

        buttonAppealWarning.setOnClickListener(v -> actionHandler.showAppealDialog(adminWarningActions));

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

        // NOTE: These IDs must exist in R.menu.menu_chat_options
        // if (id == R.id.menu_report) {
        //     actionHandler.showReportDialog();
        //     return true;
        // } else if (id == R.id.menu_block) {
        //     actionHandler.showBlockDialog();
        //     return true;
        // } else
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
        dataManager.cleanupListener();
        // if (messagesAdapter != null) messagesAdapter.cleanup(); // If adapter has cleanup logic
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

    // --- FileUploadCompletionListener Implementation (Crucial: Passes URL/Type to DataManager) ---

    @Override
    public void onFileUploadCompleted(String type, String content, String fileName) {
        dataManager.sendMessage(type, content, fileName);
    }
}