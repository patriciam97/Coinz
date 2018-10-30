package milou.patricia.coinz;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

public class WalletActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String date;
    private Spinner dropdown,dropdown2;
    private int pos1,pos2;
    private TableLayout table;
    private TextView curr,val,dateC,dateE;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        //connect to Firebase get current user
        mAuth= FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        user= mAuth.getCurrentUser();
        //Get current Date
        Calendar in = Calendar.getInstance();
        SimpleDateFormat dformat = new SimpleDateFormat("dd/MM/yy");
        date=dformat.format(in.getTime());
        //remove expired coins
        removeExpiredCoins ();
        //checkforgifts
        checkgifts();
        //Show today's exchange rates
        String rates= "Exchange Rates: \n"+"PENY:   "+MainActivity.peny+"\nQUID:    "+MainActivity.quid+"\nSHIL:   "+MainActivity.shil+"\nDOLR:    "+MainActivity.dolr;
        TextView r= (TextView) findViewById(R.id.rates);
        r.setTextSize(16);
        r.setText(rates);
        r.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        //settings of spinners
        dropdown = findViewById(R.id.orderSpinner);
        dropdown2 = findViewById(R.id.orderSpinner2);
        String[] items = new String[]{"Currency","Collected Date","Expiry Date","Value"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        items = new String[]{"Ascending","Descending"};
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown2.setAdapter(adapter);

        table = (TableLayout) findViewById(R.id.table1);
        //default selections
        dropdown.setSelection(0);
        dropdown2.setSelection(0);
        showTable("Currency", Query.Direction.ASCENDING);
    }

    public void readSpinner(View view){
        //gets index of selected values of the 2 spinners and accordingly runs the showTable method.
        int position= dropdown.getSelectedItemPosition();
        String order="";
        Query.Direction ord= Query.Direction.ASCENDING;
        switch (position) {
            //options for the first spinner
            case 0:
                order="Currency";
                break;
            case 1:
                order="Collected Date";
                break;
            case 2:
                order="Expiry Date";
                break;
            case 3:
                order="Value";
        }
        position= dropdown2.getSelectedItemPosition();
        switch (position) {
            //oprions for second spinner
            case 0:
                ord= Query.Direction.ASCENDING;
                break;
            case 1:
                ord= Query.Direction.DESCENDING;
                break;
        }
        showTable(order,ord);
    }

    public void removeExpiredCoins (){
        //get the references of the documents where the expiry date has passed and delete them
        db.collection("Users").document(user.getEmail()).collection("Coins")
                .whereLessThan("Expiry Date", date)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if(task.getResult().size()>0) {
                                //if documents found
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    //delete the reference
                                    document.getReference().delete();
                                }
                                if(task.getResult().size()>1) {
                                    Toast.makeText(WalletActivity.this, task.getResult().size() + " coins expired.", Toast.LENGTH_LONG).show();
                                }else{
                                    Toast.makeText(WalletActivity.this, task.getResult().size() + " coin expired.", Toast.LENGTH_LONG).show();
                                }
                            }else{
                                Toast.makeText(WalletActivity.this, "No coins expired.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            //if an exception occurs
                            Log.e("Exception",task.getException().getMessage());
                        }
                    }});

    }
    public void checkgifts(){
        db.collection("Users").document(user.getEmail()).collection("Coins")
                .whereEqualTo("Gift",true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for(QueryDocumentSnapshot doc:task.getResult()){
                                Map<String,Object> map=doc.getData();
                                map.replace("Gift",true,false);
                                doc.getReference().update(map);
                            }
                            if(task.getResult().size()>0) {
                                if(task.getResult().size()>1) {
                                    Toast.makeText(WalletActivity.this, "You received "+task.getResult().size() + " coins.", Toast.LENGTH_LONG).show();
                                }else{
                                    Toast.makeText(WalletActivity.this, "You received "+task.getResult().size() + " coin.", Toast.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            //if an exception occurs
                            Log.e("Exception",task.getException().getMessage());
                        }
                    }});
    }

    public void showTable(String order,Query.Direction ord){
        //get all coins frrom the databse
        // order them as specified by the user
        //and for each one add a row in the table
        table.removeAllViews();

        db.collection("Users").document(user.getEmail()).collection("Coins")
                .orderBy(order,ord)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int index=1;
                            if(task.getResult().size()>0) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    //new row
                                    TableRow row = new TableRow(WalletActivity.this);
                                    if(index % 2==0){
                                        row.setBackgroundColor(Color.rgb(242, 242, 242));
                                    }
                                    index++;
                                    row.setTextAlignment(TableRow.TEXT_ALIGNMENT_CENTER);
                                    row.setOrientation(TableRow.HORIZONTAL);
                                    row.setGravity(Gravity.CENTER_HORIZONTAL);
                                    row.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    //set the format of the Text Views
                                    if(document.contains("Gift")==true){
                                        if(document.getBoolean("Gift")==false) {
                                            setTextViews(true);
                                        }else{
                                            setTextViews(false);
                                        }
                                    }else{
                                        setTextViews(false);
                                    }
                                    //fill them in
                                    String currency=document.getString("Currency");
                                    Double value=document.getDouble("Value");
                                    curr.setText(currency);
                                    DecimalFormat df = new DecimalFormat("#.####");
                                    val.setText(df.format(value));
                                    dateC.setText(document.getString("Collected Date").toString());
                                    dateE.setText(document.getString("Expiry Date").toString());
                                    Boolean gift=document.getBoolean("Gift");

                                    //once the user clicks on the row
                                    row.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            coinfunction(document.getId(),currency,value);
                                        }
                                    });

                                    //add the row in the table
                                    table.addView(row);
                                    row.addView(curr);
                                    row.addView(val);
                                    row.addView(dateC);
                                    row.addView(dateE);
                                }
                            }else{
                                TableRow row = new TableRow(WalletActivity.this);
                                TextView tv = new TextView(WalletActivity.this);
                                tv.setTextColor(Color.BLACK);
                                tv.setTextSize(16);
                                tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                                tv.setPadding(30,30,30,30);
                                tv.setText("No coins.");
                                table.addView(row);
                                row.addView(tv);
                            }
                        } else {
                            //if an exception occurs
                            Log.e("Exception",task.getException().getMessage());
                        }
                    }
                });
    }

    private void coinfunction(String id,String curr,Double value) {
        LayoutInflater inflater = WalletActivity.this.getLayoutInflater();
        View view2= inflater.inflate(R.layout.coinpopup, null);
        Dialog dialog = new AlertDialog.Builder(this).setView(view2).show();
        //View view2= inflater.inflate(R.layout.coinpopup, null);
        TextView title =view2.findViewById(R.id.cointitle);
        title.setText(curr+": "+value);
        //delete coin from wallet
        TextView delete =view2.findViewById(R.id.deletecoin);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("Users").document(user.getEmail()).collection("Coins").document(id)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("DeleteCoin", "DocumentSnapshot successfully deleted!");
                                readSpinner(getCurrentFocus());
                                Toast.makeText(view2.getContext(),"Coin removed.",Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("DeleteCoin", "Error deleting document", e);
                            }
                        });
            }
        });
        //send coin to a friend
        TextView send =view2.findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //show list of friends
                LayoutInflater inflater = WalletActivity.this.getLayoutInflater();
                View view3= inflater.inflate(R.layout.friendspopup, null);
                Dialog dialog2 = new AlertDialog.Builder(WalletActivity.this).setView(view3).show();
                //add friends in the table
                ShowFriends sf= new ShowFriends(view3,id);
                sf.showTable();
            }
        });

    }

    public void setTextViews(Boolean gift){
        int col=0;
        if (gift==true){
            col= Color.rgb(	153, 216, 214);
        }else{
            col=Color.BLACK;
        }
        curr = new TextView(WalletActivity.this);
        curr.setTextColor(col);
        curr.setTextSize(16);
        curr.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        curr.setPadding(35, 35, 35, 35);

        val = new TextView(WalletActivity.this);
        val.setTextColor(col);
        val.setTextSize(16);
        val.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        val.setPadding(35, 35, 35, 35);

        dateC = new TextView(WalletActivity.this);
        dateC.setTextColor(col);
        dateC.setTextSize(16);
        dateC.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        dateC.setPadding(35, 35, 35, 35);

        dateE = new TextView(WalletActivity.this);
        dateE.setTextColor(col);
        dateE.setTextSize(16);
        dateE.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        dateE.setPadding(35, 35, 35, 35);

    }
}
