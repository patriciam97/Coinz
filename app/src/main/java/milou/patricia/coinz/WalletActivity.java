package milou.patricia.coinz;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WalletActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Coin newC;
    private Spinner dropdown,dropdown2;
    private TableLayout table;
    protected ArrayList<Marker> markers= new ArrayList<Marker>();
    private IconFactory iconFactory;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        mAuth= FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        user= mAuth.getCurrentUser();
        //exchange rates
        String rates= "Exchange Rates: \n"+"PENY:   "+MainActivity.peny+"\nQUID:    "+MainActivity.quid+"\nSHIL:   "+MainActivity.shil+"\nDOLR:    "+MainActivity.dolr;
        TextView r= (TextView) findViewById(R.id.rates);
        r.setTextSize(16);
        r.setText(rates);
        r.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        dropdown = findViewById(R.id.orderSpinner);
        dropdown2 = findViewById(R.id.orderSpinner2);
        String[] items = new String[]{"Currency","Collected Date","Expiry Date","Value"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        items = new String[]{"Ascending","Descending"};
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown2.setAdapter(adapter);
        table = (TableLayout) findViewById(R.id.table1);
        dropdown.setSelection(0);
        dropdown2.setSelection(0);
        showTable("Currency", Query.Direction.ASCENDING);
    }

    public void readSpinner(View view){
        int position= dropdown.getSelectedItemPosition();
        String order="";
        Query.Direction ord= Query.Direction.ASCENDING;
        switch (position) {
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
            case 0:
                ord= Query.Direction.ASCENDING;
                break;
            case 1:
                ord= Query.Direction.DESCENDING;
                break;
        }
        showTable(order,ord);
    }

    public void showTable(String order,Query.Direction ord){
        //get all coins and for each coin add it in the table

        table.removeAllViews();

        db.collection("Users").document(user.getEmail()).collection("Coins")
                .orderBy(order,ord)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                TableRow row = new TableRow(WalletActivity.this);
                                row.setTextAlignment(TableRow.TEXT_ALIGNMENT_CENTER);
                                row.setOrientation(TableRow.HORIZONTAL);
                                row.setGravity(Gravity.CENTER_HORIZONTAL);
                                row.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                row.setPadding(0,0,0,10);
                                TextView curr = new TextView(WalletActivity.this);
                                curr.setTextColor(Color.BLACK);
                                curr.setTextSize(16);
                                curr.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                                curr.setPadding(40,40,40,40);
                                //curr.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                TextView val = new TextView(WalletActivity.this);
                                val.setTextColor(Color.BLACK);
                                val.setTextSize(16);
                                val.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                                val.setPadding(40,40,40,40);
                                //val.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                TextView dateC = new TextView(WalletActivity.this);
                                dateC.setTextColor(Color.BLACK);
                                dateC.setTextSize(16);
                                dateC.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                                dateC.setPadding(40,40,40,40);
                                //dateC.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                TextView dateE = new TextView(WalletActivity.this);
                                dateE.setTextColor(Color.BLACK);
                                dateE.setTextSize(16);
                                dateE.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                                dateE.setPadding(40,40,40,40);
                                //dateE.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                curr.setText(document.getString("Currency"));
                                DecimalFormat df = new DecimalFormat("#.####");
                                val.setText(df.format(document.getDouble("Value")));
                                dateC.setText(document.getString("Collected Date").toString());
                                dateE.setText(document.getString("Expiry Date").toString());
                                Map<String, Object> data = document.getData();
                                row.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Toast.makeText(WalletActivity.this,"ok",Toast.LENGTH_SHORT).show();
                                    }
                                });
                                table.addView(row,table.getChildCount());
                                row.addView(curr);
                                row.addView(val);
                                row.addView(dateC);
                                row.addView(dateE);
                            }
                        } else {

                        }
                    }
                });
    }
}
