package milou.patricia.coinz;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class WalletActivity extends AppCompatActivity {
    //firebase objects
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    //other
    private Date date;
    private Spinner dropdown,dropdown2;
    private int coinsdepositedtoday=0;
    protected TableLayout table;
    private TextView curr,val,dateC,dateE;

    /**
     * This function is run once the activity is created
     * @param savedInstanceState Saved Instance State
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        //connect to Firebase get current user
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        user= mAuth.getCurrentUser();
        //Get current Date
        Calendar in = Calendar.getInstance();
        //date=dformat.format(in.getTime());
        date=in.getTime();
        //remove expired coins
        removeExpiredCoins();
        //checkforgifts
        checkgifts();
        //Show today's exchange rates
        String rates= "Exchange Rates: \n"+"PENY:   "+MainActivity.peny+"\nQUID:    "+MainActivity.quid+"\nSHIL:   "+MainActivity.shil+"\nDOLR:    "+MainActivity.dolr;
        TextView r= findViewById(R.id.rates);
        r.setTextSize(16);
        r.setText(rates);
        r.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        //set deposits counter
        coinscounter();
        //settings of spinners
        dropdown = findViewById(R.id.orderSpinner);
        dropdown2 = findViewById(R.id.orderSpinner2);
        String[] items = new String[]{"Currency","Collected Date","Expiry Date","Value"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        items = new String[]{"Ascending","Descending"};
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown2.setAdapter(adapter);
        table = findViewById(R.id.table1);
        //default selections
        dropdown.setSelection(0);
        dropdown2.setSelection(0);
        //show table with coins
        showTable("Currency", Query.Direction.ASCENDING);

    }

    /**
     * This function counts the number of coins deposited today
     */
    @SuppressLint("DefaultLocale")
    private void coinscounter() {

        db.collection("Users").document(user.getEmail()).collection("Bank")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        //find all coins that have have Depodited date as today
                        for (QueryDocumentSnapshot document : task.getResult()) {

                            @SuppressLint("SimpleDateFormat") SimpleDateFormat df=new SimpleDateFormat("dd/MM/yyyy");
                            String d=df.format(document.getDate("Deposited Date"));
                            String today=df.format(date);
                            if(today.equals(d)){
                                //increase counter
                                coinsdepositedtoday++;
                            }

                        }
                        //update textview
                        TextView deposits= findViewById(R.id.deposits);
                        deposits.setText(String.format("Daily Deposits Made:%d", coinsdepositedtoday));
                    }
                });

    }

    /**
     * This function reads both spinners which are included in the activity
     * The 2 choices read affect how the table will be ordered.
     * @param view Current view
     */
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

    /**
     * This function removes the coins that have expired.
     */
    @SuppressLint("SimpleDateFormat")
    public void removeExpiredCoins (){
        //get the references of the documents where the expiry date has passed and delete them
        db.collection("Users").document(user.getEmail()).collection("Coins")
                .get()
                .addOnCompleteListener(task -> {
                    Date exp=Calendar.getInstance().getTime();
                    int counter=0;
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                exp=new SimpleDateFormat("dd/MM/yyyy").parse(document.getString("Expiry Date"));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            if (date.before(exp)){
                                //delete it and increase counter of expired coins
                                counter++;
                                document.getReference().delete();
                            }
                        }
                        //Show message to the user informing him of how many coins expired.
                        if(counter>1) {
                                Toast.makeText(WalletActivity.this, counter + " coins expired.", Toast.LENGTH_LONG).show();
                            }else if(counter==1){
                                Toast.makeText(WalletActivity.this, counter + " coin expired.", Toast.LENGTH_LONG).show();
                            }

                    } else {
                        //if an exception occurs
                        Timber.e(task.getException());
                    }
                });

    }

    /**
     * This function checks if the user has received any coins from other users
     */
    public void checkgifts(){
        db.collection("Users").document(user.getEmail()).collection("Coins")
                .whereEqualTo("Gift",true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for(QueryDocumentSnapshot doc:task.getResult()){
                            Map<String,Object> map=doc.getData();
                            //change this value so that the user gets informed just once
                            map.replace("Gift",true,false);
                            doc.getReference().update(map);
                        }
                        //show a message to the user
                        if(task.getResult().size()>0) {
                            if(task.getResult().size()>1) {
                                Toast.makeText(WalletActivity.this, "You received "+task.getResult().size() + " coins.", Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(WalletActivity.this, "You received "+task.getResult().size() + " coin.", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        //if an exception occurs
                        Timber.e(task.getException());
                    }
                });
    }

    /**
     * This function retrieves the data from the firebase in the order that the user chose.
     * At the same time , the table is created.
     * @param order Order by what field
     * @param ord Ascending/Descending
     */
    @SuppressLint("SetTextI18n")
    public void showTable(String order, Query.Direction ord){
        table.removeAllViews();
        //retrieve coins
        db.collection("Users").document(user.getEmail()).collection("Coins")
                .orderBy(order,ord)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int index=1;
                        if(task.getResult().size()>0) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //for each one add a row in the table
                                //new row design choices
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
                                if(document.contains("Gift")){
                                    if(!document.getBoolean("Gift")) {
                                        setTextViews(true);
                                    }else{
                                        setTextViews(false);
                                    }
                                }else{
                                    setTextViews(false);
                                }
                                //fill each column
                                String currency=document.getString("Currency");
                                Double value=document.getDouble("Value");
                                Boolean gift=document.contains("Gift");
                                if(gift){
                                    curr.setText(currency+"(*)");
                                }else {
                                    curr.setText(currency);
                                }
                                DecimalFormat df = new DecimalFormat("#.####");
                                val.setText(df.format(value));
                                dateC.setText(document.getString("Collected Date"));
                                dateE.setText(document.getString("Expiry Date"));


                                //once the user clicks on the row
                                row.setOnClickListener(v -> coinfunction(document));

                                //add the row in the table
                                table.addView(row);
                                row.addView(curr);
                                row.addView(val);
                                row.addView(dateC);
                                row.addView(dateE);
                            }
                        }else{
                            //if no coins were collected at all
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
                        Timber.e(task.getException());
                    }
                });
    }

    /**
     * If the user clicks on a coin, this function is run
     * which allows the user to delete the coin,deposit the coin or sent it to one of his friends.
     * @param document Document of a chosen coin
     */
    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void coinfunction(QueryDocumentSnapshot document) {
        LayoutInflater inflater = WalletActivity.this.getLayoutInflater();
        @SuppressLint("InflateParams") View view2= inflater.inflate(R.layout.coinpopup, null);
        Dialog dialog = new AlertDialog.Builder(this).setView(view2).show();
        TextView title =view2.findViewById(R.id.cointitle);
        title.setText(document.getString("Currency")+": "+document.getDouble("Value"));
        TextView delete =view2.findViewById(R.id.deletecoin);
        //if the user selects to delete the coin
        delete.setOnClickListener(v -> document.getReference().delete()
                .addOnSuccessListener(aVoid -> {
                    Timber.d("DocumentSnapshot successfully deleted!");
                    //after deleting it update the table
                    readSpinner(getCurrentFocus());
                    Toast.makeText(view2.getContext(),"Coin removed.",Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Timber.tag("DeleteCoin").w(e, "Error deleting document")));
        //if the user selects to send the coin to a friend
        TextView send =view2.findViewById(R.id.send);
        send.setOnClickListener((View v) -> {
            LayoutInflater inflater1 = WalletActivity.this.getLayoutInflater();
            @SuppressLint("InflateParams") View view3= inflater1.inflate(R.layout.friendspopup, null);
            new AlertDialog.Builder(WalletActivity.this).setView(view3).show();
            //sending the coin is included in the ShowFriends class
            //show list of friends and once someone is selected the coin will be sent.
            ShowFriends sf= new ShowFriends(view3, document.getId());
            sf.showTable();
        });

      //if the user selects to deposit the coin into his/her bank account
        TextView deposit =view2.findViewById(R.id.deposit);
        deposit.setOnClickListener(v -> {
            if (coinsdepositedtoday < 26) {
            //get value and currency of coin
                Double newval = document.getDouble("Value");
                String curr = document.getString("Currency");
                //change it to a gold coin
                switch (curr) {
                    case "DOLR":
                        newval = newval * Double.parseDouble(MainActivity.dolr);
                        break;
                    case "PENY":
                        newval = newval * Double.parseDouble(MainActivity.peny);
                        break;
                    case "QUID":
                        newval = newval * Double.parseDouble(MainActivity.quid);
                        break;
                    case "SHIL":
                        newval = newval * Double.parseDouble(MainActivity.shil);
                        break;
                }
                //create a new mapping object
                Map<String, Object> coin = new HashMap<>();
                coin.put("Currency", "GOLD");
                coin.put("Value", newval);
                @SuppressLint("SimpleDateFormat") DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                Date dateWithnoTime=new Date();
                try {
                    dateWithnoTime = formatter.parse(formatter.format(date));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                coin.put("Deposited Date",dateWithnoTime);
                //add it in the user's bank account
                db.collection("Users").document(user.getEmail()).collection("Bank").document(document.getId()).set(coin)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(WalletActivity.this, "Coin deposited.", Toast.LENGTH_SHORT).show();
                                //delete that coin from the user's wallet
                                document.getReference().delete();
                                readSpinner(getCurrentFocus());
                                dialog.dismiss();
                                //update deposits counter
                                coinsdepositedtoday++;
                                TextView deposits = findViewById(R.id.deposits);
                                deposits.setText(String.format("Daily Deposits Made:%d", coinsdepositedtoday));

                            }
                        });

            }else{
                //if the user has already deposited 25 coins in that day, he/she cannot deposit more.
                Toast.makeText(WalletActivity.this,"25 coins have been already deposited. Try again tomorrow.",Toast.LENGTH_SHORT).show();
                readSpinner(getCurrentFocus());
                dialog.dismiss();
            }
        });

    }
    /*
        This function set the design decisions for each row in the table

     */
    public void setTextViews(Boolean gift){
        int col;
        //if a coin was a gift it will have a different color
        if (gift){
            col= Color.rgb(	153, 216, 214);
        }else{
            col=Color.BLACK;
        }
        //setting the textviews
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
