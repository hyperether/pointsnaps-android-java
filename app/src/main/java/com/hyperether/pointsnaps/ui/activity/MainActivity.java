package com.hyperether.pointsnaps.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.hyperether.pointsnaps.R;
import com.hyperether.pointsnaps.databinding.ActivityMainBinding;
import com.hyperether.pointsnaps.manager.Connectivity;
import com.hyperether.pointsnaps.manager.FragmentHandler;
import com.hyperether.pointsnaps.manager.ImageManager;
import com.hyperether.pointsnaps.ui.UserViewModel;
import com.hyperether.pointsnaps.ui.fragment.PhotosPreviewFragment;
import com.hyperether.pointsnaps.utils.Constants;
import com.hyperether.pointsnapssdk.repository.SharedPref;
import com.hyperether.pointsnapssdk.repository.api.ApiResponse;
import com.hyperether.pointsnapssdk.repository.api.Repository;
import com.hyperether.pointsnapssdk.repository.api.request.GetUploadUrlRequest;
import com.hyperether.pointsnapssdk.repository.api.response.GetUploadUrlResponse;
import com.hyperether.pointsnapssdk.repository.db.CollectionData;
import com.hyperether.pointsnapssdk.repository.db.ImageData;
import com.hyperether.pointsnapssdk.repository.db.SnapData;
import com.hyperether.pointsnapssdk.repository.db.State;
import com.hyperether.toolbox.HyperLog;
import com.hyperether.toolbox.graphic.HyperImageProcessing;
import com.hyperether.toolbox.permission.OnPermissionRequest;
import com.hyperether.toolbox.permission.PermissionManager;
import com.hyperether.toolbox.storage.HyperFileManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base activity
 *
 * @author Slobodan Prijic
 * @version 1.0 - 07/21/2015
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private ProgressDialog progressDialog;

    private String myCurrentPhotoPath;
    private int restToUpload = 0;
    private int restToUploadError = 0;

    private UserViewModel userViewModel;

    ActivityMainBinding activityMainBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeTheme();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setupToolbar();

        userViewModel = ViewModelProviders.of(MainActivity.this).get(UserViewModel.class);
        userViewModel.getActiveCollectionLiveData().observe(this, data -> {
            if (data == null) {
                createEmptyData();
            } else {
                userViewModel.setCollectionId(data.getCollectionData().getId());
                setUIOnDatabaseLoaded(data);
                countPhotosData(data.getImageDataList().size());
            }
        });
        userViewModel.getUploadListLiveData().observe(this, snapDataList -> {
            if (snapDataList != null)
                if (Connectivity.isConnected()) {
                    for (SnapData snapData : snapDataList) {
                        userViewModel.setUploading(snapData.getCollectionData().getId());
                        uploadData(snapData);
                    }
                }
        });

        activityMainBinding.llUpload.setOnClickListener(uploadClickListener);
        activityMainBinding.llLocation.setOnClickListener(locationClickListener);
        activityMainBinding.llDescription.setOnClickListener(descriptionClickListener);
        activityMainBinding.llPhoto.setOnClickListener(photoClickListener);
        activityMainBinding.rlPhotoOpen.setOnClickListener(photoClickListener);
        activityMainBinding.openPreview.setOnClickListener(photosPreviewClickListener);

        if (isUserLoggedIn()) {
            getPermissions();
        } else {
            FragmentHandler.getInstance(MainActivity.this).openLoginDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (!isUserLoggedIn()) {
            FragmentHandler.getInstance(MainActivity.this).openLoginDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FragmentHandler.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (!isUserLoggedIn()) {
            menu.findItem(R.id.action_login).setTitle(getString(R.string.menu_login_txt));
        } else {
            menu.findItem(R.id.action_login).setTitle(getString(R.string.menu_logout_txt));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.R
        switch (item.getItemId()) {
            case R.id.action_login:
                FragmentHandler.getInstance(MainActivity.this).openLoginDialog();
                item.setTitle(getString(R.string.menu_login_txt));
                SharedPref.logout();
                return true;
            case R.id.action_light_theme:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                SharedPref.setNightModeState(false);
                restartApp();
                return true;
            case R.id.action_dark_theme:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                SharedPref.setNightModeState(true);
                restartApp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        FragmentHandler.getInstance(this).setStepBack();
        super.onBackPressed();
        if (!isUserLoggedIn()) {
            finish();
        }
    }

    private void initializeTheme() {
        if (SharedPref.loadNightModeState()) {
            setTheme(R.style.darkTheme);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    private void restartApp() {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
    }

    public void alertDialog(String title, String message) {
        runOnUiThread(() ->
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                        .show());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(null);
        final androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.show();
        }

        toolbar.setLogo(ContextCompat.getDrawable(getApplicationContext(), R.drawable.logo));
        View view = toolbar.getChildAt(1);
        view.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pointsnaps.com"));
            startActivity(intent);
        });

        ImageView imageBack = findViewById(R.id.toolbar_image_back);
        imageBack.setVisibility(View.INVISIBLE);

        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setVisibility(View.GONE);
    }

    private OnClickListener photoClickListener = v -> openPhotoDialog();

    private OnClickListener photosPreviewClickListener = v -> openPhotosPreviewDialog();

    private OnClickListener locationClickListener = v -> {
        List<String> list = new ArrayList<>();
        list.add(Manifest.permission.ACCESS_FINE_LOCATION);
        list.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        PermissionManager.getInstance().getPermissions(MainActivity.this,
                Constants.TAG_CODE_PERMISSION_LOCATION,
                new OnPermissionRequest() {
                    @Override
                    public void onGranted(int code) {
                        //FragmentHandler.getInstance(MainActivity.this).startLocationFragment();
                        FragmentHandler.getInstance(MainActivity.this).openLocationDialog();
                    }

                    @Override
                    public void onDenied(int code) {

                    }
                }, list);
    };

    private OnClickListener descriptionClickListener = v ->
            FragmentHandler.getInstance(MainActivity.this).openWriteDialog();

    private OnClickListener uploadClickListener = v -> {
        userViewModel.setCompleted();
    };

    private void uploadData(SnapData data) {
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

            showProgress();
            Repository.getInstance().getUploadUrl(SharedPref.getToken(), request, new ApiResponse() {
                @Override
                public void onSuccess(Object response) {
                    GetUploadUrlResponse getUploadUrlResponse = (GetUploadUrlResponse) response;
                    uploadToS3(imageDataList, getUploadUrlResponse);
                    dismissProgress();
                    userViewModel.setUploaded(data.getCollectionData().getId());
                }

                @Override
                public void onError(String message) {
                    if (message != null && !message.isEmpty()) {
                        alertDialog(getString(R.string.error), message);
                    } else {
                        alertDialog(getString(R.string.error), getString(R.string.upload_error_service));
                    }
                    dismissProgress();
                    if ("Wrong locations params".equals(message)) {
                        userViewModel.delete(data.getCollectionData());
                    } else {
                        userViewModel.setReadyForUpload(data.getCollectionData().getId());
                    }
                }
            });
        }
    }

    private void uploadToS3(List<ImageData> imageDataList,
                            GetUploadUrlResponse getUploadUrlResponse) {
        String fileId = getUploadUrlResponse.getFileId();
        List<String> urls = getUploadUrlResponse.getUrls();
        for (int i = 0; i < urls.size(); i++) {
            String getUrl = urls.get(i);
            ImageData imageData = imageDataList.get(i);
            imageData.imageUrl = getUrl;
            userViewModel.update(imageData);
            String filePath = imageData.imagePath;
            String url = getUrl.substring(getUrl.lastIndexOf("/") + 1);
            Bitmap bitmap = HyperImageProcessing.getBitmapRotated(new File(filePath), Constants.PHOTO_WIDTH);

            userViewModel.setImageUploading(url);
            Repository.getInstance().uploadToS3(url, bitmap, fileId, Constants.PHOTO_COMPRESSION, new ApiResponse() {
                @Override
                public void onSuccess(Object response) {
                    userViewModel.setImageUploaded(url);
                }

                @Override
                public void onError(String message) {
                    userViewModel.setImageUploadFailed(url);
                }
            });
        }
    }

    private void dismissProgress() {
        runOnUiThread(() -> {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        });
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.uploading));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    private void createEmptyData() {
        CollectionData data = new CollectionData("", "", 0.0, 0.0, State.ACTIVE.name());
        userViewModel.insert(data);
    }

    private void setUIOnDatabaseLoaded(SnapData data) {
        boolean isLocationSet = data.getCollectionData().getLatitude() != 0.0 &&
                data.getCollectionData().getLongitude() != 0.0;
        // Upload
        if (data.getImageDataList().isEmpty()) {
            activityMainBinding.tvUpload.setText(getString(R.string.take_a_photo));
            activityMainBinding.ivUpload.setImageResource(R.drawable.ic_camera_btn);
            activityMainBinding.llUpload.setOnClickListener(photoClickListener);
        } else if (!isLocationSet) {
            activityMainBinding.tvUpload.setText(getString(R.string.title_activity_location));
            activityMainBinding.ivUpload.setImageResource(R.drawable.ic_location_btn);
            activityMainBinding.llUpload.setOnClickListener(locationClickListener);
        } else if (data.getCollectionData().getDescription().isEmpty()) {
            activityMainBinding.tvUpload.setText(getString(R.string.description_text));
            activityMainBinding.ivUpload.setImageResource(R.drawable.ic_description_btn);
            activityMainBinding.llUpload.setOnClickListener(descriptionClickListener);
        } else {
            activityMainBinding.tvUpload.setText(getString(R.string.upload));
            activityMainBinding.ivUpload.setImageResource(R.drawable.ic_upload);
            activityMainBinding.llUpload.setOnClickListener(uploadClickListener);
        }

        // Photo
        if (data.getImageDataList().isEmpty()) {
            activityMainBinding.ivPhotoOpen.setVisibility(View.GONE);
            activityMainBinding.ibPhotoOpen.setVisibility(View.GONE);
            activityMainBinding.rlPhotoOpen.setVisibility(View.GONE);
            activityMainBinding.openPreview.setVisibility(View.GONE);
            activityMainBinding.llPhoto.setVisibility(View.VISIBLE);
        } else {
            activityMainBinding.ivPhotoOpen.setVisibility(View.VISIBLE);
            activityMainBinding.ibPhotoOpen.setVisibility(View.VISIBLE);
            activityMainBinding.rlPhotoOpen.setVisibility(View.VISIBLE);
            activityMainBinding.openPreview.setVisibility(View.VISIBLE);
            activityMainBinding.llPhoto.setVisibility(View.GONE);
            File imgFile = new File(data.getImageDataList().get(0).imagePath);
            if (imgFile.exists()) {
                Glide.with(MainActivity.this).load(imgFile).into(activityMainBinding.ivPhotoOpen);
            } else {
                // TODO
            }
        }

        // Location
        activityMainBinding.llLocation.setEnabled(!data.getImageDataList().isEmpty() ||
                isLocationSet);
        activityMainBinding.tvLocation.setTextSize(16);
        if (!isLocationSet) {
            activityMainBinding.tvLocation.setText(R.string.title_activity_location);
            activityMainBinding.ivLocation.setImageResource(R.drawable.ic_location);
        } else {
            activityMainBinding.tvLocation.setText(data.getCollectionData().getAddress());
            activityMainBinding.ivLocation.setImageResource(R.drawable.ic_location_done);
        }

        // Description
        activityMainBinding.llDescription.setEnabled(isLocationSet ||
                !data.getCollectionData().getDescription().isEmpty());
        activityMainBinding.tvDescription.setTextSize(16);
        if (data.getCollectionData().getDescription().isEmpty()) {
            activityMainBinding.tvDescription.setText(R.string.description_text);
            activityMainBinding.ivDescription.setImageResource(R.drawable.ic_description);
        } else {
            activityMainBinding.tvDescription.setText(data.getCollectionData().getDescription());
            activityMainBinding.ivDescription.setImageResource(R.drawable.ic_description_done);
        }
    }

    private void openPhotoDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_camera);

        TextView openCamera = dialog.findViewById(R.id.take_a_photo);
        TextView openGallery = dialog.findViewById(R.id.upload_from_gallery);

        openCamera.setOnClickListener(v -> {
            captureClicked();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                List<String> list = new ArrayList<>();
//                list.add(Manifest.permission.READ_MEDIA_IMAGES);
//                PermissionManager.getInstance().getPermissions(MainActivity.this,
//                        Constants.TAG_CODE_PERMISSION_EXTERNAL_STORAGE,
//                        new OnPermissionRequest() {
//                            @Override
//                            public void onGranted(int code) {
//                                captureClicked();
//                            }
//
//                            @Override
//                            public void onDenied(int code) {
//                                HyperLog.getInstance().d(TAG, "openPhotoDialog", "Permission denied with code:" + code);
//                            }
//                        }, list);
//            }
            dialog.dismiss();
        });

        openGallery.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                List<String> list = new ArrayList<>();
                list.add(Manifest.permission.READ_MEDIA_IMAGES);
                PermissionManager.getInstance().getPermissions(MainActivity.this,
                        Constants.TAG_CODE_PERMISSION_EXTERNAL_STORAGE_LOAD,
                        new OnPermissionRequest() {
                            @Override
                            public void onGranted(int code) {
                                loadClicked();
                            }

                            @Override
                            public void onDenied(int code) {

                            }
                        }, list);
            } else {
                loadClicked();
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    /*
     * Method for handling permissions results provided by user
     *
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.getInstance().processPermission(requestCode, permissions, grantResults);
    }

    /**
     * Capture image
     */
    private void captureClicked() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        Context context = getApplicationContext();
        File photoFile = null;
        try {
            // Create the File where the photo should go
            photoFile = ImageManager.createImageFile();
            // Save a file: path for use with ACTION_VIEW intents
            myCurrentPhotoPath = "file:" + photoFile.getAbsolutePath();
        } catch (IOException e) {
            // Error occurred while creating the File
            HyperLog.getInstance().e(TAG, "captureClicked", e.getMessage());
        }
        // Continue only if the File was successfully created
        if (intent.resolveActivity(context.getPackageManager()) != null && photoFile != null) {
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(context, context.getPackageName() +
                        Constants.FILE_PROVIDER_NAME, photoFile);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(photoFile);
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, Constants.RESULT_CAPTURE_IMG);
        }
    }

    /**
     * Load image from gallery
     */
    private void loadClicked() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent = Intent.createChooser(intent, getString(R.string.select_file));
        startActivityForResult(intent, Constants.RESULT_LOAD_IMG);
    }

    private void imagePreview(String currentPhotoPath) {
        File imgFile = new File(currentPhotoPath);
        if (imgFile.exists()) {
            Glide.with(MainActivity.this).load(imgFile).into(activityMainBinding.ivPhotoOpen);
        }
    }

    private void addMultiplePhotos(String imagePath, String intentData) {
        ImageData imageData = new ImageData();
        imageData.imageIntentData = intentData;
        imageData.imagePath = imagePath;
        userViewModel.insert(imageData);
    }

    private void openPhotosPreviewDialog() {
        PhotosPreviewFragment dialog = new PhotosPreviewFragment();
        dialog.show(getSupportFragmentManager(), "PhotosPreviewDialog");
    }

    private void countPhotosData(int imageListSize) {
        activityMainBinding.ibPhotoCount.setText(Integer.toString(imageListSize));
        if ((imageListSize) >= 6) {
            activityMainBinding.ibPhotoOpen.setVisibility(View.GONE);
        } else {
            activityMainBinding.ibPhotoOpen.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.RESULT_CAPTURE_IMG: {
                if (resultCode == Activity.RESULT_OK) {
                    addMultiplePhotos(myCurrentPhotoPath.substring(5), "");
                }
                break;
            }
            case Constants.RESULT_LOAD_IMG: {
                if (resultCode == Activity.RESULT_OK) {
                    Uri tempUri = data.getData();
                    String path = HyperFileManager.getFilePathFromUri(getApplicationContext(),
                            tempUri,
                            getApplicationContext().getCacheDir());
                    addMultiplePhotos(path, data.getData().toString());
                }
                break;
            }
            default:
                break;
        }
    }

    private void getPermissions() {
        List<String> list = new ArrayList<>();
        list.add(Manifest.permission.ACCESS_FINE_LOCATION);
        list.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES);
        }
        PermissionManager.getInstance().getPermissions(MainActivity.this,
                Constants.TAG_CODE_PERMISSION_LOCATION,
                new OnPermissionRequest() {
                    @Override
                    public void onGranted(int code) {

                    }

                    @Override
                    public void onDenied(int code) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setCancelable(false)
                                .setTitle(getString(R.string.error))
                                .setMessage(getString(R.string.err_permission_msg))
                                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> MainActivity.this.finish())
                                .setPositiveButton(getString(R.string.ok), (dialog, which) -> getPermissions())
                                .show();
                    }
                }, list);
    }

    private boolean isUserLoggedIn() {
        return !SharedPref.getToken().isEmpty();
    }
}
