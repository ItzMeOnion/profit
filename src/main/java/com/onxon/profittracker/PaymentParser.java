package com.onxon.profittracker;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns a chat line into a Transaction, using the patterns from Settings. */
public final class PaymentParser {
    private PaymentParser() {}

    public static Optional<Transaction> parse(String raw) {
        String text = raw.replaceAll("§.", "").trim(); // strip colour codes
        if (text.isEmpty()) return Optional.empty();

        for (String regex : Settings.get().incomingPatterns) {
            Optional<Transaction> t = match(regex, text, true);
            if (t.isPresent()) return t;
        }
        for (String regex : Settings.get().outgoingPatterns) {
            Optional<Transaction> t = match(regex, text, false);
            if (t.isPresent()) return t;
        }
        return Optional.empty();
    }

    private static Optional<Transaction> match(String regex, String text, boolean incoming) {
        try {
            Matcher m = Pattern.compile(regex).matcher(text);
            if (!m.find()) return Optional.empty();

            String player = group(m, "player");
            String number = group(m, "amount");
            if (player == null || number == null) return Optional.empty();

            double amount = toAmount(number, group(m, "suffix"));
            if (amount <= 0) return Optional.empty();

            return Optional.of(new Transaction(System.currentTimeMillis(), incoming, amount, player, text));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // bad regex or unexpected number format; skip the line
        }
    }

    private static String group(Matcher m, String name) {
        try {
            return m.group(name);
        } catch (IllegalArgumentException e) {
            return null; // pattern does not have this group
        }
    }

    private static double toAmount(String number, String suffix) {
        double base = Double.parseDouble(number.replace(",", "").replace("$", ""));
        double mult = 1;
        if (suffix != null) {
            switch (suffix.toLowerCase(Locale.ROOT)) {
                case "k" -> mult = 1e3;
                case "m" -> mult = 1e6;
                case "b" -> mult = 1e9;
                case "t" -> mult = 1e12;
                default -> mult = 1;
            }
        }
        return base * mult;
    }
}
