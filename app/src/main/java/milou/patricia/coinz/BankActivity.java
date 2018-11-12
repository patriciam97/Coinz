package milou.patricia.coinz;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;


import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

public class BankActivity extends AppCompatActivity {
    //Firebse objects
    protected FirebaseAuth mAuth;
    protected FirebaseUser user;
    protected FirebaseFirestore db = FirebaseFirestore.getInstance();
    //Objects used to draw the line chart
    private LineChart chart;
    private List<Entry> entries= new ArrayList<>();
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
        chart = findViewById(R.id.chart);
        chart.setScaleEnabled(false);
        chart.getLegend().setEnabled(false);
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        chart.getAxisRight().setEnabled(false);
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setTextSize(8);
        xAxis.setLabelRotationAngle(-30);
        xAxis.setAvoidFirstLastClipping(false);
        IAxisValueFormatter formatter = (float value, AxisBase axis) -> {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
            return sdf.format(new Date((long) value));
        };
        xAxis.setValueFormatter(formatter);
        //plot the graph
        plotGraph();

    }

    /**
     * This function goes through all coins saved in the user's bank account and plot the graph
     */
    private void plotGraph() {
        db.collection("Users").document(Objects.requireNonNull(user.getEmail())).collection("Bank")
                .orderBy("Deposited Date")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        //if there are some coins stored
                        if (Objects.requireNonNull(task.getResult()).size() > 0) {
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                            Date prevday = task.getResult().getDocuments().get(0).getDate("Deposited Date");
                            double sum = 0;
                            Date d=new Date();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                d =document.getDate("Deposited Date");
                                if (sdf.format(d).equals(sdf.format(prevday))) {
                                    sum += document.getDouble("Value");
                                    totaldeposits = totaldeposits + document.getDouble("Value");
                                } else {
                                    //set entry date to that date on midnight so that plotting si more accurate
                                    Calendar entrydate = new GregorianCalendar();
                                    entrydate.setTime(prevday);
                                    entrydate.set(Calendar.HOUR_OF_DAY, 0);
                                    entrydate.set(Calendar.MINUTE, 0);
                                    entrydate.set(Calendar.SECOND, 0);
                                    entrydate.set(Calendar.MILLISECOND, 0);
                                   // entrydate.set(Calendar.HOUR,0);
                                    entries.add(new Entry(entrydate.getTimeInMillis(), (float) sum));
                                    Timber.i(document.getDate("Deposited Date") + " , " + sum);
                                    sum =document.getDouble("Value");
                                    prevday = d;
                                }
                            }
                            //add last entry
                            Calendar entrydate = new GregorianCalendar();
                            entrydate.setTime(d);
                            entrydate.set(Calendar.HOUR_OF_DAY, 0);
                            entrydate.set(Calendar.MINUTE, 0);
                            entrydate.set(Calendar.SECOND, 0);
                            entrydate.set(Calendar.MILLISECOND, 0);
                            // entrydate.set(Calendar.HOUR,0);
                            entries.add(new Entry(entrydate.getTimeInMillis(), (float) sum));
                            Timber.i("Entries size: %s", entries.size());

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
                            totaldep = findViewById(R.id.totaltxt);
                            DecimalFormat df = new DecimalFormat("#.#####");
                            df.setRoundingMode(RoundingMode.CEILING);
                            totaldep.setText(String.format("Total Gold Coins: %s", df.format(totaldeposits)));
                        } else {
                            Timber.d(task.getException(), "get failed with ");
                        }
                    }
                });

    }
}
