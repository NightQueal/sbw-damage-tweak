package civilized.sbw;
import java.util.Locale;
public final class StatText {
 public static String number(double value){return String.format(Locale.ROOT,"%.2f",value).replaceAll("0+$","").replaceAll("\\.$","");}
 public static boolean unlimited(double value){return !Double.isFinite(value)||value>=Float.MAX_VALUE/2.0;}
 public static String range(double low,double high){return unlimited(high)?number(low)+"m+":number(low)+"-"+number(high)+"m";}
 public static String change(double multiplier){double percent=(multiplier-1)*100;return (percent>0?"+":"")+number(percent)+"%";}
}
