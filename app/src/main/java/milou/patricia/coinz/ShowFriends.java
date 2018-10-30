package milou.patricia.coinz;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ShowFriends {
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private View v;
    private String coinid;
    public ShowFriends(View view2,String coinid){
        mAuth= FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        user= mAuth.getCurrentUser();
        this.v=view2;
        //if this class was created though the friend activity this variable needs to be true
        this.coinid=coinid;
    }
    public void showTable() {
        //get table
        TableLayout table=v.findViewById(R.id.table);
        //add friends in the table
        table.removeAllViews();
        db.collection("Users").document(user.getEmail()).collection("Friends")
                .orderBy("Email")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
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
                                    em.setText(email);
                                    em.setTextSize(17);
                                    em.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    //once the user clicks on the row
                                    row.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (coinid==null) {
                                                showProf(document.getString("Email"));
                                            }else{
                                                sendcoin(email);
                                            }

                                        }
                                    });

                                    //add the row in the table
                                    table.addView(row);
                                    row.addView(em);
                                }
                            } else {
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
                    }
                });
    }

    private void sendcoin(String toemail) {
        db.collection("Users").document(user.getEmail()).collection("Coins").document(coinid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot doc=task.getResult();
                        String currency=doc.getString("Currency");
                        Double val=doc.getDouble("Value");
                        String dateC=doc.getString("Collected Date");
                        String dateE=doc.getString("Expiry Date");
                        //add this document in the coins collection of the Friend he chose
                        addcoin(toemail,currency,val,dateC,dateE);
                        //delete this document
                        doc.getReference().delete();
                    }
                });
    }

    private void addcoin(String toemail, String currency, Double val, String dateC, String dateE) {
        Map<String, Object> coin = new HashMap<>();
        coin.put("Currency",currency);
        coin.put("Value", val);
        coin.put("Collected Date",dateC);
        coin.put("Expiry Date",dateE);
        coin.put("Gift",true);

        db.collection("Users").document(toemail).collection("Coins").document(coinid)
                .set(coin)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("coin", "Coin Added");
                        Toast.makeText(v.getContext(),"Coin sent.",Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("coin", "Coin Not Added", e);
                    }
                });
    }


    public void showProf(String email){
        db.collection("Users").document(email).collection("Info")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()){
                                String name= document.getString("Full Name");
                                String number= document.getString("Telephone Number");
                                String dob= document.getString("Date of Birth");
                                profpopup(email,name,dob,number);
                            }
                        }else{

                        }
                    }
                });
    }
    private void profpopup(String email,String name,String dob, String number){
        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        // Get the layout inflater
        LayoutInflater inflater = LayoutInflater.from(v.getContext());
        View view2= inflater.inflate(R.layout.popup2, null);
        TextView title=view2.findViewById(R.id.title);
        title.setText(email);
        TextView info =view2.findViewById(R.id.info);
        String text= "Full Name: "+name+"\nContact Number: "+number+"\nDate of Birth: "+dob;
        info.setText(text);
        info.setTextSize(18);
        builder.setView(view2).show();
    }
}
