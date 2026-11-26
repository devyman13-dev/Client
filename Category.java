package blockradar;

/** The kinds of blocks the radar can highlight. Colors are ARGB. */
public enum Category {
	REDSTONE("Redstone", 0xFFFF4D5E, true),
	STORAGE("Storage items", 0xFFFFD54D, true),
	PLACED("Player-placed", 0xFF3DD6F5, true),
	/** Blocks chosen in the block list. */
	PICKED("My blocks", 0xFF7CF29A, true);

	public final String label;
	public final int color;
	public boolean enabled;

	Category(String label, int color, boolean enabledByDefault) {
		this.label = label;
		this.color = color;
		this.enabled = enabledByDefault;
	}
}
