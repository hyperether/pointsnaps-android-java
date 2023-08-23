package com.hyperether.pointsnaps.ui;

import android.app.Application;
import android.graphics.Bitmap;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hyperether.pointsnaps.utils.Constants;
import com.hyperether.pointsnapssdk.repository.PointSnapsSdk;
import com.hyperether.pointsnapssdk.repository.SharedPref;
import com.hyperether.pointsnapssdk.repository.api.ApiResponse;
import com.hyperether.pointsnapssdk.repository.api.request.GetUploadUrlRequest;
import com.hyperether.pointsnapssdk.repository.api.response.GetUploadUrlResponse;
import com.hyperether.pointsnapssdk.repository.db.CollectionData;
import com.hyperether.pointsnapssdk.repository.db.ImageData;
import com.hyperether.pointsnapssdk.repository.db.Repository;
import com.hyperether.pointsnapssdk.repository.db.SnapData;
import com.hyperether.toolbox.graphic.HyperImageProcessing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UserViewModel extends AndroidViewModel {

    private Repository repository;
    private int collectionId;
    private final LiveData<SnapData> snapDataLiveData;
    private final MutableLiveData<Boolean> uploadProgressBarLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> uploadErrorLiveData = new MutableLiveData<>();

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

    public LiveData<Boolean> getUploadProgressBarLiveData() {
        return uploadProgressBarLiveData;
    }

    public MutableLiveData<String> getUploadErrorLiveData() {
        return uploadErrorLiveData;
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

    public void uploadData(SnapData data) {
        final List<ImageData> imageDataList = data.getImageDataList();
        if (imageDataList != null && !imageDataList.isEmpty()) {

            GetUploadUrlRequest request = new GetUploadUrlRequest();
            List<String> images = new ArrayList<>();
            for (ImageData imageData : imageDataList) {
                String filePath = imageData.imagePath;
                final File file = new File(filePath);
                final String fileName = file.getName().replace(".jpg", "");
                if (request.getFile() == null)
                    request.setFile(fileName);
                final String fileExt = filePath.substring(filePath.lastIndexOf(".") + 1);
                if (request.getExt() == null)
                    request.setExt(fileExt);
                String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExt);
                images.add(type);
            }

            request.setImages(images);
            request.setDescription(data.getCollectionData().getDescription());
            if (data.getCollectionData().getLatitude() > 0
                    && data.getCollectionData().getLongitude() > 0) {
                request.setLat(data.getCollectionData().getLatitude());
                request.setLon(data.getCollectionData().getLongitude());
            }
            if (!data.getCollectionData().getAddress().isEmpty()) {
                request.setAddress(data.getCollectionData().getAddress());
            }

            uploadProgressBarLiveData.postValue(true);
            PointSnapsSdk.getInstance().getUploadUrl(SharedPref.getToken(), request, new ApiResponse() {
                @Override
                public void onSuccess(Object response) {
                    GetUploadUrlResponse getUploadUrlResponse = (GetUploadUrlResponse) response;
                    uploadToS3(imageDataList, getUploadUrlResponse);
                    uploadProgressBarLiveData.postValue(false);
                    setUploaded(data.getCollectionData().getId());
                }

                @Override
                public void onError(String message) {
                    uploadErrorLiveData.postValue(message);
                    uploadProgressBarLiveData.postValue(false);
                    if ("Wrong locations params".equals(message)) {
                        delete(data.getCollectionData());
                    } else {
                        setReadyForUpload(data.getCollectionData().getId());
                    }
                }
            });
        }
    }

    public void uploadToS3(List<ImageData> imageDataList,
                           GetUploadUrlResponse getUploadUrlResponse) {
        String fileId = getUploadUrlResponse.getFileId();
        List<String> urls = getUploadUrlResponse.getUrls();
        for (int i = 0; i < urls.size(); i++) {
            String getUrl = urls.get(i);
            ImageData imageData = imageDataList.get(i);
            imageData.imageUrl = getUrl;
            update(imageData);
            String filePath = imageData.imagePath;
            String url = getUrl.substring(getUrl.lastIndexOf("/") + 1);
            Bitmap bitmap = HyperImageProcessing.getBitmapRotated(new File(filePath), Constants.PHOTO_WIDTH);

            setImageUploading(url);

            PointSnapsSdk.getInstance().uploadToS3(url, bitmap, fileId, Constants.PHOTO_COMPRESSION, new ApiResponse() {
                @Override
                public void onSuccess(Object response) {
                    setImageUploaded(url);
                }

                @Override
                public void onError(String message) {
                    setImageUploadFailed(url);
                }
            });
        }
    }
}

