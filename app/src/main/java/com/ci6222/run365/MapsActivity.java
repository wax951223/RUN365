package com.ci6222.run365;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleMap.OnPolylineClickListener {
    private GoogleMap mMap;
    private static final String TAG = MapsActivity.class.getSimpleName();
    //entry point to fused location provider
    private FusedLocationProviderClient mFusedLocationProviderClient;

    //default loc when user refuse to give location service permission
    private final LatLng mDefaultLocation = new LatLng(1.3483099, 103.680946);
    private static final int DEFAULT_ZOOM = 14;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    //current geographical location
    private Location mLastKnownLocation;
    private Location prevLocation;

    //keys for activity state
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    //Text Views
    private TextView distanceTextView;
    private TextView timeTextView;
    private TextView avgPaceTextView;

    //Switcher for view
    private ViewSwitcher viewSwitcher;

    //Handler for repeating a runnables
    private Handler handler = new Handler();

    //Parameter for drawing the trace route
    private static final int TRACEROUTE_WIDTH_PX = 12;
    private ArrayList<LatLng> points = new ArrayList<>();

    //Parameter for stopwatch
    private long Milliseconds = 0L;
    private long starttime = 0L;
    private long endtime = 0L;
    private static final int ONE_SEC = 1000;
    private static final int FIVEHUNDRED_MIL = 500;

    //Parameter for accumulating to distance run
    private float Distance = 0;
    private static final double METER_TO_KM = 0.001;

    //Historical activity intent
    private Intent historicalactivity;

    //DB
    private DbHandler dbHandler;

    //update and plot the trace route,
    private Runnable tracker = new Runnable() {
        @Override
        public void run() {
            getDeviceLocation();
            updateRoute(mLastKnownLocation);
            handler.postDelayed(this, FIVEHUNDRED_MIL);
        }
    };
    //update and display time
    private Runnable stopwatch = new Runnable() {
        @Override
        public void run() {
            Milliseconds = SystemClock.uptimeMillis() - starttime;
            int secs = (int) (Milliseconds) / 1000;
            int mins = secs / 60;
            int hours = mins / 60;
            secs %= 60;
            mins %= 60;

            String time = String.format(Locale.getDefault(),
                    "Time\n %02d:%02d:%02d", hours, mins, secs);
            timeTextView.setText(time);
            handler.postDelayed(this, ONE_SEC);
        }
    };
    //update and display distance, pace
    private Runnable dataUpdates = new Runnable() {
        @Override
        public void run() {
            String distance = String.format(Locale.getDefault(),
                    "Distance\n %.2f km", convertmeterstoKM(Distance));
            distanceTextView.setText(distance);

            float p = getPace(Distance, Milliseconds);
            String averagePace = String.format(Locale.getDefault(),
                    "Pace\n %s /km", roundupMinutes(p));
            avgPaceTextView.setText(averagePace);
            handler.postDelayed(this, FIVEHUNDRED_MIL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLocationPermission();
//        if (mLocationPermissionGranted == false) {
//            Toast.makeText(this,"Permission ERROR, App will Quit", Toast.LENGTH_SHORT).show();
//
//            finish();
//        }
        //Get location and camera position from saved instance state
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        }
        setContentView(R.layout.activity_maps);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        ImageButton StartRunningButton = findViewById(R.id.startrunningbutton);
        ImageButton StopRunningButton = findViewById(R.id.stoprunningbutton);
        ImageButton HistoryButton = findViewById(R.id.historybutton);
        distanceTextView = findViewById(R.id.distanceTextView);
        timeTextView = findViewById(R.id.timeTextView);
        avgPaceTextView = findViewById(R.id.avgPaceTextView);
        viewSwitcher = findViewById(R.id.viewSwitcher);
        dbHandler = new DbHandler(this);
        //Build the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        StartRunningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLocationPermissionGranted == false) {
                    Log.i(TAG, "permission error!!!!");
                    System.exit(0);
                }
                getDeviceStartLocation();
                Distance = 0;
                viewSwitcher.showNext();
                starttime = SystemClock.uptimeMillis();
                tracker.run();
                stopwatch.run();
                dataUpdates.run();
            }
        });

        StopRunningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.removeCallbacks(tracker);
                handler.removeCallbacks(stopwatch);
                handler.removeCallbacks(dataUpdates);
                endtime = SystemClock.uptimeMillis();
                addWorkoutToDB(Distance, starttime, endtime);
                historicalactivity = new Intent(getApplicationContext(), HistoryActivity.class);
                startActivity(historicalactivity);
                finish();
            }
        });
        HistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                historicalactivity = new Intent(getApplicationContext(), HistoryActivity.class);
                startActivity(historicalactivity);
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getLocationPermission();
        updateLocationUI();
        getDeviceStartLocation();

    }

    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            mLastKnownLocation = task.getResult();
                        } else {
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceStartLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            prevLocation = mLastKnownLocation;

                        } else {
                            Log.d(TAG, "No current location, program's using default location");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }


    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private String getStopWatchTime(long starttime, long endtime) {
        int secs = (int) ((endtime - starttime) / 1000);
        int mins = secs / 60;
        int hours = mins / 60;
        secs %= 60;
        mins %= 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs);
    }

    private float convertmeterstoKM(float meters) {
        return meters * (float) METER_TO_KM;
    }

    private float getPace(float meters, long milliseconds) {
        int secs = (int) (milliseconds / 1000);
        float mins = (float) secs / 60;
        float kms = convertmeterstoKM(meters);
        float pacevalue = 0;
        if (meters >= 3) {
            pacevalue = mins / kms;
        } else {
            pacevalue = 0;
        }
        return pacevalue;
    }


    private String generateDate() {
        GregorianCalendar calandar = new GregorianCalendar();
        int month = calandar.get(Calendar.MONTH) + 1;
        int day = calandar.get(Calendar.DAY_OF_MONTH);
        int year = calandar.get(Calendar.YEAR);
        return String.format(Locale.getDefault(), "%d/%d/%d", year, month, day);
    }

    private void addWorkoutToDB(float distance, long starttime, long endtime) {
        float kms = convertmeterstoKM(distance);
        String time_total = getStopWatchTime(starttime, endtime);
        float pace = getPace(distance, endtime - starttime);
        String date = generateDate();

        if (dbHandler.addData(kms, time_total, pace, date)) {
            Toast.makeText(this, "Activity saved", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Oops, let's go to Mr.HARIHARAN for help", Toast.LENGTH_LONG).show();
        }
    }

    private String roundupMinutes(float decimal) {
        int mins = (int) Math.floor(decimal);
        double fractional = decimal - mins;
        int secs = (int) Math.round(fractional * 60);
        return String.format(Locale.getDefault(), "%d:%02d", mins, secs);
    }

    @Override
    public void onPolylineClick(Polyline polyline) {

    }

    private void updateRoute(Location nextLocation) {
        if (mMap != null) {
            points.add(new LatLng(nextLocation.getLatitude(), nextLocation.getLongitude()));

            PolylineOptions polylineOptions = new PolylineOptions()
                    .color(Color.GREEN)
                    .width(TRACEROUTE_WIDTH_PX)
                    .jointType(JointType.BEVEL)
                    .startCap(new RoundCap())
                    .endCap(new SquareCap())
                    .clickable(false);

            polylineOptions.addAll(points);
            mMap.addPolyline(polylineOptions);

            Distance += prevLocation.distanceTo(nextLocation);
            prevLocation = nextLocation;
        }
    }
}
