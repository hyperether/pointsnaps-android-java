package com.hyperether.pointsnaps.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.hyperether.pointsnapssdk.repository.db.UserData;
import com.hyperether.pointsnapssdk.repository.db.UserRepository;

import java.util.List;

public class UserViewModel extends AndroidViewModel {

    private UserRepository repository;
    private LiveData<List<UserData>> allData;

    public UserViewModel(@NonNull Application application) {
        super(application);
        repository = UserRepository.getInstance(application);
        allData = repository.getLastUserDataLive();
    }

    public void insert(UserData data) {
        repository.insert(data);
    }

    public void delete(UserData data) {
        repository.delete(data);
    }

    public void update(UserData data) {
        repository.update(data);
    }

    public void deleteAllUserData() {
        repository.deleteAllUserData();
    }

    public LiveData<List<UserData>> getLastUserDataLive() {
        return repository.getLastUserDataLive();
    }

    public List<UserData> getAllCompletedDataList() {
        return repository.getAllCompletedDataList();
    }

    public List<UserData> getAllUserDatalist() {
        return repository.getAllUserDataList();
    }

    public UserData getLastRecordData() {
        return repository.getLastRecordData();
    }

    public void updateImage(String imagePath) {
        repository.updateImagePath(imagePath);
    }

    public void updateAddress(String address, double longitude, double latitude) {
        repository.updateAddress(address, longitude, latitude);
    }

    public void deleteImage() {
        repository.deleteImagePath();
    }

    public void updateDescription(String description) {
        repository.updateDescription(description);
    }

    public void updateCompletedState(Boolean completed) {
        repository.updateCompletedState(completed);
    }

    public void updateIntentData(String data) {
        repository.updateIntentData(data);
    }
}

