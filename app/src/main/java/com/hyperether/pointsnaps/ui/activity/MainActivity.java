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
import com.crashlytics.android.Crashlytics;
import com.hyperether.pointsnaps.R;
import com.hyperether.pointsnaps.databinding.ActivityMainBinding;
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
import com.hyperether.pointsnapssdk.repository.db.UserData;
import com.hyperether.toolbox.HyperLog;
import com.hyperether.toolbox.graphic.HyperImageProcessing;
import com.hyperether.toolbox.permission.OnPermissionRequest;
import com.hyperether.toolbox.permission.PermissionManager;
import com.hyperether.toolbox.storage.HyperFileManager;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;

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
        Fabric.with(this, new Crashlytics());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setupToolbar();

        userViewModel = ViewModelProviders.of(MainActivity.this).get(UserViewModel.class);
        List<UserData> users = userViewModel.getAllUserDatalist();
        if (users.isEmpty()) {
            createEmptyData();
        }

        userViewModel.getLastUserDataLive().observe(this, data -> {
            UserData userData = userViewModel.getLastRecordData();
            if (!userData.getmCompleted()) {
                setUIOnDatabaseLoaded(userData);
            }
        });

        userViewModel.getAllPhotosLiveListData().observe(this, data -> {
            countPhotosData();
        });

        for (UserData user : users) {
            if (user.mCompleted) {
                uploadData(user);
            }
        }

        activityMainBinding.llUpload.setOnClickListener(uploadClickListener);
        activityMainBinding.llLocation.setOnClickListener(locationClickListener);
        activityMainBinding.llDescription.setOnClickListener(descriptionClickListener);
        activityMainBinding.llPhoto.setOnClickListener(photoClickListener);
        activityMainBinding.rlPhotoOpen.setOnClickListener(photoClickListener);
        activityMainBinding.openPreview.setOnClickListener(photosPreviewClickListener);

        if (isUserLoggedIn()) {
            getPermissions();
        }

        countPhotosData();
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
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pointsnaps.com"));
                startActivity(intent);
            }
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

    private OnClickListener uploadClickListener = v -> uploadData();

    private void uploadData() {
        if (!isUserLoggedIn()) {
            FragmentHandler.getInstance(MainActivity.this).openLoginDialog();
        } else {
            showProgress();
            for (UserData userData : userViewModel.getAllPhotosListData()) {
                uploadData(userData);
            }
        }
    }

    private void uploadData(UserData data) {
        final String filePath = data.getmImagePath();
        if (filePath != null && !filePath.isEmpty()) {

            final File file = new File(filePath);
            final String fileName = file.getName().replace(".jpg", "");
            final String fileExt = filePath.substring(filePath.lastIndexOf(".") + 1);

            Bitmap bitmap = HyperImageProcessing.getBitmapRotated(new File(filePath), Constants.PHOTO_WIDTH);

            GetUploadUrlRequest request = new GetUploadUrlRequest(fileName, fileExt);
            request.setDescription(data.getmDescription());
            if (data.getmLatitude() > 0 && data.getmLatitude() > 0) {
                request.setLat(data.getmLatitude());
                request.setLon(data.getmLongitude());
            }
            if (!data.getmAddress().isEmpty()) {
                request.setAddress(data.getmAddress());
            }

            Repository.getInstance().getUploadUrl(SharedPref.getToken(), request, new ApiResponse() {
                @Override
                public void onSuccess(Object response) {
                    GetUploadUrlResponse getUploadUrlResponse = (GetUploadUrlResponse) response;
                    uploadToS3(bitmap, getUploadUrlResponse, data);
                }

                @Override
                public void onError(String message) {
                    // TODO: check this logic
                    userViewModel.updateStateToTrue(data.getId());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setUIOnDatabaseLoaded(userViewModel.getAllCompletedDataList().get(0));
                        }
                    });
                    if (userViewModel.getAllCompletedDataList().isEmpty()) {
                        createEmptyData();
                    }
                    restToUploadError = userViewModel.getAllCompletedDataList().size();
                    if (restToUploadError == 1) {
                        alertDialog(getString(R.string.error), getString(R.string.upload_error_service));
                    }
                    dismissProgress();
                }
            });
        }
    }

    private void uploadToS3(Bitmap bitmap, GetUploadUrlResponse getUploadUrlResponse, UserData data) {
        String getUrl = getUploadUrlResponse.getUrl();
        String fileId = getUploadUrlResponse.getFileId();
        String url = getUrl.substring(getUrl.lastIndexOf("/") + 1);

        Repository.getInstance().uploadToS3(url, bitmap, fileId, Constants.PHOTO_COMPRESSION,
                new ApiResponse() {
                    @Override
                    public void onSuccess(Object response) {
                        if (data != null) {
                            if (data.getmImagePath().equals("")) {
                                createEmptyData();
                            }
                            restToUpload = userViewModel.getAllCompletedDataList().size();
                            userViewModel.delete(data);
                        }
                        // 1 empty object is always in db
                        if (restToUpload == 2) {
                            dismissProgress();
                            alertDialog(getString(R.string.upload_title), getString(R.string.uploaded));
                        }
                    }

                    @Override
                    public void onError(String message) {
                        userViewModel.updateStateToTrue(data.getId());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setUIOnDatabaseLoaded(userViewModel.getAllCompletedDataList().get(0));
                            }
                        });
                        if (userViewModel.getAllCompletedDataList().isEmpty()) {
                            createEmptyData();
                        }
                        restToUploadError = userViewModel.getAllCompletedDataList().size();
                        if (restToUploadError == 1) {
                            alertDialog(getString(R.string.error), getString(R.string.upload_error_service));
                        }
                        dismissProgress();
                    }
                });
    }

    private void dismissProgress() {
        runOnUiThread(() -> {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        });
    }

    private void showProgress() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.uploading));
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void createEmptyData() {
        UserData userDataStart = new UserData("", "", "", 0.0, 0.0, false, "");
        userViewModel.insert(userDataStart);
    }

    private void setUIOnDatabaseLoaded(UserData data) {
        // Upload
        if (data.getmImagePath().isEmpty()) {
            activityMainBinding.tvUpload.setText(getString(R.string.take_a_photo));
            activityMainBinding.ivUpload.setImageResource(R.drawable.ic_camera_btn);
            activityMainBinding.llUpload.setOnClickListener(photoClickListener);
        } else if (data.getmAddress().isEmpty()) {
            activityMainBinding.tvUpload.setText(getString(R.string.title_activity_location));
            activityMainBinding.ivUpload.setImageResource(R.drawable.ic_location_btn);
            activityMainBinding.llUpload.setOnClickListener(locationClickListener);
        } else if (data.getmDescription().isEmpty()) {
            activityMainBinding.tvUpload.setText(getString(R.string.description_text));
            activityMainBinding.ivUpload.setImageResource(R.drawable.ic_description_btn);
            activityMainBinding.llUpload.setOnClickListener(descriptionClickListener);
        } else {
            activityMainBinding.tvUpload.setText(getString(R.string.upload));
            activityMainBinding.ivUpload.setImageResource(R.drawable.ic_upload);
            activityMainBinding.llUpload.setOnClickListener(uploadClickListener);
        }

        // Photo
        if (data.getmImagePath().isEmpty()) {
            activityMainBinding.ivPhotoOpen.setVisibility(View.GONE);
            activityMainBinding.ibPhotoOpen.setVisibility(View.GONE);
            activityMainBinding.openPreview.setVisibility(View.GONE);
            activityMainBinding.llPhoto.setVisibility(View.VISIBLE);
        } else {
            activityMainBinding.ivPhotoOpen.setVisibility(View.VISIBLE);
            activityMainBinding.ibPhotoOpen.setVisibility(View.VISIBLE);
            activityMainBinding.openPreview.setVisibility(View.VISIBLE);
            activityMainBinding.llPhoto.setVisibility(View.GONE);
            File imgFile = new File(data.getmImagePath());
            if (imgFile.exists()) {
                Glide.with(MainActivity.this).load(imgFile).into(activityMainBinding.ivPhotoOpen);
            } else {
                // TODO
            }
        }

        // Location
        activityMainBinding.llLocation.setEnabled(!data.getmImagePath().isEmpty() || !data.getmAddress().isEmpty());
        activityMainBinding.tvLocation.setTextSize(16);
        if (data.getmAddress().isEmpty()) {
            activityMainBinding.tvLocation.setText(R.string.title_activity_location);
            activityMainBinding.ivLocation.setImageResource(R.drawable.ic_location);
        } else {
            activityMainBinding.tvLocation.setText(data.getmAddress());
            activityMainBinding.ivLocation.setImageResource(R.drawable.ic_location_done);
        }

        // Description
        activityMainBinding.llDescription.setEnabled(!data.getmAddress().isEmpty() || !data.getmDescription().isEmpty());
        activityMainBinding.tvDescription.setTextSize(16);
        if (data.getmDescription().isEmpty()) {
            activityMainBinding.tvDescription.setText(R.string.description_text);
            activityMainBinding.ivDescription.setImageResource(R.drawable.ic_description);
        } else {
            activityMainBinding.tvDescription.setText(data.getmDescription());
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
            List<String> list = new ArrayList<>();
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            PermissionManager.getInstance().getPermissions(MainActivity.this,
                    Constants.TAG_CODE_PERMISSION_EXTERNAL_STORAGE,
                    new OnPermissionRequest() {
                        @Override
                        public void onGranted(int code) {
                            captureClicked();
                        }

                        @Override
                        public void onDenied(int code) {

                        }
                    }, list);
            dialog.dismiss();
        });

        openGallery.setOnClickListener(v -> {
            List<String> list = new ArrayList<>();
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
        String mDescription = userViewModel.getLastRecordData().getmDescription();
        String mAddress = userViewModel.getLastRecordData().getmAddress();
        double mLongitude = userViewModel.getLastRecordData().getmLongitude();
        double mLatitude = userViewModel.getLastRecordData().getmLatitude();

        UserData userData = new UserData(mDescription, imagePath, mAddress, mLongitude, mLatitude, false, intentData);
        userViewModel.insert(userData);
    }

    private void openPhotosPreviewDialog() {
        PhotosPreviewFragment dialog = new PhotosPreviewFragment();
        dialog.show(getSupportFragmentManager(), "PhotosPreviewDialog");
    }

    private void countPhotosData() {
        List<UserData> userData = userViewModel.getAllPhotosListData();
        activityMainBinding.ibPhotoCount.setText(Integer.toString(userData.size()));
        if ((userData.size()) >= 6) {
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
                    try {
                        String path = HyperFileManager.getFilePathFromUri(getApplicationContext(), tempUri);
                        addMultiplePhotos(path, data.getData().toString());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
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
        list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
