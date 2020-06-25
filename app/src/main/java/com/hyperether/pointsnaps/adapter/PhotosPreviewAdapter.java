package com.hyperether.pointsnaps.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hyperether.pointsnaps.R;
import com.hyperether.pointsnaps.ui.UserViewModel;
import com.hyperether.pointsnapssdk.repository.db.ImageData;

import java.util.List;

public class PhotosPreviewAdapter extends RecyclerView.Adapter<PhotosPreviewAdapter.PhotosViewHolder> {

    private Context context;
    private List<ImageData> dataList;
    private UserViewModel userViewModel;

    public PhotosPreviewAdapter(Context context, List<ImageData> dataList) {
        this.context = context;
        this.dataList = dataList;

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
        Glide.with(context).load(dataList.get(position).imagePath).into(holder.image);
        holder.deleteImage.setOnClickListener(v -> userViewModel.delete(dataList.get(position)));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
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
