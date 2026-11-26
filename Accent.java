package blockradar;

/** The menu's highlight colors. */
public enum Accent {
	BLUE("Blue", 0xFF4C8DFF),
	PURPLE("Purple", 0xFF9B6CFF),
	RED("Red", 0xFFFF4D6A),
	GREEN("Green", 0xFF2FCF7A),
	PINK("Pink", 0xFFFF6FC1),
	CYAN("Cyan", 0xFF27C4DC);

	public final String label;
	public final int color;

	Accent(String label, int color) {
		this.label = label;
		this.color = color;
	}

	public Accent next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
