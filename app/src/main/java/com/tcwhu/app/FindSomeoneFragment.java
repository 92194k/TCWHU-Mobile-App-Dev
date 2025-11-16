package com.tcwhu.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FindSomeoneFragment extends Fragment implements StudentFinderAdapter.OnChatStartListener {

    private RecyclerView recyclerView;
    private StudentFinderAdapter adapter;
    private List<Student> allStudentList;
    private List<Student> filteredStudentList;
    private FirebaseFirestore db;
    private TextView emptyView;
    private TextInputEditText inputSearch;
    private ChipGroup chipContainer;

    private String currentFilterYear = "all";
    private String currentSearchQuery = "";
    private String currentUserId;

    private List<String> blockedUsersList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find_someone, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        inputSearch = view.findViewById(R.id.inputSearch);
        chipContainer = view.findViewById(R.id.chipContainer);

        allStudentList = new ArrayList<>();
        filteredStudentList = new ArrayList<>();

        setupRecyclerView();
        setupSearchListener();
        setupYearFilters(chipContainer);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCurrentUserProfile();
    }

    private void setupRecyclerView() {
        if (recyclerView == null) {
            Log.e("FindSomeoneFragment", "RecyclerView not found!");
            return;
        }
        adapter = new StudentFinderAdapter(filteredStudentList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
    }

    private void setupSearchListener() {
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupYearFilters(ChipGroup chipGroup) {
        chipGroup.removeAllViews();
        List<String> years = Arrays.asList("all", "1st Year", "2nd Year", "3rd Year", "4th Year");

        for (String year : years) {
            Chip chip = new Chip(getContext());
            chip.setText(year.equals("all") ? "All Years" : year);
            chip.setCheckable(true);
            chip.setTag(year);
            chipGroup.addView(chip);
        }

        if (chipGroup.getChildCount() > 0) {
            ((Chip) chipGroup.getChildAt(0)).setChecked(true);
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                ((Chip) group.getChildAt(0)).setChecked(true);
                currentFilterYear = "all";
            } else {
                int checkedId = checkedIds.get(0);
                Chip selectedChip = group.findViewById(checkedId);
                currentFilterYear = (String) selectedChip.getTag();
            }
            applyFilters();
        });
    }

    private void loadCurrentUserProfile() {
        if (currentUserId == null) {
            loadAllVerifiedStudents(); // No user, just load students
            return;
        }
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Student currentUser = documentSnapshot.toObject(Student.class);
                        if (currentUser != null && currentUser.getBlockedUsers() != null) {
                            blockedUsersList = currentUser.getBlockedUsers();
                        }
                    }
                    loadAllVerifiedStudents();
                })
                .addOnFailureListener(e -> {
                    loadAllVerifiedStudents();
                });
    }

    private void loadAllVerifiedStudents() {
        // --- CRITICAL FIX: Use the simple query ---
        // We will filter out the admin in the code, which is safer and won't crash.
        db.collection("users")
                .whereEqualTo("isVerified", true)
                .whereEqualTo("isBanned", false)
                .whereEqualTo("isSuspended", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allStudentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Student student = document.toObject(Student.class);
                            student.setUserId(document.getId());

                            // --- CRITICAL FIX: Filter out admin role in code ---
                            // This safely handles 'null' roles
                            if ("admin".equals(student.getRole())) {
                                continue; // Skip this user, it's the admin
                            }

                            if (currentUserId != null
                                    && !student.getUserId().equals(currentUserId)
                                    && !blockedUsersList.contains(student.getUserId())) {
                                allStudentList.add(student);
                            }
                        }
                        applyFilters();
                    } else {
                        Toast.makeText(getContext(), "Error loading students.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFilters() {
        filteredStudentList.clear();

        List<Student> searchResults = allStudentList.stream()
                .filter(student ->
                        (student.getNickname() != null && student.getNickname().toLowerCase().contains(currentSearchQuery)) ||
                                (student.getInterests() != null && student.getInterests().toLowerCase().contains(currentSearchQuery)))
                .filter(student ->
                        currentFilterYear.equals("all") || (student.getYearLevel() != null && student.getYearLevel().equals(currentFilterYear)))
                .collect(Collectors.toList());

        filteredStudentList.addAll(searchResults);
        adapter.notifyDataSetChanged();
        checkIfEmpty();
    }

    private void checkIfEmpty() {
        if (filteredStudentList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onChatStart(Student student) {
        if (getActivity() == null || student == null || student.getUserId() == null) {
            Toast.makeText(getContext(), "Error: Cannot start chat.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), ChatWindowActivity.class);
        intent.putExtra(ChatWindowActivity.EXTRA_OTHER_USER_ID, student.getUserId());
        startActivity(intent);
    }

    @Override
    public void onSkip(int currentPosition) {
        if (recyclerView == null) return;
        int nextPosition = currentPosition + 1;
        if (nextPosition < adapter.getItemCount()) {
            recyclerView.smoothScrollToPosition(nextPosition);
        } else {
            Toast.makeText(getContext(), "You've reached the end of the list!", Toast.LENGTH_SHORT).show();
        }
    }
}