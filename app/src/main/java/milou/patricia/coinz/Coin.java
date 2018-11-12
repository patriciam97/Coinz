package milou.patricia.coinz;

import android.annotation.SuppressLint;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

public class Coin {
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    //coin's properties
    private String id,currency;
    private double value;
    private LatLng latlng;
    private Marker marker;
    Coin(){

    }
    Coin(String id, String value, String currency, String lat, String lng) {
        this.id=id;
        this.value=Double.parseDouble(value);
        this.currency=currency;
        this.latlng= new LatLng(Double.parseDouble(lat),Double.parseDouble(lng));
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
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
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dformat = new SimpleDateFormat("dd/MM/yy");
        Map<String, Object> coin = new HashMap<>();
        coin.put("Currency",currency);
        coin.put("Value", value);
        coin.put("Collected Date", dformat.format(in.getTime()));
        //set the expiry date to be 2 days later
        in.add(Calendar.DAY_OF_YEAR, 2);
        coin.put("Expiry Date", dformat.format(in.getTime()));

        db.collection("Users").document(Objects.requireNonNull(user.getEmail())).collection("Coins").document(id)
                .set(coin)
                .addOnSuccessListener(aVoid -> Timber.d("Added In Wallet"))
                .addOnFailureListener(e -> Timber.tag("Coin").w(e, "Not Added in Wallet"));
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
    public Marker getMarker(){
        return marker;
    }


}
