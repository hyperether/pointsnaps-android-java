package com.hyperether.pointsnaps.ui.fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hyperether.pointsnaps.App;
import com.hyperether.pointsnaps.R;
import com.hyperether.pointsnaps.databinding.FragmentLocationBinding;
import com.hyperether.pointsnaps.manager.Connectivity;
import com.hyperether.pointsnaps.ui.UserViewModel;
import com.hyperether.pointsnaps.utils.Constants;
import com.hyperether.toolbox.HyperLog;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for search user location (address) on map
 *
 * @author Slobodan Prijic
 * @version 1.1 - 03/07/2020
 */
public class LocationFragment extends ToolbarFragment implements OnMapReadyCallback {

    public static final String TAG = Constants.LOCATION_FRAGMENT_TAG;
    private static final float MIN_ACCURACY = 100.0f;
    private static final float MIN_DISTANCE_THRESHOLD = 20.0f;

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private Marker mMarker = null;
    private ProgressDialog mProgressDialog;

    private LocationCallback locationCallback;
    private boolean requestLocationUpdates;

    private Location lastLocation;
    private String lastAddress = "";
    private Double longitude = 0.0;
    private Double latitude = 0.0;

    private UserViewModel userViewModel;
    private FragmentLocationBinding binding;

    public static LocationFragment newInstance() {
        return new LocationFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_location, container, false);
        View view = binding.getRoot();
        setupToolbar(view, getResources().getString(R.string.title_activity_location));
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        userViewModel = ViewModelProviders.of(getActivity()).get(UserViewModel.class);
        userViewModel.getActiveCollectionLiveData().observe(this, data -> {
            binding.setData(data.getCollectionData());
        });

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setMessage(getResources().getString(R.string.locating));
        mProgressDialog.show();

        /*
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.map);
         */
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        binding.buttonLocationOk.setOnClickListener(buttonOkListener);
        binding.buttonLocationCancel.setOnClickListener(buttonCancelListener);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        if (lastLocation != null) {
                            if (location.getAccuracy() < MIN_ACCURACY) {
                                float distance = location.distanceTo(lastLocation);
                                if (distance > MIN_DISTANCE_THRESHOLD ||
                                        location.getAccuracy() < lastLocation.getAccuracy()) {
                                    lastLocation = location;
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                    userViewModel.updateLocation(lastAddress, longitude, latitude);
                                    updateAddress(location);
                                    updateMap(location);
                                }
                            }
                        } else {
                            lastLocation = location;
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            userViewModel.updateLocation(lastAddress, longitude, latitude);
                            updateAddress(location);
                            updateMap(location);
                        }
                    }
                }
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startRequestingLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocationServices.getFusedLocationProviderClient(getActivity())
                .removeLocationUpdates(locationCallback);
        requestLocationUpdates = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        checkLocationChanged(binding.addressView);
//        if (mapFragment != null)
//            getFragmentManager().beginTransaction().remove(mapFragment).commit();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_login).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }

    private View.OnClickListener buttonOkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (latitude == 0.0 && longitude == 0.0) {
                binding.addressView.setText(R.string.no_location);
            }
            dismiss();
        }
    };

    private View.OnClickListener buttonCancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            binding.addressView.setText("");
            dismiss();
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @SuppressLint("MissingPermission")
    private synchronized void updateMap(Location location) {
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
            mProgressDialog.dismiss();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 16.0f));
            if (mMarker != null) {
                mMarker.remove();
            }

            int height = 70;
            int width = 70;
            BitmapDrawable bmDraw = (BitmapDrawable) getResources().getDrawable(R.drawable.app_icon);
            Bitmap b = bmDraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                    .draggable(true));

        }
    }

    /*
     *  Map initialisation
     *
     * */

    @SuppressLint("MissingPermission")
    private synchronized void startRequestingLocationUpdates() {
        if (!requestLocationUpdates) {
            requestLocationUpdates = true;

            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(1000);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.getFusedLocationProviderClient(getActivity())
                    .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateAddress(Location location) {
        if (location == null)
            return;

        if (!Connectivity.isConnected())
            return;

        Context context = App.getInstance().getApplicationContext();
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1);

            if (addresses != null) {
                Address address = addresses.get(0);
                String sAddress = "";
                if (address.getThoroughfare() != null && address.getFeatureName() != null) {
                    sAddress += address.getThoroughfare() + " " + address.getFeatureName();
                } else {
                    sAddress += getResources().getString(R.string.unknown_road);
                }
                if (address.getLocality() != null) {
                    sAddress += ", " + address.getLocality();
                }

                binding.addressView.setText(sAddress);
                userViewModel.updateLocation(sAddress, location.getLongitude(),
                        location.getLatitude());
                lastAddress = sAddress;
            }
        } catch (IOException e) {
            HyperLog.getInstance().e(TAG, "updateLocation", e.getMessage());
        }
    }

    private void checkLocationChanged(EditText address) {
        if (!lastAddress.equals(address.getText().toString())) {
            userViewModel.updateLocation(address.getText().toString(), longitude, latitude);
        }
    }
}