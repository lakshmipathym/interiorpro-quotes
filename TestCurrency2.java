import java.text.NumberFormat;
import java.util.Locale;
import java.text.DecimalFormat;

public class TestCurrency2 {
    public static void main(String[] args) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("hi", "IN"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        System.out.println(format.format(132491.50));
        
        System.out.println(String.format(new Locale("en", "IN"), "%,.2f", 132491.50));
        System.out.println(String.format(new Locale("hi", "IN"), "%,.2f", 132491.50));
        
        DecimalFormat df = new DecimalFormat("#,##,##0.00");
        System.out.println(df.format(132491.50));
    }
}
