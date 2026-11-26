package blockradar;

/** How chunks with highlighted blocks are shown. */
public enum ChunkStyle {
	/** Filled in blue, with an outline. */
	SOLID,
	/** Blue outline only. */
	LINES,
	OFF;

	/** Closer than this many blocks, a chunk is not filled so you can see what you dig. */
	private static final int CLEAR_DISTANCE = 8;

	/** Whether this chunk gets the solid fill, seen from a player standing at (x, z). */
	public boolean fills(int chunkX, int chunkZ, double x, double z) {
		if (this != SOLID) return false;
		double west = chunkX * 16.0;
		double north = chunkZ * 16.0;
		double awayX = Math.max(0, Math.max(west - x, x - (west + 16)));
		double awayZ = Math.max(0, Math.max(north - z, z - (north + 16)));
		return Math.max(awayX, awayZ) >= CLEAR_DISTANCE;
	}

	/** The style after this one, for the menu button. */
	public ChunkStyle next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
