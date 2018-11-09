package milou.patricia.coinz;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BankActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private LineChart chart;
    private TextView totaldep;
    private List<Entry> entries=new ArrayList<Entry>();
    private ArrayList<Double> ylabels= new ArrayList<Double>();
    private ArrayList<String> xlabels= new ArrayList<String>();
    private double totaldeposits=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank);
        mAuth=FirebaseAuth.getInstance();
        user= mAuth.getCurrentUser();
        chart = (LineChart) findViewById(R.id.chart);
        chart.getDescription().setEnabled(false);
        chart.setScaleEnabled(false);
        chart.getLegend().setEnabled(false);
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        chart.getAxisRight().setEnabled(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAvoidFirstLastClipping(true);
        //xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM");
                return sdf.format(new Date((long) value));
            }
        };
        //xAxis.setValueFormatter(formatter);
        plotGraph();

    }

    private void plotGraph() {
        db.collection("Users").document(user.getEmail()).collection("Bank")
                .orderBy("Deposited Date")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().size() > 0) {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                                String prevday = sdf.format(new Date(task.getResult().getDocuments().get(0).getLong("Deposited Date")));
                                double sum = 0;
                                int counter = 0;
                                long dnum = 0;
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String d = sdf.format(new Date(document.getLong("Deposited Date")));
                                    dnum = document.getLong("Deposited Date");
                                    if (d.equals(prevday)) {
                                        sum = sum + document.getDouble("Value");
                                        totaldeposits = totaldeposits + document.getDouble("Value");
                                    } else {
                                        entries.add(new Entry(dnum, (float) sum));
                                        ylabels.add(sum);
                                        xlabels.add(d);
                                        Log.i("point", document.getLong("Deposited Date") + " , " + sum);
                                        counter++;
                                        sum =document.getDouble("Value");
                                        prevday = d;
                                    }
                                }
                                entries.add(new Entry(dnum, (float) sum));
                                System.out.println("-------------------------" + entries.size() + "----------");

                                // Setting Data
                                LineDataSet dataSet = new LineDataSet(entries, null);
                                chart.setDescription(null);
                                LineData data = new LineData(dataSet);
                                chart.setData(data);
                                chart.invalidate();
                                totaldep = (TextView) findViewById(R.id.totaltxt);
                                DecimalFormat df = new DecimalFormat("#.#####");
                                df.setRoundingMode(RoundingMode.CEILING);
                                //System.out.println(df.format(newval));
                                totaldep.setText("Total Amount of Deposits: " + df.format(totaldeposits));
                            } else {
                                Log.d("document", "get failed with ", task.getException());
                            }
                        }
                    }
                });

    }
}
