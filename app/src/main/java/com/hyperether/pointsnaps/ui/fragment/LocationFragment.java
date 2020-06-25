package com.hyperether.pointsnaps.ui.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
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
import com.hyperether.pointsnaps.ui.UserViewModel;
import com.hyperether.pointsnaps.utils.Constants;
import com.hyperether.toolbox.HyperLog;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for search user location (address) on map
 *
 * @author Marko Katic
 * @version 1.0 - 07/04/2017
 */
public class LocationFragment extends ToolbarFragment implements OnMapReadyCallback,
        OnRequestPermissionsResultCallback, LocationListener {

    public static final String TAG = Constants.LOCATION_FRAGMENT_TAG;
    private GoogleMap mMap;
    private String addressChagned = "";
    private ProgressDialog mProgressDialog;
    private Location location;
    private Double longitude = 0.0;
    private Double latitude = 0.0;
    private Marker mMarker = null;
    private boolean isMapInitialisationStarted;
    private LocationManager locationManager;
    SupportMapFragment mapFragment;

    //ROOM db
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
        mapFragment.getMapAsync(this);
         */

        mapFragment = (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getActivity().getSystemService(Context
                .LOCATION_SERVICE);

        binding.buttonLocationOk.setOnClickListener(buttonOkListener);
        binding.buttonLocationCancel.setOnClickListener(buttonCancelListener);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapInitialisation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        flushLocationListener();
        checkLocationChanged(binding.addressView);
        if (mapFragment != null)
            getFragmentManager().beginTransaction().remove(mapFragment).commit();
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
        mapInitialisation();
    }

    /*
     *  Map initialisation
     *
     * */

    private synchronized void mapInitialisation() {
        if (!isMapInitialisationStarted) {
            isMapInitialisationStarted = true;

            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(1000);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.getFusedLocationProviderClient(getActivity())
                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            super.onLocationResult(locationResult);
                            LocationServices.getFusedLocationProviderClient(getActivity())
                                    .removeLocationUpdates(this);
                            if (locationResult != null && locationResult.getLocations().size() > 0) {
                                location = locationResult.getLocations().get(0);
                                if (mMap != null && location != null) {
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
                                    updateAddress(location);
                                } else {
                                    // Map is not initialised, so you can start again by setting
                                    // isMapInitialisationStarted to false value
                                    isMapInitialisationStarted = false;
                                }
                            }
                        }
                    }, Looper.getMainLooper());
        }
    }

    private void updateAddress(Location location) {
        if (location != null) {
            Context context = App.getInstance().getApplicationContext();
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);

                if (addresses != null) {
                    Address returnedAddress = addresses.get(0);

                    String locatedAddress = "";
                    String strReturnedAddress;
                    if (returnedAddress.getThoroughfare() == null || returnedAddress.getFeatureName() == null) {
                        strReturnedAddress = getResources().getString(R.string.unknown_road) + " , " + returnedAddress.getLocality();
                    } else if (returnedAddress.getLocality() == null) {
                        strReturnedAddress = getResources().getString(R.string.unknown_road);
                    } else {
                        strReturnedAddress = returnedAddress.getThoroughfare() + " " +
                                returnedAddress.getFeatureName() + ", " + returnedAddress.getLocality();
                        locatedAddress = strReturnedAddress;
                    }

                    binding.addressView.setText(strReturnedAddress);
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    userViewModel.updateLocation(locatedAddress, longitude, latitude);
                    addressChagned = locatedAddress;
                }
            } catch (IOException e) {
                HyperLog.getInstance().e(TAG, "updateLocation", e.getMessage());
            }
        }
    }

    private void checkLocationChanged(EditText address) {
        if (!addressChagned.equals(address.getText().toString())) {
            userViewModel.updateLocation(address.getText().toString(), longitude, latitude);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (this.location == null) {
            this.location = location;
            flushLocationListener();
            mapInitialisation();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER) && location == null) {
            if (isMapInitialisationStarted) {
                // To start mapInitialisation with first known location flag must be set to false
                isMapInitialisationStarted = false;
            }
            mapInitialisation();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /*
     * Removes location updates and clear location management flags
     *
     * */
    private void flushLocationListener() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        isMapInitialisationStarted = false;
    }
}