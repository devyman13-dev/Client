package blockradar;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Walks the loaded chunks a few at a time so the game does not freeze. */
public final class Scanner {
	/** One kind of highlighted block. "rule" is null if only the player's choice flags it. */
	public record Key(Block block, Category rule, boolean picked) {
		/** The category it shows under right now, or null if it is switched off. */
		public Category shown() { return Picks.shown(rule, picked); }
	}
	public record Hit(BlockPos pos, Key key, int color) {
		public Block block() { return key.block(); }
		public Category category() {
			Category shown = key.shown();
			return shown != null ? shown : Category.PICKED;
		}
	}
	/** A chunk position (block position divided by 16). */
	public record Chunk(int x, int z) {}
	/** One block type in a chunk. "highlighted" is how many of them the radar marks. */
	public record Row(Block block, int count, int highlighted, int color) {}
	/** What is in one chunk, highlighted block types first. */
	public record ChunkReport(Chunk chunk, List<Row> rows, int highlighted) {}
	/** Size of a touching group. "partial" means there may be more than "size". */
	public record Group(int size, boolean partial) {}

	private static final long TIME_BUDGET_NANOS = 4_000_000L; // 4 ms per tick
	private static final int MAX_DRAWN = 2000;
	/** Most positions remembered per chunk for one kind of block. Counts are never limited. */
	private static final int MAX_KEPT_FLAGGED = 4096;
	private static final int MAX_KEPT_PICKED = 256;

	/** How many of one kind a chunk holds, and where (some of) them are. */
	private static final class Tally {
		int count;
		final List<Hit> hits = new ArrayList<>();
	}
	private record ChunkData(Map<Key, Tally> tallies, int blocks) {}
	private record Rule(Category category, boolean dependsOnPlace, int color, boolean picked, int mapColor) {
		/** False if this block can never be highlighted in this dimension. */
		boolean possible() { return picked || category != null || dependsOnPlace; }
	}

	/**
	 * Totals for the menu. "blocks" is every non-air block scanned, "suspicious" is what the
	 * radar's own rules flagged, and "highlighted" also includes the player's chosen blocks.
	 */
	public record Stats(long blocks, long suspicious, long highlighted,
		List<Map.Entry<Block, Integer>> byBlock, Map<Category, Integer> byCategory,
		Map<Block, Integer> colors) {}

	private final Map<Long, ChunkData> chunks = new HashMap<>();
	private final ArrayDeque<int[]> queue = new ArrayDeque<>();
	/** Rules result per block for the current dimension, so each block is only checked once. */
	private final Map<Block, Rule> known = new IdentityHashMap<>();
	private ClientLevel level;
	private List<Hit> drawn = List.of();
	/** Chunks holding at least one highlighted block. */
	private List<Chunk> marked = List.of();
	private int ticks;

	/** If set, only this block type is highlighted. */
	private Block focus;

	public List<Hit> drawn() { return drawn; }
	public List<Chunk> marked() { return marked; }
	public Block focus() { return focus; }

	/** Brings back the block that was selected last time the game ran. */
	public void restoreFocus(String id) {
		for (Block block : BuiltInRegistries.BLOCK) {
			if (BuiltInRegistries.BLOCK.getKey(block).toString().equals(id)) focus = block;
		}
	}

	public void setFocus(Block block) {
		focus = block;
		Settings.onlyBlock = block == null ? "" : BuiltInRegistries.BLOCK.getKey(block).toString();
		BlockRadarClient.saveSettings();
		refreshDrawn(Minecraft.getInstance().player);
	}

	public void clear() {
		chunks.clear();
		queue.clear();
		drawn = List.of();
		marked = List.of();
	}

	/** Starts over, for when the chosen blocks change. */
	public void rescan() {
		known.clear();
		clear();
	}

	public void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (client.level == null || player == null) {
			level = null;
			clear();
			return;
		}
		if (client.level != level) { // joined a world or changed dimension
			level = client.level;
			known.clear();
			clear();
		}

		if (queue.isEmpty()) startSweep(client, player);

		long deadline = System.nanoTime() + TIME_BUDGET_NANOS;
		while (!queue.isEmpty() && System.nanoTime() < deadline) {
			int[] next = queue.poll();
			scanChunk(next[0], next[1], player.getBlockY());
		}

		if (++ticks % 10 == 0) refreshDrawn(player);
	}

	private void startSweep(Minecraft client, LocalPlayer player) {
		int radius = client.options.getEffectiveRenderDistance();
		int px = player.getBlockX() >> 4;
		int pz = player.getBlockZ() >> 4;

		// Forget chunks that are now out of range.
		for (Iterator<Long> it = chunks.keySet().iterator(); it.hasNext();) {
			long key = it.next();
			int x = (int) (key >> 32);
			int z = (int) key;
			if (Math.abs(x - px) > radius || Math.abs(z - pz) > radius) it.remove();
		}

		List<int[]> order = new ArrayList<>();
		for (int x = px - radius; x <= px + radius; x++) {
			for (int z = pz - radius; z <= pz + radius; z++) order.add(new int[] {x, z});
		}
		// Nearest chunks first.
		order.sort(Comparator.comparingInt(c -> (c[0] - px) * (c[0] - px) + (c[1] - pz) * (c[1] - pz)));
		queue.addAll(order);
	}

	private static String path(String id) {
		return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
	}

	private static String blockName(Block block) {
		return path(BuiltInRegistries.BLOCK.getKey(block).toString());
	}

	/** What the rules say about a block before looking at where it is. */
	private Rule rule(Block block, String dimension) {
		return known.computeIfAbsent(block, b -> {
			String id = BuiltInRegistries.BLOCK.getKey(b).toString();
			String name = path(id);
			boolean picked = Settings.picked.contains(id) && Picks.allowed(name);
			return new Rule(Rules.classify(name, dimension, "", Integer.MAX_VALUE), Rules.dependsOnPlace(name),
				Colors.of(name), picked, b.defaultMapColor().col);
		});
	}

	/** The block's own color, else its category's, else the color it has on maps. */
	private static int color(Rule rule, Category category) {
		if (rule.color() != 0) return rule.color();
		if (category != null) return category.color;
		return rule.mapColor() != 0 ? 0xFF000000 | rule.mapColor() : Category.PICKED.color;
	}

	private void scanChunk(int chunkX, int chunkZ, int playerY) {
		long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
		ChunkAccess access = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
		if (!(access instanceof LevelChunk chunk)) {
			chunks.remove(key);
			return;
		}

		String dimension = path(level.dimension().identifier().toString());
		Map<Key, Tally> tallies = new HashMap<>();
		int[] blocks = {0};
		LevelChunkSection[] sections = chunk.getSections();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		// Sections near the player's height come first, so limited positions are the useful ones.
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < sections.length; i++) order.add(i);
		order.sort(Comparator.comparingInt(
			i -> Math.abs((chunk.getSectionYFromSectionIndex(i) << 4) + 8 - playerY)));

		// The last kind seen, so long runs of one block stay fast.
		Key lastKey = null;
		Tally lastTally = null;

		for (int i : order) {
			LevelChunkSection section = sections[i];
			if (section.hasOnlyAir()) continue;
			section.getStates().count((state, count) -> {
				if (!state.isAir()) blocks[0] += count;
			});
			if (!section.maybeHas(state -> rule(state.getBlock(), dimension).possible())) continue;
			int baseY = chunk.getSectionYFromSectionIndex(i) << 4;

			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						BlockState state = section.getBlockState(x, y, z);
						Block block = state.getBlock();
						Rule rule = rule(block, dimension);
						if (!rule.possible()) continue;
						Category category = rule.category();
						pos.set((chunkX << 4) + x, baseY + y, (chunkZ << 4) + z);
						// The biome is only looked up for blocks that depend on it.
						if (rule.dependsOnPlace()) {
							String biome = path(level.getBiome(pos).getRegisteredName());
							category = Rules.classify(blockName(block), dimension, biome, pos.getY());
						}
						if (category == null && !rule.picked()) continue;

						if (lastKey == null || lastKey.block() != block || lastKey.rule() != category) {
							lastKey = new Key(block, category, rule.picked());
							lastTally = tallies.computeIfAbsent(lastKey, k -> new Tally());
						}
						lastTally.count++;
						int limit = category != null ? MAX_KEPT_FLAGGED : MAX_KEPT_PICKED;
						if (lastTally.hits.size() < limit) {
							lastTally.hits.add(new Hit(pos.immutable(), lastKey, color(rule, category)));
						}
					}
				}
			}
		}

		chunks.put(key, new ChunkData(tallies, blocks[0]));
	}

	private boolean visible(Key key) {
		return key.shown() != null && (focus == null || key.block() == focus);
	}

	/** Picks the nearest hits to draw. Counting always uses every block. */
	private void refreshDrawn(LocalPlayer player) {
		if (player == null) return;
		BlockPos here = player.blockPosition();
		int px = here.getX() >> 4;
		int pz = here.getZ() >> 4;
		List<Long> nearest = new ArrayList<>(chunks.keySet());
		nearest.sort(Comparator.comparingLong(k -> {
			long dx = (int) (k >> 32) - px;
			long dz = (int) k.longValue() - pz;
			return dx * dx + dz * dz;
		}));

		List<Hit> all = new ArrayList<>();
		List<Chunk> found = new ArrayList<>();
		for (long key : nearest) {
			// Far chunks cannot make the cut once there are plenty of closer hits.
			boolean room = all.size() < MAX_DRAWN * 4;
			boolean any = false;
			for (Map.Entry<Key, Tally> entry : chunks.get(key).tallies().entrySet()) {
				if (!visible(entry.getKey())) continue;
				any = true;
				if (room) all.addAll(entry.getValue().hits);
			}
			if (any) found.add(new Chunk((int) (key >> 32), (int) key));
		}
		marked = found;
		if (all.size() > MAX_DRAWN) {
			all.sort(Comparator.comparingDouble(h -> h.pos().distSqr(here)));
			all = new ArrayList<>(all.subList(0, MAX_DRAWN));
		}
		drawn = all;
	}

	/** Counts everything scanned so far. Block types are listed biggest first. */
	public Stats stats() {
		long blocks = 0;
		long suspicious = 0;
		long highlighted = 0;
		Map<Block, Integer> counts = new HashMap<>();
		Map<Category, Integer> categories = new EnumMap<>(Category.class);
		Map<Block, Integer> colors = new HashMap<>();
		for (ChunkData data : chunks.values()) {
			blocks += data.blocks();
			for (Map.Entry<Key, Tally> entry : data.tallies().entrySet()) {
				Key key = entry.getKey();
				int count = entry.getValue().count;
				if (key.rule() != null) categories.merge(key.rule(), count, Integer::sum);
				if (key.picked()) categories.merge(Category.PICKED, count, Integer::sum);
				Category shown = key.shown();
				if (shown == null) continue;
				highlighted += count;
				if (shown != Category.PICKED) suspicious += count;
				counts.merge(key.block(), count, Integer::sum);
				if (!entry.getValue().hits.isEmpty()) {
					colors.putIfAbsent(key.block(), entry.getValue().hits.get(0).color());
				}
			}
		}
		List<Map.Entry<Block, Integer>> byBlock = new ArrayList<>(counts.entrySet());
		byBlock.sort((a, b) -> b.getValue() - a.getValue());
		return new Stats(blocks, suspicious, highlighted, byBlock, categories, colors);
	}

	/** Counts every block type in a loaded chunk, leaving out ores and copper. */
	public ChunkReport report(Chunk where) {
		Map<Block, Integer> counts = new HashMap<>();
		if (level != null && level.getChunkSource().getChunk(where.x(), where.z(), ChunkStatus.FULL, false)
				instanceof LevelChunk chunk) {
			for (LevelChunkSection section : chunk.getSections()) {
				if (section.hasOnlyAir()) continue;
				section.getStates().count((state, count) -> {
					if (!state.isAir() && Picks.allowed(blockName(state.getBlock()))) {
						counts.merge(state.getBlock(), count, Integer::sum);
					}
				});
			}
		}

		Map<Block, Integer> marks = new HashMap<>();
		Map<Block, Integer> colors = new HashMap<>();
		int highlighted = 0;
		ChunkData data = chunks.get(((long) where.x() << 32) | (where.z() & 0xFFFFFFFFL));
		if (data != null) {
			for (Map.Entry<Key, Tally> entry : data.tallies().entrySet()) {
				if (entry.getKey().shown() == null) continue;
				Block block = entry.getKey().block();
				Tally tally = entry.getValue();
				highlighted += tally.count;
				marks.merge(block, tally.count, Integer::sum);
				if (!tally.hits.isEmpty()) colors.putIfAbsent(block, tally.hits.get(0).color());
			}
		}

		List<Row> rows = new ArrayList<>();
		counts.forEach((block, count) -> rows.add(new Row(block, count,
			marks.getOrDefault(block, 0), colors.getOrDefault(block, Theme.ACCENT))));
		// Highlighted types first, then the most common.
		rows.sort(Comparator.comparing((Row row) -> row.highlighted() == 0)
			.thenComparing(row -> -row.count()));
		return new ChunkReport(where, rows, highlighted);
	}

	/** Size of the touching group (including diagonals) of the same block as this hit. */
	public Group clusterSize(Hit start) {
		Set<BlockPos> remaining = new HashSet<>();
		boolean partial = false;
		for (ChunkData data : chunks.values()) {
			for (Tally tally : data.tallies().values()) {
				if (tally.hits.isEmpty() || tally.hits.get(0).block() != start.block()) continue;
				if (tally.count > tally.hits.size()) partial = true;
				for (Hit hit : tally.hits) remaining.add(hit.pos());
			}
		}
		ArrayDeque<BlockPos> open = new ArrayDeque<>();
		remaining.remove(start.pos());
		open.add(start.pos());
		int size = 0;
		while (!open.isEmpty()) {
			BlockPos p = open.poll();
			size++;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos n = p.offset(dx, dy, dz);
						if (remaining.remove(n)) open.add(n);
					}
				}
			}
		}
		return new Group(size, partial);
	}
}
