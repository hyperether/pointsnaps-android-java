package com.hyperether.pointsnaps.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hyperether.pointsnaps.R;
import com.hyperether.pointsnaps.adapter.PhotosPreviewAdapter;
import com.hyperether.pointsnaps.ui.UserViewModel;

public class PhotosPreviewFragment extends DialogFragment {

    private RecyclerView recyclerView;
    private PhotosPreviewAdapter adapter;
    private UserViewModel userViewModel;
    private ImageView closeDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_photos_preview, container, false);

        recyclerView = rootView.findViewById(R.id.photo_review_recycler);
        userViewModel = ViewModelProviders.of(getActivity()).get(UserViewModel.class);
        userViewModel.getActiveCollectionLiveData().observe(this, data -> {
            adapter = new PhotosPreviewAdapter(getActivity(), data.getImageDataList());
            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        });

        closeDialog = rootView.findViewById(R.id.close_photos_preview_dialog);
        closeDialog.setOnClickListener(v -> getDialog().dismiss());
        return rootView;
    }
}
