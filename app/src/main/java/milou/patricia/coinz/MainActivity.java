package milou.patricia.coinz;


import android.annotation.SuppressLint;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.android.core.location.*;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;

import java.text.SimpleDateFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;

import static java.lang.Math.toIntExact;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,LocationEngineListener,PermissionsListener,AsyncResponse,SensorEventListener, StepListener{
    //firebase objects
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    //objects used for the implementation of map
    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap map;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location locationOrigin;
    private String mapstr="";

    private Bitmap[] IMarkers= new Bitmap[10];
    private IconFactory iconFactory;
    private Icon icon,icon2 ;
    private MapboxNavigation navigation;
    //objects that implement step detector
    private StepDetector simpleStepDetector;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int totaldailysteps=0;
    private int goal;
    //others
    private static final String PREFS_NAME = "preferences";
    private static final String PREF_MAP = "MapStyle";
    private static final String PREF_STEP = "Step Counter";
    private Boolean campus=false;
    private Boolean wait=false;
    public static String shil,peny,dolr,quid;
    private ArrayList<Coin> markers= new ArrayList<>();
    private Coin closestcoin = new Coin();
    private int loc=10;
    private TextView tvSteps;
    private boolean stepenable=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //set the map
        Mapbox.getInstance(this, "pk.eyJ1IjoicGF0cmljaWFtOTciLCJhIjoiY2pqaWl3aHFqMWMyeDNsbXh4MndnY3hzMiJ9.Fqn_9bmuScR4IqUrUbP6lA");
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        //get current date in the format 2018/10/09
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        String date1=dateFormat.format(date);
        //get current user
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        user= mAuth.getCurrentUser();
        //download todays map
        String maplink = "http://www.homepages.inf.ed.ac.uk/stg/coinz/" +date1+ "/coinzmap.geojson";
        new DownloadFileTask(this).execute(maplink);
        //set marker icons as appropriate
        iconFactory= IconFactory.getInstance(MainActivity.this);
        for(int i=0;i<10;i++){
            String x=Integer.toString(i);
            Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier("p"+x, "drawable", getPackageName()));
            IMarkers[i] = Bitmap.createScaledBitmap(imageBitmap,60, 60, false);
        }
        LocationListener locListener;
        locListener = new LocationListener() {
            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onProviderDisabled(String provider) {
            }
            @Override
            public void onLocationChanged(Location location) {
                locationOrigin = location;
            }
        };
        //set up navigation
        editNavigationBars();
        navigation = new MapboxNavigation(this, "pk.eyJ1IjoicGF0cmljaWFtOTciLCJhIjoiY2pqaWl3aHFqMWMyeDNsbXh4MndnY3hzMiJ9.Fqn_9bmuScR4IqUrUbP6lA");
        //step sensor
        tvSteps= findViewById(R.id.txt_steps);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
        String stepmode= settings.getString(PREF_STEP,"");
        if (stepmode.equals("Enable")){
            tvSteps.setVisibility(View.VISIBLE);
            stepenable=true;
        }else if (stepmode.equals("Disable")){
            tvSteps.setVisibility(View.INVISIBLE);
            stepenable=false;
        }
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        assert sensorManager != null;
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        getStepGoal();
        getStepsfromEarlier();
        sensorManager.registerListener(MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void editNavigationBars() {
        //set up the navigation bar
        AHBottomNavigation bottomNavigation=findViewById(R.id.bottom_navigation);
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
        AHBottomNavigationItem item1 = new AHBottomNavigationItem(R.string.tab_1, R.drawable.profile, R.color.color_tab_1);
        AHBottomNavigationItem item2 = new AHBottomNavigationItem(R.string.tab_3, R.drawable.friends, R.color.color_tab_1);
        AHBottomNavigationItem item3 = new AHBottomNavigationItem(R.string.tab_2, R.drawable.wallet,R.color.color_tab_1);
        AHBottomNavigationItem item4 = new AHBottomNavigationItem(R.string.tab_4, R.drawable.piggybank,R.color.color_tab_1);
        AHBottomNavigationItem item5 = new AHBottomNavigationItem(R.string.tab_5, R.drawable.settings,R.color.color_tab_1);
        // Add items
        bottomNavigation.addItem(item1);
        bottomNavigation.addItem(item2);
        bottomNavigation.addItem(item3);
        bottomNavigation.addItem(item4);
        bottomNavigation.addItem(item5);
        // Change colors
        bottomNavigation.setAccentColor(Color.parseColor("#99d8d6"));
        bottomNavigation.setInactiveColor(Color.parseColor("#99d8d6"));
        //set up the action of each item
        bottomNavigation.setOnTabSelectedListener((position, wasSelected) -> {
            if (position==0){
                Profile();
            }else if (position==1){
                Friends();
            }else if(position==2){
                Wallet();
            }else if(position==3){
                Bank();
            }else if(position==4){
            Settings();
            }
            return true;
        });

    }

    /**
     * This function is called once the download of the map is complete
     * @param output String read from the internet
     */
    public void processFinish(String output){
        mapstr=output;
        try {
            ReadJSON();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * This file turns the string from the processFinish function to a json file and reads the file
     */
    public void ReadJSON()throws JSONException {
        ProgressBar pgsBar;
        JSONArray features;
        JSONObject rates;
        //while the text downloaded is not empty
        while(!mapstr.equals("")) {
            //turn it into a json file
            JSONObject json = new JSONObject(mapstr);
            //read the rates
            rates=json.getJSONObject("rates");
            shil = rates.getString("SHIL");
            peny = rates.getString("PENY");
            quid = rates.getString("QUID");
            dolr = rates.getString("DOLR");
           features=json.getJSONArray("features");
           //read all markers
           for(int i=0;i<features.length();i++){
               JSONObject feature =features.getJSONObject(i);
               //get properties
               JSONObject properties =feature.getJSONObject("properties");
               String id =properties.getString("id");
               String value =properties.getString("value");
               String currency =properties.getString("currency");
               String markersymbol =properties.getString("marker-symbol");
               String markercolour =properties.getString("marker-color");
               //get coordinates
               JSONObject geometry =feature.getJSONObject("geometry");
               JSONArray coordinates =geometry.getJSONArray("coordinates");
               String lng=coordinates.get(0).toString();
               String lat=coordinates.get(1).toString();
               //check is that coin was collected today at an earlier play from the same user
               if(user.getEmail()!=null) {
                   DocumentReference docRef = db.collection("Users").document(user.getEmail()).collection("Coins").document(id);
                   docRef.get().addOnCompleteListener(task -> {
                       if (task.isSuccessful()) {
                           DocumentSnapshot document = task.getResult();
                           if (document.exists()) {
                               //if yes don't show it on the map
                           } else {
                               //create the appropriate marker symbol
                               icon=null;
                               icon = getNumIcon(markersymbol);
                               while(icon==null){
                                   //wait until icon is generated
                               }
                               icon2=null;
                               icon2 = getColourIcon(icon, markercolour);
                               while(icon2==null){
                                    //wait until icon is transformed
                               }
                               Coin coin = new Coin(id, value, currency,lat, lng);
                               //add that coin on the map
                               addMarker(coin, icon2);
                           }
                       } else {
                           Timber.d(task.getException(), "get failed with ");
                       }
                   });
               }
           }
            break;
        }
        //once all markers have been placed , switch the progress bar off.
        pgsBar = findViewById(R.id.pBar);
        pgsBar.setVisibility(View.GONE);
    }

    /**
     * Get the marker with the right symbol
     * @param markersymbol The symbol to be used on the marker
     * @return Icon icon with specific number
     */
    public Icon getNumIcon(String markersymbol){
        Icon icon=null;
        switch (markersymbol) {
            case "0": icon= iconFactory.fromBitmap(IMarkers[0]);
                        break;
            case "1": icon= iconFactory.fromBitmap(IMarkers[1]);
                        break;
            case "2":  icon= iconFactory.fromBitmap(IMarkers[2]);
                        break;
            case "3":  icon= iconFactory.fromBitmap(IMarkers[3]);
                        break;
            case "4":  icon= iconFactory.fromBitmap(IMarkers[4]);
                        break;
            case "5":  icon= iconFactory.fromBitmap(IMarkers[5]);
                        break;
            case "6":  icon= iconFactory.fromBitmap(IMarkers[6]);
                        break;
            case "7":  icon= iconFactory.fromBitmap(IMarkers[7]);
                        break;
            case "8":  icon= iconFactory.fromBitmap(IMarkers[8]);
                        break;
            case "9":  icon= iconFactory.fromBitmap(IMarkers[9]);
                        break;
        }
       return icon;
    }

    /**
     * Set the marker's icon to the right colour
     * @param icon Current icon of marker
     * @param colour Color to use
     * @return New Icon of that marker
     */
    public Icon getColourIcon(Icon icon,String colour) {
        final Bitmap bmp =icon.getBitmap();
        int [] allpixels = new int [bmp.getHeight() * bmp.getWidth()];
        bmp.getPixels(allpixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        int to =Color.parseColor(colour);
        for(int i = 0; i < allpixels.length; i++)
        {
            if(allpixels[i] == Color.rgb(0,174,239))
            {
                allpixels[i] =to;
            }
        }

       bmp.setPixels(allpixels,0,bmp.getWidth(),0, 0,bmp.getWidth(),bmp.getHeight());
        return iconFactory.fromBitmap(bmp);
    }

    /**
     * Add that marker on the map
     * @param coin Current coin object
     * @param icon Icon to use
     */
    public void addMarker(Coin coin,Icon icon){
        mapView.getMapAsync(mapboxMap -> {
            // One way to add a marker view
            MarkerOptions m= new MarkerOptions()
                    .position(coin.getLatLng())
                    .setIcon(icon);
            Marker marker= m.getMarker();
            coin.addMaker(marker);
            markers.add(coin);
            mapboxMap.addMarker(m);
        });
    }

    /**
     * This function is called once the user collects a coin
     * the marker of the collected coin has to be removed from the map
     * @param marker Coin's marker
     */
    public void removeMarker(Marker marker){
        mapView.getMapAsync(mapboxMap -> mapboxMap.removeMarker(marker));
    }

    /**
     * This function is called once the map is ready
     * @param mapboxMap Map
     */
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map=mapboxMap;
        if (mapboxMap == null) {
            Timber.d("[onMapReady] mapBox is null");
        } else {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
            String styleValue = settings.getString(PREF_MAP, "");
            if(styleValue.equals("Dark")){
                map.setStyle("mapbox://styles/mapbox/dark-v9");
            }else if(styleValue.equals("Light")){
                map.setStyle("mapbox://styles/mapbox/streets-v9");
            }
            // Set user interface options
            map.getUiSettings().setCompassEnabled(true);
            //map.getUiSettings().setZoomControlsEnabled(true);
            // Make location information available
            enableLocation();
        }
    }

    /**
     * This function retrived the user's location
     */
    private void enableLocation(){
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();
            // Create an instance of the plugin. Adding in LocationLayerOptions is also an optional parameter
            LocationLayerPlugin locationLayerPlugin = new LocationLayerPlugin(mapView,map);
            // Set the plugin's camera mode
            locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
            getLifecycle().addObserver(locationLayerPlugin);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    /**
     * This function initializes the Location Engine
     */
    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine() {
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.addLocationEngineListener(this);
        locationEngine.setInterval(5000); // preferably every 5 seconds
        locationEngine.setFastestInterval(1000); // at most every second
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();
        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            locationOrigin = lastLocation;
            setCameraPosition(lastLocation);
        }
    }

    /**
     * This function initializes the location layer used for the user's location
     */
    @SuppressWarnings("MissingPermission")
    private void initializeLocationLayer() {
        if (mapView == null) {
            Timber.d("mapView is null");
        } else {
            if (map == null) {
                Timber.d("map is null");
            } else {
                locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
                locationLayerPlugin.setLocationLayerEnabled(true);
                locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
                locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
            }
        }
    }
    /**
     * This function requests location updates
     */
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        //locationEngine.requestLocationUpdates();
       // locationEngine.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locListener);
        locationEngine.requestLocationUpdates();
        locationOrigin = locationEngine.getLastLocation();//.getLastKnownLocation(LocationManager.GPS_PROVIDER);

    }
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
    }

    /**
     * If location is granted enable Location
     * @param granted If permission is granted
     */
    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocation();
        } else {
            finish();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressWarnings("MissingPermission")
    public void onStart() {
        super.onStart();
        mapView.onStart();
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStart();
        }
    }
    /**
     * This function sets the focus of the map on a specific location
     * @param location location to focus on
     */
    private void setCameraPosition(Location location){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),14.00));
    }

    /**
     * If the map is focused on the user's location, this function will make the map to focus on central campus.
     * If the map is focused on central campus, this function will make the map to focus on the user's location.
     * @param view Current view
     */
    public void centreMap(View view){
        Timber.i("Clicked");
        Button centremapbtn = findViewById(R.id.centremap);
        if (!campus){
            campus=true; //whenever campus==true, then the map is focused on central campus
            final Location campuslocation = new Location("central campus");
            campuslocation.setLatitude(55.943877);
            campuslocation.setLongitude(-3.187479);
            setCameraPosition(campuslocation);
            centremapbtn.setText(R.string.loc);
        }else{
            campus=false;//otherwise, then the map is focused on the user's location
            setCameraPosition(locationOrigin);
           centremapbtn.setText(R.string.campus);
        }
    }

    /**
     * This function is called when the user's location change
     * @param location Updated location
     */
    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            Timber.d("[onLocationChanged] location is null");
        } else {
            Timber.d("[onLocationChanged] location is not null");
            locationOrigin = location;
            if((markers.size()>0)) {
                if (!wait) {
                    checkifclosetocoin();
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void checkifclosetocoin(){
        Double minimumDist=getClosestCoin(null);
        //if the user is in the range away from a coin a pop up will allow him/her to collect it
        if (minimumDist <= 25 && closestcoin.getCurrency()!=null) {
            wait=true;
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            // Get the layout inflater
            LayoutInflater inflater = MainActivity.this.getLayoutInflater();
            @SuppressLint("InflateParams") View view2= inflater.inflate(R.layout.popup, null);
            TextView title =view2.findViewById(R.id.title);
            title.setText("Do you want to collect it?");
            EditText info =view2.findViewById(R.id.editTextDialogUserInput);
            info.clearFocus();
            info.setKeyListener(null);
            info.setHintTextColor(null);
            String text= closestcoin.getCurrency()+"   "+closestcoin.getValue();
            info.setText(text);
            builder.setView(view2)
                    // Add action buttons
                    .setPositiveButton("Yeah!", (dialog, whichButton) -> {
                        //add coin in wallet
                        wait=false;
                        addCoin();
                    }).setNegativeButton("Cancel", (dialog, whichButton) -> wait=false).show();
        }
    }
    /**
     * This functions returns the distance of the coin which is closest to the user
     * @return Distance to closest coin
     */
    public Double getClosestCoin(String curr) {
        Location userLocation = locationOrigin;
        float[] result = new float[1];
        Double minimumDist=0.0;
           if(curr !=null){
            for  (int x = 0; x < markers.size(); x++) {
                //for the first coin that satisfies the currency parameter
                //get its latlong coordinates
                //get the user's location
                    if (markers.get(x).getCurrency().toString().equals(curr)) {
                        LatLng coin = markers.get(x).getLatLng();
                        //this function sets result to the distance between that coin and the user in metres
                        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                                coin.getLatitude(), coin.getLongitude(), result);
                        minimumDist = (double) result[0];
                        break;
                    }

            }
            }else{
                LatLng coin = markers.get(0).getLatLng();
                //this function sets result to the distance between that coin and the user in metres
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        coin.getLatitude(), coin.getLongitude(), result);
                minimumDist = (double) result[0];
            }
                //for the first coin  if (markers.size() > 0) {
                //get its latlong coordinates
                //get the user's location
                for (int x = 1; x < markers.size(); x++) {
                    //do the same for the rest of the coins and always compare
                    //to check which one has the minimum distance
                    Double dist = 0.0;
                    if (curr != null) {
                        if (markers.get(x).getCurrency().equals(curr)) {
                            LatLng coin = markers.get(x).getLatLng();
                            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                                    coin.getLatitude(), coin.getLongitude(), result);
                            dist = (double) result[0];
                            if (dist < minimumDist) {
                                minimumDist = dist;
                                closestcoin = markers.get(x);
                            }
                        }
                    } else {
                        LatLng coin = markers.get(x).getLatLng();
                        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                                coin.getLatitude(), coin.getLongitude(), result);
                        dist = (double) result[0];
                        if (dist < minimumDist) {
                            minimumDist = dist;
                            closestcoin = markers.get(x);
                        }

        }}
        return minimumDist;
    }

    /**
     * If all requirements are met,this function add a specific coin in the user's wallet
     */
    public void addCoin(){
        Toast.makeText(MainActivity.this,"New Coin Added", Toast.LENGTH_SHORT).show();
        closestcoin.addInWallet();
        markers.remove(closestcoin);
        removeMarker(closestcoin.getMarker());
    }

    
    @SuppressLint("SetTextI18n")
    public void getSpinner(View view){
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View view3 = inflater.inflate(R.layout.choosecoinpopup, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        Spinner dropdown = view3.findViewById(R.id.spinner);
        Button b= view3.findViewById(R.id.button);
        dropdown.setSelection(4);
        builder.setView(view3).show();
        b.setOnClickListener(v -> {
            loc= dropdown.getSelectedItemPosition();
            switch (loc) {
                //options for the spinner
                case 0:
                    navigateToClosestCoin("DOLR");
                    break;
                case 1:
                    navigateToClosestCoin("QUID");
                    break;
                case 2:
                    navigateToClosestCoin("PENY");
                    break;
                case 3:
                    navigateToClosestCoin("SHIL");
                    break;
                case 4:
                    navigateToClosestCoin(null);
                    break;
            }

        });
    }

    /**
     * This function starts a navigation to the coin closest to the user.
     * @param choice choice
     */
    @SuppressLint("SetTextI18n")
    private void navigateToClosestCoin(String choice){
        while(closestcoin==null) {
            /* if closest coin has not been retrieved yet */
        }
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View view3 = inflater.inflate(R.layout.popup, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        TextView title = view3.findViewById(R.id.title);
        title.setText("Closest Coin Found:");
        EditText info =view3.findViewById(R.id.editTextDialogUserInput);
        info.clearFocus();
        info.setKeyListener(null);
        info.setHintTextColor(null);
        String text= closestcoin.getCurrency()+"   "+closestcoin.getValue();
        info.setText(text);
        builder.setView(view3)
                // Add action buttons
                .setPositiveButton("Start Navigation", (dialog, whichButton) -> {
                    Timber.i("Starts");
                    Point destination = Point.fromLngLat(closestcoin.getLatLng().getLongitude(), closestcoin.getLatLng().getLatitude());
                    getRoute(destination);
                }).setNegativeButton("Cancel", (dialog, whichButton) -> {
                    // Do nothing.
                }).show();
    }

    /**
     * This function finds the quickest route to the destination point
     * @param dest Destination point
     */
    private void getRoute(Point dest){
        //get user's location
        Point origin = Point.fromLngLat(locationOrigin.getLongitude(),locationOrigin.getLatitude());
        //build the route
        if (Mapbox.getAccessToken() != null) {
            NavigationRoute.builder(this)
                    .accessToken(Mapbox.getAccessToken())
                    .origin(origin)
                    .destination(dest)
                    .build()
                    .getRoute(new Callback<DirectionsResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
                            // You can get the generic HTTP info about the response
                            Timber.d("Response code: %s", response.code());
                            if (response.body() == null) {
                                Timber.e("No routes found, make sure you set the right user and access token.");
                                return;
                            } else {
                                if (response.body() != null && response.body().routes().size() < 1) {
                                    Timber.e("No routes found");
                                    return;
                                }
                            }

                            DirectionsRoute currentRoute = null;
                            if (response.body() != null) {
                                currentRoute = response.body().routes().get(0);
                            }
                            //start the navigation
                            startNavigation(currentRoute);
                        }

                        @Override
                        public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
                            Timber.e("Error: %s", throwable.getMessage());
                        }
                    });
        }
    }

    /**
     * This function starts the navigation
     * @param directionsRoute Directions to be used
     */
    private void startNavigation(DirectionsRoute directionsRoute) {
        NavigationLauncherOptions.Builder navOptions = NavigationLauncherOptions.builder()
                        .directionsRoute(directionsRoute)
                        .enableOffRouteDetection(false)
                        //by walking
                        .directionsProfile(DirectionsCriteria.PROFILE_WALKING);

        NavigationLauncher.startNavigation(MainActivity.this, navOptions.build());
        navigation.addNavigationEventListener(running -> {
        });
    }

    /**
     * Starts the Profile Activity.
     */
    public void Profile(){
        Intent i = new Intent(MainActivity.this, ShowProf.class);
        startActivity(i);
    }

    /**
     * Starts the Wallet Activity.
     */
    public void Wallet(){
        Intent i = new Intent(MainActivity.this, WalletActivity.class);
        startActivity(i);
    }

    /**
     * Starts the Friends Activity.
     */
    private void Friends() {
        Intent i = new Intent(MainActivity.this, FriendsActivity.class);
        startActivity(i);
    }

    /**
     * Starts the Bank Activity.
     */
    private void Bank() {
        Intent i = new Intent(MainActivity.this, BankActivity.class);
        startActivity(i);
    }

    /**
     * This function is called when the user clicks on the Settings tab.
     */
    private void Settings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        @SuppressLint("InflateParams") View view2= inflater.inflate(R.layout.setting, null);
        Switch dayview=view2.findViewById(R.id.dayview);
        Switch nightview=view2.findViewById(R.id.nightview);
        Switch stepcounter=view2.findViewById(R.id.stepcounter);

        if(map.getStyleUrl().equals("mapbox://styles/mapbox/streets-v9")){
            dayview.setChecked(true);
        }else{
            nightview.setChecked(true);
        }
        if(tvSteps.getVisibility() == View.VISIBLE){
            stepcounter.setChecked(true);
        }else{
            stepcounter.setChecked(false);
        }
        Button save= view2.findViewById(R.id.save);
        AlertDialog alert = builder.setView(view2).show();
        Button signout= view2.findViewById(R.id.signout);
        signout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(MainActivity.this, LoginScreen.class);
            startActivity(i);
        });
        save.setOnClickListener(v -> {
            Boolean day = dayview.isChecked();
            Boolean night = nightview.isChecked();
            Boolean step = stepcounter.isChecked();
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            if(day && night){
                Toast.makeText(view2.getContext(),"You need to select one map style.",Toast.LENGTH_SHORT).show();
            }else if(night){
                map.setStyle("mapbox://styles/mapbox/dark-v9");
                editor.putString(PREF_MAP,"Dark");
                editor.apply();
            }else if(day){
                map.setStyle("mapbox://styles/mapbox/streets-v9");
                editor.putString(PREF_MAP,"Light");
                editor.apply();
            }else if(!day&&!night){
                Toast.makeText(view2.getContext(),"You need to select one map style.",Toast.LENGTH_SHORT).show();
            }
            if(step){
                tvSteps.setVisibility(View.VISIBLE);
                stepenable=true;
                editor.putString(PREF_STEP,"Enable");
                editor.apply();
            }else{
                tvSteps.setVisibility(View.INVISIBLE);
                stepenable=false;
                editor.putString(PREF_STEP,"Disable");
                editor.apply();
            }
            alert.dismiss();
        });


    }
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        updateSteps();
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        updateSteps();
        super.onStop();
        mapView.onStop();
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStart();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        updateSteps();
        super.onDestroy();
        if(locationEngine!=null){
            locationEngine.deactivate();
        }
        mapView.onDestroy();
        navigation.stopNavigation();
        navigation.onDestroy();
    }

    /**
     * This function gets the goal stored in the firebase. If not(first time played) the goal is set to 50.
     */
    private void getStepGoal(){
        //get goal
        DocumentReference docRef = db.collection("Users").document(user.getEmail()).collection("Steps").document("GOAL");
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                        goal=(int) Math.round(document.getDouble("Goal"));
                        if(goal==0){
                            goal=50;
                        }
                }else{
                    //set goal to 50
                    Map<String, Object> step = new HashMap<>();
                    step.put("Goal",50);
                    db.collection("Users").document(user.getEmail()).collection("Steps").document("GOAL")
                            .set(step)
                            .addOnSuccessListener(aVoid -> Timber.d("DocumentSnapshot successfully updated."))
                            .addOnFailureListener(e -> Timber.tag("User").w(e, "Error updating document"));
                    goal=50;
                }
            } else {
                Timber.d(task.getException(), "get failed with ");
            }
        });
    }
    private void stepsgoalreached(){
        if(totaldailysteps>=goal){
            Map<String, Object> step = new HashMap<>();
            step.put("Goal",goal*2);
            db.collection("Users").document(user.getEmail()).collection("Steps").document("GOAL")
                    .set(step)
                    .addOnSuccessListener(aVoid -> Timber.d("DocumentSnapshot successfully updated."))
                    .addOnFailureListener(e -> Timber.tag("User").w(e, "Error updating document"));
            goal=goal*2;
            Toast.makeText(MainActivity.this, "GOAL REACHED! New goal: " + goal, Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * This function retrives the amount of steps from an earlier play.
     */
    @SuppressLint("SetTextI18n")
    private void getStepsfromEarlier(){
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
        Date date = new Date();
        String date1=dateFormat.format(date);
        //get sum of today's step if its not the first time played today
        DocumentReference docRef = db.collection("Users").document(user.getEmail()).collection("Steps").document(date1);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Timber.d("DocumentSnapshot data: %s", document.getData());
                    totaldailysteps=toIntExact(document.getLong("Steps"));
                    TextView TvSteps= findViewById(R.id.txt_steps);
                    TvSteps.setText(TEXT_NUM_STEPS + totaldailysteps);
                }
            } else {
                Timber.d(task.getException(), "get failed with ");
            }
        });
    }
    /**
     * This function updates the steps that the user took in Firebase.
     */
    private void updateSteps() {
            @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
            Date date = new Date();
            String date1 = dateFormat.format(date);
            Map<String, Object> step = new HashMap<>();
            step.put("Steps", totaldailysteps);
            db.collection("Users").document(user.getEmail()).collection("Steps").document(date1)
                    .set(step)
                    .addOnSuccessListener(aVoid -> Timber.d("DocumentSnapshot successfully updated."))
                    .addOnFailureListener(e -> Timber.tag("User").w(e, "Error updating document"));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Updates the step counter.
     * @param timeNs Current time
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void step(long timeNs) {
        if(stepenable) {
            totaldailysteps++;
            TextView TvSteps = findViewById(R.id.txt_steps);
            TvSteps.setText(TEXT_NUM_STEPS + totaldailysteps);
            stepsgoalreached();
            updateSteps();
        }
    }
    @SuppressLint("SetTextI18n")
    public void showGoal(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Get the layout inflater
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        @SuppressLint("InflateParams") View view2= inflater.inflate(R.layout.popup, null);
        TextView title =view2.findViewById(R.id.title);
        title.setText("Step Counter");
        EditText info =view2.findViewById(R.id.editTextDialogUserInput);
        info.clearFocus();
        info.setKeyListener(null);
        info.setHintTextColor(null);
        String text= "Next Goal: \n"+goal;
        info.setText(text);
        builder.setView(view2).show();
    }
}
