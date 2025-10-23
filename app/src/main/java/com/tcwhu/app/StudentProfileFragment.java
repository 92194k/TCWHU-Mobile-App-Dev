package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class StudentProfileFragment extends Fragment {

    private Button buttonLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment, which contains our button
        View view = inflater.inflate(R.layout.fragment_student_profile, container, false);

        // Find the logout button from the layout
        buttonLogout = view.findViewById(R.id.buttonLogout);

        // Set the click listener for the logout button
        buttonLogout.setOnClickListener(v -> {
            // Sign the current user out of Firebase
            FirebaseAuth.getInstance().signOut();

            // Create an Intent to go back to the main LandingActivity
            Intent intent = new Intent(getActivity(), LandingActivity.class);

            // These flags clear the entire app history, so the user can't press 'back'
            // to get into the dashboard after logging out.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Finish the current activity (StudentHomeActivity)
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }
}