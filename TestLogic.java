public class TestLogic {
    public static String format(double amount, boolean strict) {
        boolean isNegative = amount < 0;
        double absAmount = Math.abs(amount);
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("0.00");
        formatter.setDecimalFormatSymbols(new java.text.DecimalFormatSymbols(java.util.Locale.US));
        String res = formatter.format(absAmount);
        
        String[] split = res.split("\\.");
        String intPart = split[0];
        String decimalPart = split[1];
        
        String formattedInt = "";
        if (intPart.length() > 3) {
            formattedInt = "," + intPart.substring(intPart.length() - 3);
            intPart = intPart.substring(0, intPart.length() - 3);
            while (intPart.length() > 2) {
                formattedInt = "," + intPart.substring(intPart.length() - 2) + formattedInt;
                intPart = intPart.substring(0, intPart.length() - 2);
            }
            formattedInt = intPart + formattedInt;
        } else {
            formattedInt = intPart;
        }
        
        String finalSign = isNegative ? "-" : "";
        if (!strict && decimalPart.equals("00")) {
            return finalSign + formattedInt;
        } else {
            return finalSign + formattedInt + "." + decimalPart;
        }
    }
    
    public static void main(String[] args) {
        System.out.println(format(132491.50, false));
        System.out.println(format(132491.0, false));
        System.out.println(format(1000.0, false));
        System.out.println(format(10000.0, false));
        System.out.println(format(100000.0, false));
        System.out.println(format(1000000.0, false));
        
        System.out.println(format(132491.50, true));
        System.out.println(format(132491.0, true));
        System.out.println(format(1000.0, true));
        System.out.println(format(10000.0, true));
        System.out.println(format(100000.0, true));
        System.out.println(format(1000000.0, true));
    }
}
