package com.hyperether.pointsnaps.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.hyperether.pointsnaps.R;
import com.hyperether.pointsnaps.databinding.ActivityMainBinding;
import com.hyperether.pointsnaps.manager.Connectivity;
import com.hyperether.pointsnaps.manager.FragmentHandler;
import com.hyperether.pointsnaps.ui.UserViewModel;
import com.hyperether.pointsnaps.ui.fragment.PhotosPreviewFragment;
import com.hyperether.pointsnaps.utils.Constants;
import com.hyperether.pointsnapssdk.repository.SharedPref;
import com.hyperether.pointsnapssdk.repository.db.CollectionData;
import com.hyperether.pointsnapssdk.repository.db.ImageData;
import com.hyperether.pointsnapssdk.repository.db.SnapData;
import com.hyperether.pointsnapssdk.repository.db.State;
import com.hyperether.toolbox.permission.OnPermissionRequest;
import com.hyperether.toolbox.permission.PermissionManager;
import com.hyperether.toolbox.storage.HyperFileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Base activity
 *
 * @author Slobodan Prijic
 * @version 1.1 - 24/08/2023
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private ProgressDialog progressDialog;

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
                        userViewModel.uploadData(snapData);
                    }
                }
        });
        userViewModel.getUploadProgressBarLiveData().observe(this, state -> {
            if (state) {
                showProgress();
            } else {
                dismissProgress();
            }
        });
        userViewModel.getUploadErrorLiveData().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                alertDialog(getString(R.string.error), message);
            } else {
                alertDialog(getString(R.string.error), getString(R.string.upload_error_service));
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            List<String> list = new ArrayList<>();
            list.add(Manifest.permission.READ_MEDIA_IMAGES);
            PermissionManager.getInstance().getPermissions(MainActivity.this,
                    Constants.TAG_CODE_PERMISSION_EXTERNAL_STORAGE_LOAD,
                    new OnPermissionRequest() {
                        @Override
                        public void onGranted(int code) {
                            openImagePicker();
                        }

                        @Override
                        public void onDenied(int code) {

                        }
                    }, list);
        } else {
            openImagePicker();
        }
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
     * Load image from gallery
     */
    private void openImagePicker() {
        ImagePicker.with(this)
                .crop()                    //Crop image(Optional), Check Customization for more option
                .compress(1024)            //Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
                .start();
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
        if (resultCode == Activity.RESULT_OK) {
            Uri tempUri = data.getData();
            String path = HyperFileManager.getFilePathFromUri(getApplicationContext(),
                    tempUri,
                    getApplicationContext().getCacheDir());
            addMultiplePhotos(path, data.getData().toString());
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
