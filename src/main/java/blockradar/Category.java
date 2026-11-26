package blockradar;

/** The kinds of blocks the radar can highlight. Colors are ARGB. */
public enum Category {
	REDSTONE("Redstone", 0xFFFF2020, true),
	REDSTONE_ORE("Redstone ore", 0xFFFF80C0, false),
	WRONG_DIMENSION("Wrong dimension", 0xFFC040FF, true),
	WRONG_BIOME("Wrong biome", 0xFFFFA000, true),
	CRAFTED("Crafted blocks", 0xFF20E0FF, false);

	public final String label;
	public final int color;
	public boolean enabled;

	Category(String label, int color, boolean enabledByDefault) {
		this.label = label;
		this.color = color;
		this.enabled = enabledByDefault;
	}
}
