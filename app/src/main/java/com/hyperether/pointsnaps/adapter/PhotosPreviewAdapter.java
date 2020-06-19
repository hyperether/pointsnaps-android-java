package com.hyperether.pointsnaps.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.hyperether.pointsnaps.R;
import com.hyperether.pointsnaps.ui.UserViewModel;
import com.hyperether.pointsnapssdk.repository.db.UserData;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

public class PhotosPreviewAdapter extends RecyclerView.Adapter<PhotosPreviewAdapter.PhotosViewHolder> {

    private Context context;
    private List<UserData> userData;

    private UserViewModel userViewModel;

    public PhotosPreviewAdapter(Context context, List<UserData> userData) {
        this.context = context;
        this.userData = userData;

        userViewModel = ViewModelProviders.of((FragmentActivity) context).get(UserViewModel.class);
    }

    @NonNull
    @Override
    public PhotosViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.photo_item, parent, false);
        return new PhotosViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotosViewHolder holder, int position) {
        Glide.with(context).load(userData.get(position).getmImagePath()).into(holder.image);

        holder.deleteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userViewModel.delete(userData.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return userData.size();
    }

    public class PhotosViewHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private ImageButton deleteImage;

        public PhotosViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.photo_adapter_preview);
            deleteImage = itemView.findViewById(R.id.photo_adapter_delete);
        }
    }
}
