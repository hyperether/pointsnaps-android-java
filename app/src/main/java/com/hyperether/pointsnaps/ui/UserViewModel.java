package com.hyperether.pointsnaps.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.hyperether.pointsnapssdk.repository.db.CollectionData;
import com.hyperether.pointsnapssdk.repository.db.ImageData;
import com.hyperether.pointsnapssdk.repository.db.Repository;
import com.hyperether.pointsnapssdk.repository.db.SnapData;

import java.util.List;

public class UserViewModel extends AndroidViewModel {

    private Repository repository;
    private int collectionId;
    private LiveData<SnapData> snapDataLiveData;

    public UserViewModel(@NonNull Application application) {
        super(application);
        repository = Repository.getInstance(application);
        snapDataLiveData = repository.getActiveCollectionLiveData();
    }

    public int getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(int collectionId) {
        this.collectionId = collectionId;
    }

    public void insert(CollectionData data) {
        repository.insert(data);
    }

    public void delete(CollectionData data) {
        repository.delete(data);
    }

    public void insert(ImageData data) {
        data.collectionId = collectionId;
        repository.insert(data);
    }

    public void update(ImageData data) {
        repository.update(data);
    }

    public void delete(ImageData data) {
        repository.delete(data);
    }

    public LiveData<SnapData> getActiveCollectionLiveData() {
        return snapDataLiveData;
    }

    public LiveData<List<SnapData>> getUploadListLiveData() {
        return repository.getUploadListLiveData();
    }

    public void updateLocation(String address, double longitude, double latitude) {
        repository.updateLocation(address, longitude, latitude, collectionId);
    }

    public void updateDescription(String description) {
        repository.updateDescription(description, collectionId);
    }

    public void setCompleted() {
        repository.setCompleted(collectionId);
    }

    public void setReadyForUpload(int collectionId) {
        repository.setCompleted(collectionId);
    }

    public void setUploading(int collectionId) {
        repository.setUploading(collectionId);
    }

    public void setUploaded(int collectionId) {
        repository.setUploaded(collectionId);
    }

    public void setImageUploaded(String imageUrl) {
        repository.setImageUploaded(imageUrl);
    }

    public void setImageUploading(String imageUrl) {
        repository.setImageUploading(imageUrl);
    }

    public void setImageReadyForUpload(String imageUrl) {
        repository.setImageReadyForUpload(imageUrl);
    }

    public void setImageUploadFailed(String imageUrl) {
        repository.setImageUploadFailed(imageUrl);
    }
}

