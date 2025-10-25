package com.tcwhu.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private LinearLayout chipContainer;

    private String currentFilterYear = "all";
    private String currentSearchQuery = "";
    private String currentUserId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Nullable @Override
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
        loadAllVerifiedStudents();
    }

    private void setupRecyclerView() {
        adapter = new StudentFinderAdapter(filteredStudentList, this);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);
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

    private void setupYearFilters(LinearLayout container) {
        container.removeAllViews();
        List<String> years = Arrays.asList("all", "1st Year", "2nd Year", "3rd Year", "4th Year");
        for (String year : years) {
            Button button = new Button(getContext());
            button.setText(year.equals("all") ? "All Years" : year);
            button.setBackgroundResource(R.drawable.bg_search_input);
            button.setTextColor(Color.BLACK);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 8, 0);
            button.setLayoutParams(params);

            button.setOnClickListener(v -> {
                currentFilterYear = year;
                for (int i = 0; i < container.getChildCount(); i++) {
                    container.getChildAt(i).setBackgroundResource(R.drawable.bg_search_input);
                    ((Button)container.getChildAt(i)).setTextColor(Color.BLACK);
                }
                button.setBackgroundColor(Color.parseColor("#6A0DAD"));
                button.setTextColor(Color.WHITE);
                applyFilters();
            });
            container.addView(button);
        }
        if (container.getChildCount() > 0) {
            Button firstButton = (Button) container.getChildAt(0);
            firstButton.setBackgroundColor(Color.parseColor("#6A0DAD"));
            firstButton.setTextColor(Color.WHITE);
        }
    }

    private void loadAllVerifiedStudents() {
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
                            if (currentUserId != null && !student.getUserId().equals(currentUserId)) {
                                allStudentList.add(student);
                            }
                        }
                        applyFilters();
                    } else {
                        Toast.makeText(getContext(), "Error loading students. Check Firestore Index.", Toast.LENGTH_SHORT).show();
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
}