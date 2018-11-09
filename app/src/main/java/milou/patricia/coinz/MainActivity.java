package milou.patricia.coinz;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;

import java.text.SimpleDateFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,LocationEngineListener,PermissionsListener,AsyncResponse{
    private ProgressBar pgsBar;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private MapView mapView;
    private MapboxMap map;
    private Button centremapbtn;
    private Boolean campus=false;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location locationOrigin;
    private String mapstr="";
    private JSONObject json;
    private IconFactory iconFactory;
    private Icon icon ;
    public static String shil,peny,dolr,quid;
    ArrayList<Coin> markers= new ArrayList<Coin>();
    private Bitmap[] IMarkers= new Bitmap[10];
    Coin closestcoin = new Coin();
    private String selected="";
    private MapboxNavigation navigation;
    private NavigationMapRoute navigationMapRoute;
    private boolean bottomSheetVisible = true;
    private NavigationView navigationView;
    private FloatingActionButton fabNightModeToggle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoicGF0cmljaWFtOTciLCJhIjoiY2pqaWl3aHFqMWMyeDNsbXh4MndnY3hzMiJ9.Fqn_9bmuScR4IqUrUbP6lA");
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        //get current date in the format 2018/10/09
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        String date1=dateFormat.format(date);
        //user
        mAuth=FirebaseAuth.getInstance();
        user= mAuth.getCurrentUser();

        String maplink = "http://www.homepages.inf.ed.ac.uk/stg/coinz/" +date1+ "/coinzmap.geojson";
        Log.i("map link", maplink);
        new DownloadFileTask(this).execute(maplink);
        iconFactory= IconFactory.getInstance(MainActivity.this);
        for(int i=0;i<10;i++){
            String x=Integer.toString(i);
            Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier("p"+x, "drawable", getPackageName()));
            IMarkers[i] = Bitmap.createScaledBitmap(imageBitmap,60, 60, false);
        }
        editNavigationBars();

        navigation = new MapboxNavigation(this, "pk.eyJ1IjoicGF0cmljaWFtOTciLCJhIjoiY2pvOTF6dm1iMGZsZTNxb3g1MDU0ZGtsYiJ9.R6jgvI9ZH7mUIIxwzJjW_Q");
    }

    private void editNavigationBars() {
        //the bottom one
        AHBottomNavigation bottomNavigation=findViewById(R.id.bottom_navigation);
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
        AHBottomNavigationItem item1 = new AHBottomNavigationItem(R.string.tab_1, R.drawable.profile, R.color.color_tab_1);
        AHBottomNavigationItem item2 = new AHBottomNavigationItem(R.string.tab_2, R.drawable.wallet, R.color.color_tab_1);
        AHBottomNavigationItem item3 = new AHBottomNavigationItem("Friends", R.drawable.friends,R.color.color_tab_1);
        AHBottomNavigationItem item4 = new AHBottomNavigationItem("Bank", R.drawable.piggybank,R.color.color_tab_1);

        // Add items
        bottomNavigation.addItem(item1);
        bottomNavigation.addItem(item2);
        bottomNavigation.addItem(item3);
        bottomNavigation.addItem(item4);
        // Change colors
        bottomNavigation.setAccentColor(Color.parseColor("#99d8d6"));
        bottomNavigation.setInactiveColor(Color.parseColor("#99d8d6"));

        bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                if (position==0){
                    Profile();
                }else if (position==1){
                    Wallet();
                }else if(position==2){
                    Friends();
                }else if(position==3){
                    Bank();
                }
                return true;
            }
        });

    }


    public void processFinish(String output){
        mapstr=output;
        try {
            ReadJSON();
            Log.i("async","done");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void ReadJSON()throws JSONException {
        JSONArray features;
        JSONObject rates;
        while(mapstr!="") {
            json = new JSONObject(mapstr);
            Log.i("map",mapstr);
            String dat= json.getString("date-generated");
            rates=json.getJSONObject("rates");
            shil =rates.getString("SHIL").toString();
            peny =rates.getString("PENY").toString();
            quid =rates.getString("QUID").toString();
            dolr =rates.getString("DOLR").toString();
           features=json.getJSONArray("features");
           for(int i=0;i<features.length();i++){
               JSONObject feature =features.getJSONObject(i);
               //get properties
               JSONObject properties =feature.getJSONObject("properties");
               String id =properties.getString("id");
               String value =properties.getString("value");
               String currency =properties.getString("currency");
               String markersymbol =properties.getString("marker-symbol");
               //String markercolor =properties.getString("marker-color");
                //get coordinates
               JSONObject geometry =feature.getJSONObject("geometry");
               JSONArray coordinates =geometry.getJSONArray("coordinates");
               String lng=coordinates.get(0).toString();
               String lat=coordinates.get(1).toString();
               //if that coin is included in Firebase(wallet) dont show it
               DocumentReference docRef = db.collection("Users").document(user.getEmail()).collection("Coins").document(id);
               docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                   @Override
                   public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                       if (task.isSuccessful()) {
                           DocumentSnapshot document = task.getResult();
                           if (document.exists()) {

                           } else {
                               icon = getNumIcon(markersymbol);
                               icon = getColourIcon(icon, currency);
                               Coin coin = new Coin(id, value, currency, markersymbol, lat, lng);
                               addMarker(coin, icon);
                           }
                       } else {
                           Log.d("wallet", "get failed with ", task.getException());
                       }
                   }
               });
           }
            break;
        }
        pgsBar = (ProgressBar)findViewById(R.id.pBar);
        pgsBar.setVisibility(View.GONE);
    }

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
    public Icon getColourIcon(Icon icon,String currency){
        final Bitmap bmp =icon.getBitmap();
        int [] allpixels = new int [bmp.getHeight() * bmp.getWidth()];
        bmp.getPixels(allpixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        int to=0;
        switch (currency){
            case "PENY": to =Color.rgb(255,0,0); //red
                            break;
            case "DOLR": to =Color.rgb(0,170,255); //blue
                            break;
            case "SHIL": to =Color.rgb(255,204,0);  //yellow
                            break;
            case "QUID": to =Color.rgb(0,153,51);  //green
                            break;
        }

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

    public void addMarker(Coin coin,Icon icon){
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                // One way to add a marker view
                MarkerOptions m= new MarkerOptions()
                        .position(coin.getLatLng())
                        .setIcon(icon);
                Marker marker= m.getMarker();
                coin.addMaker(marker);
                markers.add(coin);
                mapboxMap.addMarker(m);
            }
        });
    }
    public void removeMarker(Marker marker){
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mapboxMap.removeMarker(marker);
            }
        });
    }
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map=mapboxMap;
        enableLocation();
    }

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

    private void initializeLocationLayer(){
        locationLayerPlugin=new LocationLayerPlugin(mapView,map,locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
    }
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        locationEngine.requestLocationUpdates();

    }
    private void setCameraPosition(Location location){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),14.00));
    }
    public void centreMap(View view){
        Log.i("button","clicked");
        centremapbtn=(Button) findViewById(R.id.centremap);
        if (campus==false){
            campus=true; //whenever campus==true, then the map is focused on central campus
            final Location campuslocation = new Location("central campus");
            campuslocation.setLatitude(55.943877);
            campuslocation.setLongitude(-3.187479);
            setCameraPosition(campuslocation);
            centremapbtn.setText("MY LOCATION");
        }else{
            campus=false;//otherwise, then the map is focused on the user's location
            setCameraPosition(locationOrigin);
           centremapbtn.setText("CENTRAL CAMPUS");
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        if(location!=null){
            locationOrigin=location;
            setCameraPosition(locationOrigin); //was location before
        }
    }
    public Double getClosestCoin() {
        Double minimumDist=0.0;
        if (markers.size() > 0) {
            Location userLocation = locationOrigin;
            float[] result = new float[1];
            LatLng coin = markers.get(0).getLatLng();
            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                    coin.getLatitude(), coin.getLongitude(), result);
            minimumDist = (double) result[0];

            for (int x = 1; x < markers.size(); x++) {
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
    public void CheckIfClosetoACoin(View view) {
        Double minimumDist=getClosestCoin();
            if (minimumDist <= 25000) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                // Get the layout inflater
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                View view2= inflater.inflate(R.layout.popup2, null);
                TextView title =view2.findViewById(R.id.title);
                title.setText("Do you want to collect it?");
                TextView info =view2.findViewById(R.id.info);
                String text= closestcoin.getCurrency()+"   "+closestcoin.getValue();
                info.setText(text);
                builder.setView(view2)
                        // Add action buttons
                        .setPositiveButton("Yeah!", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                addCoin();
                            }
                        }).setNegativeButton("Nah", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
            }else{
                Toast.makeText(MainActivity.this,"Get closer!",Toast.LENGTH_SHORT).show();
            }
    }

    public void addCoin(){
        Toast.makeText(MainActivity.this,"New Coin Added", Toast.LENGTH_SHORT).show();
        closestcoin.addInWallet();
        markers.remove(closestcoin);
        removeMarker(closestcoin.getMarker());
    }

    public void navigateToClosestCoin(View view){
        Double mindist=getClosestCoin();
        while(closestcoin==null) {
            System.out.println("not yet");
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
                .setPositiveButton("Start Navigation", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Point destination = Point.fromLngLat(closestcoin.getLatLng().getLongitude(), closestcoin.getLatLng().getLatitude());
                        getRoute(destination);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    private void getRoute(Point dest){
        Point origin = Point.fromLngLat(locationOrigin.getLongitude(),locationOrigin.getLatitude());
        Point destination=dest;
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d("navigation", "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e("navigation", "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e("navigation", "No routes found");
                            return;
                        }

                        DirectionsRoute currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
//                        if (navigationMapRoute != null) {
//                            navigationMapRoute.removeRoute();
//                        } else {
//                            navigationMapRoute = new NavigationMapRoute(null, mapView, map, R.style.NavigationMapRoute);
//                        }
//                        navigationMapRoute.addRoute(currentRoute);
                        startNavigation(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e("navigation", "Error: " + throwable.getMessage());
                    }
                });
    }
    private void startNavigation(DirectionsRoute directionsRoute) {
        NavigationLauncherOptions.Builder navOptions = NavigationLauncherOptions.builder()
                        .directionsRoute(directionsRoute)
                        .enableOffRouteDetection(false)
                        .directionsProfile(DirectionsCriteria.PROFILE_WALKING);

        NavigationLauncher.startNavigation(MainActivity.this, navOptions.build());
        navigation.addNavigationEventListener(new NavigationEventListener() {
            @Override
            public void onRunning(boolean running) {
                if (!running){
                  //  navigationMapRoute.removeRoute();
                }
            }
        });
    }


    public void Profile(){
        Intent i = new Intent(MainActivity.this, ShowProf.class);
        startActivity(i);
    }
    public void Wallet(){
        Intent i = new Intent(MainActivity.this, WalletActivity.class);
        startActivity(i);
    }
    private void Friends() {
        Intent i = new Intent(MainActivity.this, FriendsActivity.class);
        startActivity(i);
    }
    private void Bank() {
        Intent i = new Intent(MainActivity.this, BankActivity.class);
        startActivity(i);
    }
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
    }

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
      //  FirebaseAuth.getInstance().signOut();
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

}
