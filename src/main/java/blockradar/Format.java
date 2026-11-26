package blockradar;

import java.util.Locale;

/** Turns numbers into short text. */
public final class Format {
	private Format() {}

	public static String count(long number) {
		return String.format(Locale.US, "%,d", number);
	}

	public static String percent(long part, long whole) {
		if (part <= 0 || whole <= 0) return "0%";
		double value = 100.0 * part / whole;
		if (value < 0.01) return "<0.01%";
		return String.format(Locale.US, value < 10 ? "%.2f%%" : "%.1f%%", value);
	}
}
