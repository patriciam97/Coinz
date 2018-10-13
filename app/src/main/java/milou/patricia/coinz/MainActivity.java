package milou.patricia.coinz;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;

import java.text.SimpleDateFormat;


import java.util.List;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,LocationEngineListener,PermissionsListener,AsyncResponse{
    private FirebaseAuth mAuth;
    private static MapView mapView;
    private MapboxMap map;
    private Button centremapbtn;
    private Boolean campus=false;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location locationOrigin;
    private static String mapstr="";
    private static JSONObject json;
    private static IconFactory iconFactory;
    private static Bitmap resizedPeny,resizedShil,resizedDolr,resizedQuid;
    private static Icon icon;

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

        String maplink = "http://www.homepages.inf.ed.ac.uk/stg/coinz/" +date1+ "/coinzmap.geojson";
        Log.i("map link", maplink);
        DownloadFileTask dft = new DownloadFileTask();
        dft.execute(maplink);
        iconFactory= IconFactory.getInstance(MainActivity.this);
//        iconPeny= iconFactory.fromResource(R.drawable.iconpenny);
//        iconDolr= iconFactory.fromResource(R.drawable.icondolr);
//        iconShil= iconFactory.fromResource(R.drawable.iconshelly);
//        iconQuid= iconFactory.fromResource(R.drawable.iconquid);
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier("iconpenny", "drawable", getPackageName()));
        resizedPeny = Bitmap.createScaledBitmap(imageBitmap,50, 50, false);
        imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier("icondolr", "drawable", getPackageName()));
        resizedDolr = Bitmap.createScaledBitmap(imageBitmap,50, 50,  false);
        imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier("iconshelly", "drawable", getPackageName()));
        resizedShil = Bitmap.createScaledBitmap(imageBitmap,50, 50,  false);
        imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier("iconquid", "drawable", getPackageName()));
        resizedQuid = Bitmap.createScaledBitmap(imageBitmap,50, 50,  false);

    }


    public static void processFinish(String output){
        mapstr=output;
        try {
            addMarkers();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public static void addMarkers()throws JSONException {
        JSONArray features;
        while(mapstr!="") {
            json = new JSONObject(mapstr);
            String dat= json.getString("date-generated");
           features=json.getJSONArray("features");
           for(int i=0;i<features.length();i++){
               JSONObject feature =features.getJSONObject(i);
               //get properties
               JSONObject properties =feature.getJSONObject("properties");
               String id =properties.getString("id");
               String value =properties.getString("value");
               String currency =properties.getString("currency");
               String markersymbol =properties.getString("marker-symbol");
               String markercolor =properties.getString("marker-color");
                //get coordinates
               JSONObject geometry =feature.getJSONObject("geometry");
               JSONArray coordinates =geometry.getJSONArray("coordinates");
               String lng=coordinates.get(0).toString();
               String lat=coordinates.get(1).toString();
               //icon to use
               switch (currency) {
                   case "SHIL":icon=iconFactory.fromBitmap(resizedShil);
                                break;
                   case "PENY":icon=iconFactory.fromBitmap(resizedPeny);
                                 break;
                   case "QUID":icon=iconFactory.fromBitmap(resizedQuid);
                                 break;
                   case "DOLR":icon=iconFactory.fromBitmap(resizedDolr);
                                break;
               }
               mapView.getMapAsync(new OnMapReadyCallback() {
                   @Override
                   public void onMapReady(MapboxMap mapboxMap) {
                       // One way to add a marker view
                       mapboxMap.addMarker(new MarkerOptions()
                               .position(new LatLng(Double.parseDouble(lat),Double.parseDouble(lng)))
                               .title(value.substring(0,1))
                               .snippet(currency)
                               .setIcon(icon));
                       }
               });

           }
            break;
        }
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
        FirebaseAuth.getInstance().signOut();
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
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

}
