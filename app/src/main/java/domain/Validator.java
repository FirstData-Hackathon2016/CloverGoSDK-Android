package domain;

import android.util.Patterns;

import java.util.Calendar;
import java.util.regex.Pattern;

public class Validator {

    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public static boolean validateCardNumber(String cardNumber) {
        boolean isOdd = true;
        int sum = 0;

        for (int index = cardNumber.length() - 1; index >= 0; index--) {
            char c = cardNumber.charAt(index);
            if (!Character.isDigit(c)) {
                return false;
            }
            int digitInteger = Integer.parseInt("" + c);
            isOdd = !isOdd;

            if (isOdd) {
                digitInteger *= 2;
            }

            if (digitInteger > 9) {
                digitInteger -= 9;
            }

            sum += digitInteger;
        }

        return sum % 10 == 0 && isWholePositiveNumber(cardNumber);

    }

    private static boolean isWholePositiveNumber(String value) {
        if (value == null) {
            return false;
        }
        for (char c : value.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public static boolean validateEmailInput(String email) {
        boolean isValidEmail = Pattern.compile(EMAIL_PATTERN).matcher(email).matches();
        return isValidEmail;
    }

    public static boolean validatePhoneNumberInput(String phoneNumber) {
        // Strip out non-numerics
        String numberString = phoneNumber.replaceAll("[^\\d]", "");
        boolean isValidPhoneNumber = numberString.length() >= 10 && Patterns.PHONE.matcher(numberString).matches();
        return isValidPhoneNumber;
    }

    public static boolean validateCardExpiry(String cardExpiry) {
        if (cardExpiry.length() != 4)
            return false;

        Calendar currentDate = Calendar.getInstance();
        int month = currentDate.get(Calendar.MONTH) + 1;
        int year = currentDate.get(Calendar.YEAR) % 100;

        int cardMonth = Integer.parseInt(cardExpiry.substring(0, 2));
        int cardYear = Integer.parseInt(cardExpiry.substring(2, 4));

        if (cardYear > year && cardMonth < 13)
            return true;
        else if ((cardMonth < 13 && cardMonth >= month) && cardYear >= year)
            return true;
        else
            return false;
    }
}