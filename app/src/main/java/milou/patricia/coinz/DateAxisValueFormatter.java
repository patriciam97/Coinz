package milou.patricia.coinz;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateAxisValueFormatter implements IAxisValueFormatter {
    private Date[] mValues;

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");

    public DateAxisValueFormatter(Date[] values) {
        this.mValues = values; }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return sdf.format(new Date((long) value));
    }
}