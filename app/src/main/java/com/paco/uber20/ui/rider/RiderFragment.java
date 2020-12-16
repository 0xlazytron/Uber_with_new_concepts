package com.paco.uber20.ui.rider;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.paco.uber20.Callback.IFirebaseDriverInfoListener;
import com.paco.uber20.Callback.IFirebaseFailedListener;
import com.paco.uber20.Common;
import com.paco.uber20.Model.AnimationModel;
import com.paco.uber20.Model.DriverGeoModel;
import com.paco.uber20.Model.EventBus.SelectedPlaceEvent;
import com.paco.uber20.Model.GeoQueryModel;
import com.paco.uber20.Model.driverModel;
import com.paco.uber20.R;
import com.paco.uber20.Remote.IGoogleAPI;
import com.paco.uber20.Remote.RetrofitClient;
import com.paco.uber20.RequestDriver;
import com.paco.uber20.ui.rider.RiderViewModel;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class RiderFragment extends Fragment implements OnMapReadyCallback,
        IFirebaseFailedListener, IFirebaseDriverInfoListener {
    @BindView(R.id.rider_panel)
    SlidingUpPanelLayout slidingUpPanelLayout;
    @BindView(R.id.txt_welcome)
    TextView txt_welcome;
    private AutocompleteSupportFragment autocompleteSupportFragment;
    private GoogleMap mMap;
    private String cityName;
    private RiderViewModel riderViewModel;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    SupportMapFragment riderFragment;
    DatabaseReference onlineRef, currentUserRef, driversLocationRef;
    GeoFire geoFire;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleAPI iGoogleAPI;

    //Driver Limiter
    private double distance = 1.0;
    private static final double LIMIT_RANGE = 10.0;
    private Location previousLocation, currentLocation;
    private boolean firstTime = true;

    //Listeners
    IFirebaseDriverInfoListener iFirebaseDriverInfoListener;
    IFirebaseFailedListener iFirebaseFailedListener;
    ValueEventListener onlineValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if (dataSnapshot.exists() && currentUserRef != null)
                currentUserRef.onDisconnect().removeValue();
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Snackbar.make(riderFragment.getView(),
                    databaseError.getMessage(),
                    Snackbar.LENGTH_LONG).show();
        }
    };


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (currentUserRef != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            geoFire.removeLocation(FirebaseAuth.getInstance().getCurrentUser().getUid());
            onlineRef.removeEventListener(onlineValueEventListener);
        }
    }

    @Override
    public void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerOnlineSystem();
    }

    private void registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        riderViewModel = new ViewModelProvider(this).get(RiderViewModel.class);
        View root = inflater.inflate(R.layout.fragment_rider, container, false);
        init();
        initViews(root);

        riderFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.ridermap);
        riderFragment.getMapAsync(this);

        return root;
    }

    private void initViews(View root) {
        ButterKnife.bind(this,root);
        Common.setWelcomeMessage(txt_welcome);
    }

    public void init() {
        Places.initialize(getContext(),getString(R.string.google_maps_key));
        autocompleteSupportFragment = (AutocompleteSupportFragment)getChildFragmentManager().findFragmentById(R.id.auto_complete_fragment);
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID,Place.Field.ADDRESS,Place.Field.NAME,Place.Field.LAT_LNG));
        autocompleteSupportFragment.setHint(getString(R.string.where_to));
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onPlaceSelected(@NonNull Place place) {
//                Snackbar.make(getView(),""+place.getLatLng(),Snackbar.LENGTH_LONG).show();
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            LatLng origin = new LatLng(location.getLatitude(),location.getLongitude());
                            LatLng destination = new LatLng(place.getLatLng().latitude,place.getLatLng().longitude);
                            startActivity(new Intent(getContext(), RequestDriver.class));
                            EventBus    .getDefault().postSticky(new SelectedPlaceEvent(origin,destination));
                        });
            }

            @Override
            public void onError(@NonNull Status status) {
                Snackbar.make(getView(),""+status.getStatusMessage(),Snackbar.LENGTH_LONG).show();
            }
        });
        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);

        iFirebaseFailedListener = this;
        iFirebaseDriverInfoListener = this;
        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected");
        driversLocationRef = FirebaseDatabase.getInstance().getReference(Common.RIDERS_LOCATION_REFERENCES);
        currentUserRef = FirebaseDatabase.getInstance().getReference(Common.RIDERS_LOCATION_REFERENCES)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        geoFire = new GeoFire(driversLocationRef);
        registerOnlineSystem();
        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(10f); //50m
        locationRequest.setInterval(5000); //15sec
        locationRequest.setFastestInterval(3000); //10 sec
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                Toast.makeText(getContext(), "" + newPosition, Toast.LENGTH_SHORT).show();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));
                if (firstTime) {
                    previousLocation = currentLocation = locationResult.getLastLocation();
                    firstTime = false;
                    SetRestricPlacesInCountry(locationResult.getLastLocation());
                } else {
                    previousLocation = currentLocation;
                    currentLocation = locationResult.getLastLocation();
                }
                if (previousLocation.distanceTo(currentLocation) / 1000 <= LIMIT_RANGE)//notOver Range
                {
                    loadAvailabelDrivers();
                } else {
                    //do nothing
                }
                //set New Location
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                        new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                locationResult.getLastLocation().getLongitude()),
                        (key, error) -> {
                            if (error != null) {
                                Snackbar.make(riderFragment.getView(), error.getMessage(), Snackbar.LENGTH_LONG).show();
                            } else {
                                Snackbar.make(riderFragment.getView(), "Rider is online!", Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
        };
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        loadAvailabelDrivers();
    }

    private void SetRestricPlacesInCountry(Location location) {
        try {
            Geocoder geocoder = new Geocoder(getContext(),Locale.getDefault());
            List<Address> addressesList  = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            if(addressesList.size()>0){
                autocompleteSupportFragment.setCountry(addressesList.get(0).getCountryCode());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @SuppressLint("MissingPermission")
    private void loadAvailabelDrivers() {
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> {
                    Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();
                }).addOnSuccessListener(location -> {
            //Load All Drivers In city
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addressList;
            try {
                addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if(addressList.size()>0) {
                    cityName = addressList.get(0).getLocality();
                }
                if(!TextUtils.isEmpty(cityName)){
                    //Query To get Driver Location From Db
                    DatabaseReference driver_location_ref = FirebaseDatabase.getInstance()
                            .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                            .child(cityName);
                    GeoFire gf = new GeoFire(driver_location_ref);
                    GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(
                                    location.getLatitude(), location.getLongitude()),
                            distance);
                    geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                        @Override
                        public void onKeyEntered(String key, GeoLocation location) {
                            Common.driversFound.add(new DriverGeoModel(key, location));

                        }

                        @Override
                        public void onKeyExited(String key) {

                        }

                        @Override
                        public void onKeyMoved(String key, GeoLocation location) {

                        }

                        @Override
                        public void onGeoQueryReady() {
                            if (distance <= LIMIT_RANGE) {
                                distance++;
                                loadAvailabelDrivers();//continue search in new Distance
                            } else {
                                distance = 1.0;
                                addDriverMarker();
                            }


                        }

                        @Override
                        public void onGeoQueryError(DatabaseError error) {
                            Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_LONG).show();
                        }
                    });
                    //Listen to new Driver in city & range
                    driver_location_ref.addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                            //have new Driver
                            GeoQueryModel geoQueryModel = dataSnapshot.getValue(GeoQueryModel.class);
                            GeoLocation geoLocation = new GeoLocation(geoQueryModel.getL().get(0),
                                    geoQueryModel.getL().get(1));
                            DriverGeoModel driverGeoModel = new DriverGeoModel(dataSnapshot.getKey(),
                                    geoLocation);
                            Location newDriverLocation = new Location("");
                            newDriverLocation.setLatitude(geoLocation.latitude);
                            newDriverLocation.setLongitude(geoLocation.longitude);
                            float newDistance = location.distanceTo(newDriverLocation)/1000; //in KM
                            if(newDistance<=LIMIT_RANGE){
                                findDriverByKey(driverGeoModel); //If Driver in Range, add to map

                            }
                        }

                        @Override
                        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        }

                        @Override
                        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }else{
                    Snackbar.make(getView(),getString(R.string.city_name_empty),Snackbar.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();


            }


        });
    }

    private void addDriverMarker() {
        if (Common.driversFound.size() > 0) {
            Observable.fromIterable(Common.driversFound)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(driverGeoModel -> {
                        findDriverByKey(driverGeoModel);
                    }, throwable -> {
                        Snackbar.make(getView(), throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                    }, () -> {
                    });

        } else {
            Snackbar.make(getView(), getString(R.string.driver_not_found), Snackbar.LENGTH_LONG).show();
        }
    }

    private void findDriverByKey(DriverGeoModel driverGeoModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.DRIVER_INFO_REFERENCE)
                .child(driverGeoModel.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChildren()) {
                            driverGeoModel.setDriverInfoModel(dataSnapshot.getValue(driverModel.class));
                            iFirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel);
                        } else {
                            iFirebaseFailedListener.onFirebaseLoadFailed(getString(R.string.not_found_with_key)
                                    + driverGeoModel.getKey());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        iFirebaseFailedListener.onFirebaseLoadFailed(databaseError.getMessage());

                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(getContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getContext(),
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        mMap.setOnMyLocationButtonClickListener(() -> {
                            fusedLocationProviderClient.getLastLocation()
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), " " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnSuccessListener((Location location) -> {
                                        if (location != null) {
                                            LatLng driverLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 18f));
                                            Toast.makeText(getContext(), "Location " + driverLocation, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Snackbar.make(riderFragment.getView(), "Location Null!", Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                            return true;
                        });
                        View locationButton = ((View) riderFragment.getView().findViewById(Integer.parseInt("1"))
                                .getParent()).findViewById(Integer.parseInt("2"));
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        params.setMargins(0, 0, 0, 250);

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(getContext(), "PERMISSION " + permissionDeniedResponse.getPermissionName() + " " + "WAS DENIED!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
        mMap.getUiSettings().setZoomControlsEnabled(true);
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_maps_style));
            if (!success)
                Log.e("[ERROR] =>", "MAP STYLE PARSING ERROR");
        } catch (Resources.NotFoundException x) {
            Log.e("[ERROR] =>", x.getMessage());
        }
    }

    @Override
    public void onFirebaseLoadFailed(String messgae) {
        Snackbar.make(getView(), messgae, Snackbar.LENGTH_LONG).show();
    }
    @Override
    public void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel) {
        //if already have marker with this key, doesn't set again
        if (!Common.markerList.containsKey(driverGeoModel.getKey())) {
            Common.markerList.put(driverGeoModel.getKey(),
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(driverGeoModel.getGeoLocation().latitude,
                                    driverGeoModel.getGeoLocation().longitude))
                            .flat(true)
                            .title(Common.buildName(driverGeoModel.getDriverInfoModel().getName()))
                            .snippet(driverGeoModel.getDriverInfoModel().getPhone())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.drvcar))));
            if (!TextUtils.isEmpty(cityName)) {
                DatabaseReference driverLocation = FirebaseDatabase.getInstance()
                        .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                        .child(cityName)
                        .child(driverGeoModel.getKey());
                driverLocation.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.hasChildren()) {
                            if (Common.markerList.get(driverGeoModel.getKey()) != null) {
                                Common.markerList.get(driverGeoModel.getKey()).remove(); //remove marker
                                Common.markerList.remove(driverGeoModel.getKey()); //remove marker from hash map
                                Common.driverLocationSubscribe.remove(driverGeoModel.getKey()); //Remove Driver Info Too
                                driverLocation.removeEventListener(this);

                            }
                        } else {
                            if (Common.markerList.get(driverGeoModel.getKey()) != null){
                                GeoQueryModel geoQueryModel = dataSnapshot.getValue(GeoQueryModel.class);
                                AnimationModel animationModel = new AnimationModel(false,geoQueryModel);
                                if(Common.driverLocationSubscribe.get(driverGeoModel.getKey())!=null){
                                    Marker currentMarker = Common.markerList.get(driverGeoModel.getKey());
                                    AnimationModel oldPostiton = Common.driverLocationSubscribe.get(driverGeoModel.getKey());
                                    String from = new StringBuilder()
                                            .append(oldPostiton.getGeoQueryModel().getL().get(0))
                                            .append("")
                                            .append(oldPostiton.getGeoQueryModel().getL().get(1))
                                            .toString();
                                    String to = new StringBuilder()
                                            .append(animationModel.getGeoQueryModel().getL().get(0))
                                            .append("")
                                            .append(animationModel.getGeoQueryModel().getL().get(1))
                                            .toString();
                                    moveMarkerAnimation(driverGeoModel.getKey(),animationModel,currentMarker,from,to);

                                }else{
                                    Common.driverLocationSubscribe.put(driverGeoModel.getKey(),animationModel);
                                }
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Snackbar.make(getView(), databaseError.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });

            }
        } else {

        }
    }

    private void moveMarkerAnimation(String key, AnimationModel animationModel, Marker currentMarker, String from, String to) {
        if(!animationModel.isRun()){
            //Request API
            compositeDisposable.add(iGoogleAPI.getDirections("driving",
                    "less_driving",
                    from,to,getString(R.string.google_api_key))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(returnResult->{
                        Log.d("API_RETURN",returnResult);
                        try{
                            JSONObject jsonObject = new JSONObject(returnResult);
                            JSONArray jsonArray = jsonObject.getJSONArray("routes");
                            for(int i = 0; i<jsonArray.length(); i++){
                                JSONObject route = jsonArray.getJSONObject(i);
                                JSONObject poly = route.getJSONObject("overview_polyline");
                                String polyline = poly.getString("points");
                                // polylinelist = Common.decodePoly(polyline);
                                animationModel.setPolylinelist(Common.decodePoly(polyline));

                            }
                            //Moving Driver Car
//                    handler = new Handler();
//                    index  = -1;
//                    next = 1;
                            animationModel.setIndex(-1);
                            animationModel.setNext(1);
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    if (animationModel.getPolylinelist()!=null && animationModel.getPolylinelist().size() > 1) {
                                        if (animationModel.getIndex() <animationModel.getPolylinelist().size() - 2) {
//                                    index++;
                                            animationModel.setIndex(animationModel.getIndex()+1);
//                                    next = index + 1;
                                            animationModel.setNext(animationModel.getIndex()+1);
//                                    start = polylinelist.get(index);
                                            animationModel.setStart(animationModel.getPolylinelist().get(animationModel.getIndex()));
//                                    end = polylinelist.get(next);
                                            animationModel.setEnd(animationModel.getPolylinelist().get(animationModel.getNext()));

                                        }
                                        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 1);
                                        valueAnimator.setDuration(3000);
                                        valueAnimator.setInterpolator(new LinearInterpolator());
                                        valueAnimator.addUpdateListener(animation -> {
//                                    v = animation.getAnimatedFraction();
                                            animationModel.setV(animation.getAnimatedFraction());
//                                    lat = v * end.latitude + (1 - v) * start.latitude;
                                            animationModel.setLat(animationModel.getV() * animationModel.getEnd().latitude + (1-animationModel.getV())*animationModel.getStart().latitude);
                                            animationModel.setLng(animationModel.getV() * animationModel.getEnd().longitude + (1-animationModel.getV())*animationModel.getStart().longitude);
//                                    lng = v * end.longitude + (1 - v) * start.longitude;
                                            LatLng newPos = new LatLng(animationModel.getLat(), animationModel.getLng());
                                            currentMarker.setPosition(newPos);
                                            currentMarker.setAnchor(0.5f, 0.5f);
                                            currentMarker.setRotation(Common.getBearing(animationModel.getStart(), newPos));
                                        });
                                        valueAnimator.start();
                                        if (animationModel.getIndex() < animationModel.getPolylinelist().size() - 2) {
                                            animationModel.getHandler().postDelayed(this, 1500);

                                        }else if(animationModel.getIndex() < animationModel.getPolylinelist().size() - 1){
                                            animationModel.setRun(false);
                                            Common.driverLocationSubscribe.put(key,animationModel);
                                        }

                                    }
                                }
                            };
                            animationModel.getHandler().postDelayed(runnable, 1500);
                        }catch(Exception e){
                            Snackbar.make(getView(), e.getMessage(),Snackbar.LENGTH_LONG).show();
                        }
                    }));
        }
    }
}