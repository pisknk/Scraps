package com.playpass.scraps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class LibraryFragment extends Fragment {

    private RecyclerView libraryRecyclerView;
    private TextView emptyLibraryText;

    public LibraryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        libraryRecyclerView = view.findViewById(R.id.library_recycler_view);
        emptyLibraryText = view.findViewById(R.id.empty_library_text);

        // TODO: Set up RecyclerView adapter and load library items
        
        // For now, show the empty view since we don't have any items
        showEmptyView(true);
    }

    private void showEmptyView(boolean isEmpty) {
        if (isEmpty) {
            libraryRecyclerView.setVisibility(View.GONE);
            emptyLibraryText.setVisibility(View.VISIBLE);
        } else {
            libraryRecyclerView.setVisibility(View.VISIBLE);
            emptyLibraryText.setVisibility(View.GONE);
        }
    }
} 