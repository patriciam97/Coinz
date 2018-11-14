package milou.patricia.coinz;

import android.annotation.SuppressLint;

import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class ShowFriends {
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    public ArrayList<String> friends= new ArrayList<>();
    private View v;
    private String coinid;

    /**
     * Constructor
     * @param view2 Current View
     * @param coinid  Coin id(can also be null)
     */
    ShowFriends(View view2, String coinid){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        user= mAuth.getCurrentUser();
        this.v=view2;
        this.coinid=coinid;
    }

    /**
     * This function retrives all friends of the user and presents them in a Table Layout.
     */
    @SuppressLint("SetTextI18n")
    public void showTable() {
        //get table
        TableLayout table=v.findViewById(R.id.table);
        //add friends in the table
        table.removeAllViews();
        db.collection("Users").document(user.getEmail()).collection("Friends")
                //order them by email
                .orderBy("Email")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int index = 1;
                        if (task.getResult().size() > 0) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //new row
                                TableRow row = new TableRow(v.getContext());
                                //alternate the background color of the rows
                                if (index % 2 == 0) {
                                    row.setBackgroundColor(Color.rgb(242, 242, 242));
                                }
                                //increment the counter of the rows
                                index++;
                                //design decisions of each row
                                row.setTextAlignment(TableRow.TEXT_ALIGNMENT_CENTER);
                                row.setOrientation(TableRow.HORIZONTAL);
                                row.setGravity(Gravity.CENTER_HORIZONTAL);
                                row.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                //for each row create a textview
                                TextView em = new TextView(v.getContext());
                                //and show the email of his friend
                                String email=document.getString("Email");
                                friends.add(email);
                                em.setText(email);
                                em.setTextSize(17);
                                em.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                //once the user clicks on the row
                                row.setOnClickListener(v -> {
                                    if (coinid==null) {
                                        //if not a coin was sent, then this class if accessed from the Friends Activity
                                        showProf(document.getString("Email"));
                                    }else{
                                        //else the user is trying to send him a coin
                                        sendcoin(email);
                                    }

                                });

                                //add the row in the table
                                table.addView(row);
                                row.addView(em);
                            }
                        } else {
                            //If the user has no friends at that time
                            TableRow row = new TableRow(v.getContext());
                            TextView tv = new TextView(v.getContext());
                            tv.setTextColor(Color.BLACK);
                            tv.setTextSize(16);
                            tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                            tv.setPadding(30, 30, 30, 30);
                            tv.setText("No friends.");
                            table.addView(row);
                            row.addView(tv);
                        }
                    }
                });
    }

    /**
     * This function sends a coin to the friend selected
     * @param toemail Friend selected email
     */
    private void sendcoin(String toemail) {
        //get coin's information
        db.collection("Users").document(user.getEmail()).collection("Coins").document(coinid)
                .get()
                .addOnCompleteListener(task -> {
                    DocumentSnapshot doc=task.getResult();
                    String currency=doc.getString("Currency");
                    Double val=doc.getDouble("Value");
                    String dateC=doc.getString("Collected Date");
                    String dateE=doc.getString("Expiry Date");
                    //add this document in the coins collection of the Friend he chose
                    addcoin(toemail,currency,val,dateC,dateE);
                    //delete this document
                    doc.getReference().delete();

                });

    }

    /**
     * This function creates a new coin and adds it in the Friend's Wallet
     * @param toemail friend's email
     * @param currency Coin's currency
     * @param val Coin's value
     * @param dateC Coin's collection date
     * @param dateE Coin's expiry date
     */
    private void addcoin(String toemail, String currency, Double val, String dateC, String dateE) {
        //create a new object, set gift=true
        Map<String, Object> coin = new HashMap<>();
        coin.put("Currency",currency);
        coin.put("Value", val);
        coin.put("Collected Date",dateC);
        coin.put("Expiry Date",dateE);
        coin.put("Gift",true);
        //add it
        db.collection("Users").document(toemail).collection("Coins").document(coinid)
                .set(coin)
                .addOnSuccessListener(aVoid -> {
                    Timber.d("Coin Sent");
                    Toast.makeText(v.getContext(),"Coin sent.",Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(v.getContext(),"Please try again later.",Toast.LENGTH_SHORT).show();
                    Timber.tag("coin").w(e, "Coin Not Sent");
                });
    }

    /**
     * This function is called only from the FriendsActivity, when the user clicks on an email.
     * In this case, more information is available for the selected user.
     * @param email email of selected user
     */
    private void showProf(String email){
        db.collection("Users").document(email).collection("Info")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()){
                            //get details
                            String name= document.getString("Full Name");
                            String number= document.getString("Telephone Number");
                            String dob= document.getString("Date of Birth");
                            //show them with a dialog
                            profpopup(email,name,dob,number);
                        }
                    }
                });
    }

    /**
     * Show the information available through a dialog.
     * @param email Email
     * @param name Full Name
     * @param dob Date Of Birth
     * @param number Phone Number
     */
    private void profpopup(String email,String name,String dob, String number){
        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        // Get the layout inflater
        LayoutInflater inflater = LayoutInflater.from(v.getContext());
        @SuppressLint("InflateParams") View view2= inflater.inflate(R.layout.popup2, null);
        TextView title=view2.findViewById(R.id.title);
        title.setText(email);
        TextView info =view2.findViewById(R.id.info);
        String text= "Full Name: "+name+"\nContact Number: "+number+"\nDate of Birth: "+dob;
        info.setText(text);
        info.setTextSize(18);
        builder.setView(view2).show();
    }
}
