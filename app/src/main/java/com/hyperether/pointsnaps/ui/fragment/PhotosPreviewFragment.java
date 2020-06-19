package com.hyperether.pointsnaps.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.hyperether.pointsnaps.R;
import com.hyperether.pointsnaps.adapter.PhotosPreviewAdapter;
import com.hyperether.pointsnaps.ui.UserViewModel;
import com.hyperether.pointsnapssdk.repository.db.UserData;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PhotosPreviewFragment extends DialogFragment {

    private RecyclerView recyclerView;
    private PhotosPreviewAdapter adapter;
    private List<UserData> userData;

    private UserViewModel userViewModel;

    private ImageView closeDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_photos_preview, container, false);

        userViewModel = ViewModelProviders.of(getActivity()).get(UserViewModel.class);
        userData = userViewModel.getAllPhotosListData();

        userViewModel.getAllPhotosLiveListData().observe(this, data -> {
            userData.clear();
            userData.addAll(data);
            setupRecyclerView(rootView);
        });

        setupRecyclerView(rootView);

        closeDialog = rootView.findViewById(R.id.close_photos_preview_dialog);
        closeDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        return rootView;
    }

    public void setupRecyclerView(View rootView){
        recyclerView = rootView.findViewById(R.id.photo_review_recycler);
        adapter = new PhotosPreviewAdapter(getActivity(), userData);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
}
