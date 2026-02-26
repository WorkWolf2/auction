package fr.hyping.hypingauctions.util;

public class NumberUtil {

    public static boolean isInteger(String integerStr) {
        try {
            Integer.parseInt(integerStr);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

}
