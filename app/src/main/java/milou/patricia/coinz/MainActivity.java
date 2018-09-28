package milou.patricia.coinz;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.mapboxsdk.Mapbox;
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

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,LocationEngineListener,PermissionsListener{
    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location locationOrigin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoicGF0cmljaWFtOTciLCJhIjoiY2pqaWl3aHFqMWMyeDNsbXh4MndnY3hzMiJ9.Fqn_9bmuScR4IqUrUbP6lA");
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

    }
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map=mapboxMap;
        enableLocation();
    }

    private void enableLocation(){
        if (permissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
            initializeLocationLayer();
        }else{
            permissionsManager=new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine(){
        locationEngine= new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation= locationEngine.getLastLocation();
        if(lastLocation!=null){
            locationOrigin=lastLocation;
            setCameraPosition(lastLocation);
        }else{
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
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),13.00));
    }
    @Override
    public void onLocationChanged(Location location) {
        if(location!=null){
            locationOrigin=location;
            setCameraPosition(location);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            enableLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @SuppressWarnings("MissingPermission")
    public void onStart() {
        super.onStart();
        if(locationEngine!=null){
            locationEngine.requestLocationUpdates();
        }
        if(locationLayerPlugin!=null){
            locationLayerPlugin.onStart();
        }
        mapView.onStart();
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
        if(locationEngine!=null){
            locationEngine.removeLocationUpdates();
        }
        if(locationLayerPlugin!=null){
            locationLayerPlugin.onStop();
        }
        mapView.onStop();
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
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }



}
