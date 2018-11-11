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
import java.sql.Time;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class BankActivity extends AppCompatActivity {
    //Firebse objects
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    //Objects used to draw the line chart
    private LineChart chart;
    private List<Entry> entries=new ArrayList<Entry>();
    private XAxis xAxis;
    //other
    private TextView totaldep;
    private double totaldeposits=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank);
        //get current user
        mAuth=FirebaseAuth.getInstance();
        user= mAuth.getCurrentUser();
        //chart properties
        chart = (LineChart) findViewById(R.id.chart);
        chart.getDescription().setEnabled(false);
        chart.setScaleEnabled(false);
        chart.getLegend().setEnabled(false);
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        chart.getAxisRight().setEnabled(false);
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAvoidFirstLastClipping(true);
       // xAxis.setGranularity(0f); // minimum axis-step (interval) is 1
        xAxis.setTextSize(8);
        xAxis.setLabelRotationAngle(-30);
        xAxis.setAvoidFirstLastClipping(false);
        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
                return sdf.format(new Date((long) value));
            }
        };
        xAxis.setValueFormatter(formatter);
        //plot the graph
        plotGraph();

    }

    /**
     * This function goes through all coins saved in the user's bank account and plot the graph
     */
    private void plotGraph() {
        db.collection("Users").document(user.getEmail()).collection("Bank")
                .orderBy("Deposited Date")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            //if there are some coins stored
                            if (task.getResult().size() > 0) {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                                String prevday = sdf.format(task.getResult().getDocuments().get(0).getDate("Deposited Date"));
                                double sum = 0;
                                Date dnum =new Date();

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String d = sdf.format(document.getDate("Deposited Date"));
                                    dnum = document.getDate("Deposited Date");
                                    //dnum.setTime(0);
                                    if (d.equals(prevday)) {
                                        sum = sum + document.getDouble("Value");
                                        totaldeposits = totaldeposits + document.getDouble("Value");
                                    } else {
                                        //set entry date to that date on midnight so that plotting si more accurate
                                        Calendar entrydate = new GregorianCalendar();
                                        entrydate.setTime(dnum);
                                        entrydate.set(Calendar.HOUR_OF_DAY, 0);
                                        entrydate.set(Calendar.MINUTE, 0);
                                        entrydate.set(Calendar.SECOND, 0);
                                        entrydate.set(Calendar.MILLISECOND, 0);
                                       // entrydate.set(Calendar.HOUR,0);
                                        entries.add(new Entry(entrydate.getTimeInMillis(), (float) sum));
                                        Log.i("point", document.getLong("Deposited Date") + " , " + sum);
                                        sum =document.getDouble("Value");
                                        prevday = d;
                                    }
                                }
                                //add last entry
                                entries.add(new Entry(dnum.getTime(), (float) sum));
                                Log.i("Bank Entries",entries.size()+"");

                                // Setting Data
                                xAxis.setLabelCount(entries.size());
                                Calendar cal1 = Calendar.getInstance();
                                cal1.setTime(new Date((long)entries.get(0).getX()));
                                cal1.add(Calendar.DAY_OF_YEAR, -1);
                                Date previousDate = cal1.getTime();
                                xAxis.setAxisMinimum(previousDate.getTime());
                                cal1.setTime(new Date((long)entries.get(entries.size()-1).getX()));
                                cal1.add(Calendar.DAY_OF_YEAR, +1);
                                Date oneDayLater= cal1.getTime();
                                xAxis.setAxisMaximum(oneDayLater.getTime());
                                LineDataSet dataSet = new LineDataSet(entries, null);
                                chart.setDescription(null);
                                LineData data = new LineData(dataSet);
                                chart.setData(data);
                                chart.invalidate();
                                totaldep = (TextView) findViewById(R.id.totaltxt);
                                DecimalFormat df = new DecimalFormat("#.#####");
                                df.setRoundingMode(RoundingMode.CEILING);
                                totaldep.setText("Total Amount of Deposits: " + df.format(totaldeposits));
                            } else {
                                Log.d("document", "get failed with ", task.getException());
                            }
                        }
                    }
                });

    }
}
