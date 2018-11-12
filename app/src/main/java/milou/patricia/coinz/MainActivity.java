package milou.patricia.coinz;


import android.annotation.SuppressLint;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import java.util.List;
import java.util.Date;

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
    private Icon icon ;
    private MapboxNavigation navigation;
    //objects that implement step detector
    private StepDetector simpleStepDetector;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps;
    //others
    private Boolean campus=false;
    public static String shil,peny,dolr,quid;
    private ArrayList<Coin> markers= new ArrayList<Coin>();
    private Coin closestcoin = new Coin();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //set the map
        Mapbox.getInstance(this, "pk.eyJ1IjoicGF0cmljaWFtOTciLCJhIjoiY2pqaWl3aHFqMWMyeDNsbXh4MndnY3hzMiJ9.Fqn_9bmuScR4IqUrUbP6lA");
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        //get current date in the format 2018/10/09
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
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
        //set up navigation
        editNavigationBars();
        navigation = new MapboxNavigation(this, "pk.eyJ1IjoicGF0cmljaWFtOTciLCJhIjoiY2pvOTF6dm1iMGZsZTNxb3g1MDU0ZGtsYiJ9.R6jgvI9ZH7mUIIxwzJjW_Q");
        //step sensor
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        assert sensorManager != null;
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        numSteps = 0;
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

        // Add items
        bottomNavigation.addItem(item1);
        bottomNavigation.addItem(item2);
        bottomNavigation.addItem(item3);
        bottomNavigation.addItem(item4);
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
                   docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                       @Override
                       public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                           if (task.isSuccessful()) {
                               DocumentSnapshot document = task.getResult();
                               if (document.exists()) {
                                   //if yes don't show it on the map
                               } else {
                                   //create the appropriate marker symbol
                                   icon = getNumIcon(markersymbol);
                                   icon = getColourIcon(icon, markercolour);
                                   Coin coin = new Coin(id, value, currency,lat, lng);
                                   //add that coin on the map
                                   addMarker(coin, icon);
                               }
                           } else {
                               Log.d("Doc", "get failed with ", task.getException());
                           }
                       }
                   });
               }
           }
            break;
        }
        //once all markers have been placed , switch the progress bar off.
        pgsBar = (ProgressBar) findViewById(R.id.pBar);
        pgsBar.setVisibility(View.GONE);
    }

    /**
     * Get the marker with the right symbol
     * @param markersymbol The symbol to be used on the marker
     * @return Icon icon with specific number
     */
    public Icon getNumIcon(String markersymbol){
        switch (markersymbol) {
            case "0": return iconFactory.fromBitmap(IMarkers[0]);
            case "1": return iconFactory.fromBitmap(IMarkers[1]);
            case "2": return iconFactory.fromBitmap(IMarkers[2]);
            case "3": return iconFactory.fromBitmap(IMarkers[3]);
            case "4": return iconFactory.fromBitmap(IMarkers[4]);
            case "5": return iconFactory.fromBitmap(IMarkers[5]);
            case "6": return iconFactory.fromBitmap(IMarkers[6]);
            case "7": return iconFactory.fromBitmap(IMarkers[7]);
            case "8": return iconFactory.fromBitmap(IMarkers[8]);
            case "9": return iconFactory.fromBitmap(IMarkers[9]);
        }
       return icon;
    }

    /**
     * Set the marker's icon to the right colour
     * @param icon Current icon of marker
     * @param colour Color to use
     * @return New Icon of that marker
     */
    public Icon getColourIcon(Icon icon,String colour){
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
        enableLocation();
    }

    /**
     * This function retrived the user's location
     */
    private void enableLocation(){
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();
            // Create an instance of the plugin. Adding in LocationLayerOptions is also an optional
            // parameter
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
    private void initializeLocationEngine(){
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            locationOrigin = lastLocation;
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

//    private void initializeLocationLayer(){
//        locationLayerPlugin=new LocationLayerPlugin(mapView,map,locationEngine);
//        locationLayerPlugin.setLocationLayerEnabled(true);
//        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
//        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
//    }

    /**
     * This function requests location updates
     */
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        locationEngine.requestLocationUpdates();

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
        if(location!=null){
            locationOrigin=location;
            setCameraPosition(locationOrigin); //was location before
        }
    }

    /**
     * This functions returns the distance of the coin which is closest to the user
     * @return Distance to closest coin
     */
    public Double getClosestCoin() {
        Double minimumDist=0.0;
        if (markers.size() > 0) {
            //for the first coin
            //get its latlong coordinates
            //get the user's location
            Location userLocation = locationOrigin;
            float[] result = new float[1];
            LatLng coin = markers.get(0).getLatLng();
            //this function sets result to the distance between that coin and the user in metres
            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                    coin.getLatitude(), coin.getLongitude(), result);
            minimumDist = (double) result[0];

            for (int x = 1; x < markers.size(); x++) {
                //do the same for the rest of the coins and always compare
                //to check which one has the minimum distance
                coin = markers.get(x).getLatLng();
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        coin.getLatitude(), coin.getLongitude(), result);
                Double dist = (double) result[0];
                if (dist < minimumDist) {
                    minimumDist = dist;
                    closestcoin = markers.get(x);
                }
            }

            }
        return minimumDist;
    }

    /**
     * This function checks if the user is 25 metres away from a coin
     * If yes, the user can collect it
     * @param view Current View
     */
    @SuppressLint("SetTextI18n")
    public void CheckIfClosetoACoin(View view) {
        Double minimumDist=getClosestCoin();
            //if the user is in the range away from a coin a pop up will allow him/her to collect it
            if (minimumDist <= 2500 && closestcoin.getCurrency()!=null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                // Get the layout inflater
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                @SuppressLint("InflateParams") View view2= inflater.inflate(R.layout.popup2, null);
                TextView title =view2.findViewById(R.id.title);
                title.setText("Do you want to collect it?");
                TextView info =view2.findViewById(R.id.info);
                String text= closestcoin.getCurrency()+"   "+closestcoin.getValue();
                info.setText(text);
                builder.setView(view2)
                        // Add action buttons
                        .setPositiveButton("Yeah!", (dialog, whichButton) -> {
                            //add coin in wallet
                            addCoin();
                        }).setNegativeButton("Cancel", (dialog, whichButton) -> {
                            // Do nothing.
                        }).show();
            }else{
                //if the user is not close
                Toast.makeText(MainActivity.this,"Get closer!",Toast.LENGTH_SHORT).show();
            }
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

    /**
     * This function starts a navigation to the coin closest to the user.
     * @param view Current view
     */
    @SuppressLint("SetTextI18n")
    public void navigateToClosestCoin(View view){
        Double mindist=getClosestCoin();
        while(closestcoin==null) {
            /* if closest coin has not been retrieved yet */
        }
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View view3 = inflater.inflate(R.layout.popup2, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        TextView title = view3.findViewById(R.id.title);
        title.setText("Closest Coin Found:");
        TextView info =view3.findViewById(R.id.info);
        String text= closestcoin.getCurrency()+"   "+closestcoin.getValue();
        info.setText(text);
        builder.setView(view3)
                // Add action buttons
                .setPositiveButton("Start Navigation", (dialog, whichButton) -> {
                    Timber.i("Starts");
                    Point destination = Point.fromLngLat(closestcoin.getLatLng().getLongitude(), closestcoin.getLatLng().getLatitude());
                    getRoute(destination);
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
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

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
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
        super.onDestroy();
        if(locationEngine!=null){
            locationEngine.deactivate();
        }
        mapView.onDestroy();
        FirebaseAuth.getInstance().signOut();
        navigation.stopNavigation();
        navigation.onDestroy();
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
        numSteps++;
        TextView TvSteps= findViewById(R.id.txt_steps);
        TvSteps.setText(TEXT_NUM_STEPS + numSteps);
    }
}
