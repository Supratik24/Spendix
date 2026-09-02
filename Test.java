import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Test {
    public static void main(String[] args) {
        String regex = "(?i)(?:Rs\\.?|INR\\.?)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)";
        Pattern p = Pattern.compile(regex);
        
        String s2 = "Rs.      6146.000 credited";
        Matcher m2 = p.matcher(s2);
        if(m2.find()) System.out.println("S2: " + m2.group(1));
        else System.out.println("S2: NOT FOUND");
    }
}