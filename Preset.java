package blockradar;

/** Ready-made performance settings. */
public enum Preset {
	MAX_FPS("Max FPS", 250, 24, false, 1, 6),
	BALANCED("Balanced", 600, 64, true, 2, 3),
	QUALITY("Quality", 2000, 256, true, 4, 1);

	public final String label;
	private final int outlines;
	private final int chunks;
	private final boolean fill;
	private final int scanMillis;
	private final int rescanSeconds;

	Preset(String label, int outlines, int chunks, boolean fill, int scanMillis, int rescanSeconds) {
		this.label = label;
		this.outlines = outlines;
		this.chunks = chunks;
		this.fill = fill;
		this.scanMillis = scanMillis;
		this.rescanSeconds = rescanSeconds;
	}

	public void apply() {
		Settings.maxOutlines = outlines;
		Settings.maxChunks = chunks;
		Settings.fill = fill;
		Settings.scanMillis = scanMillis;
		Settings.rescanSeconds = rescanSeconds;
	}

	/** The preset the settings match right now, or null if they were changed by hand. */
	public static Preset current() {
		for (Preset p : values()) {
			if (Settings.maxOutlines == p.outlines && Settings.maxChunks == p.chunks && Settings.fill == p.fill
				&& Settings.scanMillis == p.scanMillis && Settings.rescanSeconds == p.rescanSeconds) return p;
		}
		return null;
	}
}
