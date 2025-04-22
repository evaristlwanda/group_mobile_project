package com.example.myapplication;

import  com.example.myapplication.R.*;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RequiresExtension;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, true);

        // Get a reference to the RecyclerView
        RecyclerView myRecyclerView = view.findViewById(id.myRecyclerView);
        // Set the layout manager
        myRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create the data
        List<ListItem> items = new ArrayList<>();
        items.add(new ListItem("Buddy 1", R.drawable.buddy1));
        items.add(new ListItem("Buddy 2", R.drawable.buddy3));
        items.add(new ListItem("Buddy 3", R.drawable.buddy));
        // Add more items here

        // Create and set the adapter
        ListAdapter adapter = new ListAdapter(items);
        myRecyclerView.setAdapter(adapter);

        MaterialButton findBuddiesButton = view.findViewById(R.id.findBuddiesButton);
        findBuddiesButton.setOnClickListener(v -> {
            // Handle button click
        });

        return view;
    }
}