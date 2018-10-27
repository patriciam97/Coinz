package milou.patricia.coinz;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Coin {
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String id,currency,markerSymbol;
    private double value;
    private LatLng latlng;
    private Marker marker;
    public Coin() {

    }
    public Coin(String id, String value, String currency, String mS,String lat, String lng) {
        this.id=id;
        this.value=Double.parseDouble(value);
        this.currency=currency;
        this.markerSymbol=mS;
        this.latlng= new LatLng(Double.parseDouble(lat),Double.parseDouble(lng));
        mAuth= FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        user= mAuth.getCurrentUser();
    }
    public void addMaker(Marker m){
        this.marker=m;
    }
    public void addInWallet(){
        Calendar in = Calendar.getInstance();
        SimpleDateFormat dformat = new SimpleDateFormat("dd/MM/yy");
        Map<String, Object> coin = new HashMap<>();
        coin.put("Currency",currency);
        coin.put("Value", value);
        coin.put("Collected Date",dformat.format(in.getTime()).toString());
        in.add(Calendar.DAY_OF_YEAR, 2);
        coin.put("Expiry Date",dformat.format(in.getTime()).toString());

        db.collection("Users").document(user.getEmail()).collection("Coins").document(id)
                .set(coin)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("coin", "Coin Added");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("coin", "Coin Not Added", e);
                    }
                });
    }
    public String getId(){
        return id;
    }
    public String getCurrency(){
        return currency;
    }
    public Double getValue(){
        return value;
    }
    public LatLng getLatLng(){
        return latlng;
    }
    public String getMarkerSymbol(){
        return markerSymbol;
    }
    public Marker getMarker(){
        return marker;
    }


}
