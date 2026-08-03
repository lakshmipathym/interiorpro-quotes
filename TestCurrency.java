import java.text.NumberFormat;
import java.util.Locale;

public class TestCurrency {
    public static void main(String[] args) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        System.out.println(format.format(132491.50));
        System.out.println(format.format(132491.0));
        
        NumberFormat format2 = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        format2.setMinimumFractionDigits(2);
        format2.setMaximumFractionDigits(2);
        System.out.println(format2.format(132491.50));
        System.out.println(format2.format(132491.0));
    }
}
