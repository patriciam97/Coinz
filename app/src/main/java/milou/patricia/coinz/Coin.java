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
    //Firebase objects
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    //coin's properties
    private String id,currency,markerSymbol,markerColour;
    private double value;
    private LatLng latlng;
    private Marker marker;
    public Coin() {

    }
    public Coin(String id, String value, String currency, String mS,String mC,String lat, String lng) {
        this.id=id;
        this.value=Double.parseDouble(value);
        this.currency=currency;
        this.markerSymbol=mS;
        this.markerColour=mC;
        this.latlng= new LatLng(Double.parseDouble(lat),Double.parseDouble(lng));
        mAuth= FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        user= mAuth.getCurrentUser();
    }

    /**
     * This function saves the marker for this instance of the coin.
     * @param m Marker object
     */
    public void addMaker(Marker m){

        this.marker=m;
    }

    /**
     * This function saves the coin in the user's wallet(Firebase)
     */
    public void addInWallet(){
        Calendar in = Calendar.getInstance();
        SimpleDateFormat dformat = new SimpleDateFormat("dd/MM/yy");
        Map<String, Object> coin = new HashMap<>();
        coin.put("Currency",currency);
        coin.put("Value", value);
        coin.put("Collected Date",dformat.format(in.getTime()).toString());
        //set the expiry date to be 2 days later
        in.add(Calendar.DAY_OF_YEAR, 2);
        coin.put("Expiry Date",dformat.format(in.getTime()).toString());

        db.collection("Users").document(user.getEmail()).collection("Coins").document(id)
                .set(coin)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Coin", "Added In Wallet");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Coin", "Not Added in Wallet", e);
                    }
                });
    }
    //getters
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
    public String getMarkerColour(){
        return markerColour;
    }
    public Marker getMarker(){
        return marker;
    }


}
