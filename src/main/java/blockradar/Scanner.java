package blockradar;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	public record Hit(BlockPos pos, Block block, Category category) {}

	private static final long TIME_BUDGET_NANOS = 4_000_000L; // 4 ms per tick
	private static final int MAX_DRAWN = 2000;

	private final Map<Long, List<Hit>> chunks = new HashMap<>();
	private final ArrayDeque<int[]> queue = new ArrayDeque<>();
	/** Rules result per block for the current dimension, so each block is only checked once. */
	private final Map<Block, Optional<Category>> known = new IdentityHashMap<>();
	private ClientLevel level;
	private List<Hit> drawn = List.of();
	private int ticks;

	/** If set, only this block type is highlighted. */
	private Block focus;

	public List<Hit> drawn() { return drawn; }
	public Block focus() { return focus; }

	public void setFocus(Block block) {
		focus = block;
		refreshDrawn(Minecraft.getInstance().player);
	}

	public void clear() {
		chunks.clear();
		queue.clear();
		drawn = List.of();
	}

	public void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (client.level == null || player == null) {
			level = null;
			focus = null;
			clear();
			return;
		}
		if (client.level != level) { // joined a world or changed dimension
			level = client.level;
			focus = null;
			known.clear();
			clear();
		}

		if (queue.isEmpty()) startSweep(client, player);

		long deadline = System.nanoTime() + TIME_BUDGET_NANOS;
		while (!queue.isEmpty() && System.nanoTime() < deadline) {
			int[] next = queue.poll();
			scanChunk(next[0], next[1]);
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

	/** What the rules say about a block before looking at the biome. */
	private Category baseCategory(Block block, String dimension) {
		return known.computeIfAbsent(block,
			b -> Optional.ofNullable(Rules.classify(blockName(b), dimension, ""))).orElse(null);
	}

	private void scanChunk(int chunkX, int chunkZ) {
		long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
		ChunkAccess access = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
		if (!(access instanceof LevelChunk chunk)) {
			chunks.remove(key);
			return;
		}

		String dimension = path(level.dimension().identifier().toString());
		List<Hit> hits = new ArrayList<>();
		LevelChunkSection[] sections = chunk.getSections();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection section = sections[i];
			if (section.hasOnlyAir() || !section.maybeHas(state -> {
				Category c = baseCategory(state.getBlock(), dimension);
				return c != null && c.enabled;
			})) continue;
			int baseY = chunk.getSectionYFromSectionIndex(i) << 4;

			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						BlockState state = section.getBlockState(x, y, z);
						Category category = baseCategory(state.getBlock(), dimension);
						if (category == null || !category.enabled) continue;
						pos.set((chunkX << 4) + x, baseY + y, (chunkZ << 4) + z);
						// The biome is only looked up for blocks that depend on it.
						if (category == Category.WRONG_BIOME) {
							String biome = path(level.getBiome(pos).getRegisteredName());
							category = Rules.classify(blockName(state.getBlock()), dimension, biome);
						}
						if (category != null && category.enabled) {
							hits.add(new Hit(pos.immutable(), state.getBlock(), category));
						}
					}
				}
			}
		}

		if (hits.isEmpty()) chunks.remove(key);
		else chunks.put(key, hits);
	}

	private boolean visible(Hit hit) {
		return hit.category().enabled && (focus == null || hit.block() == focus);
	}

	/** Picks the nearest hits to draw. Counting always uses every hit. */
	private void refreshDrawn(LocalPlayer player) {
		if (player == null) return;
		BlockPos here = player.blockPosition();
		List<Hit> all = new ArrayList<>();
		for (List<Hit> list : chunks.values()) {
			for (Hit hit : list) if (visible(hit)) all.add(hit);
		}
		if (all.size() > MAX_DRAWN) {
			all.sort(Comparator.comparingDouble(h -> h.pos().distSqr(here)));
			all = new ArrayList<>(all.subList(0, MAX_DRAWN));
		}
		drawn = all;
	}

	/** How many of each block type were found in all scanned chunks, biggest first. */
	public List<Map.Entry<Block, Integer>> totals() {
		Map<Block, Integer> counts = new HashMap<>();
		for (List<Hit> list : chunks.values()) {
			for (Hit hit : list) {
				if (hit.category().enabled) counts.merge(hit.block(), 1, Integer::sum);
			}
		}
		List<Map.Entry<Block, Integer>> result = new ArrayList<>(counts.entrySet());
		result.sort((a, b) -> b.getValue() - a.getValue());
		return result;
	}

	public int total(Block block) {
		int count = 0;
		for (List<Hit> list : chunks.values()) {
			for (Hit hit : list) if (hit.block() == block && hit.category().enabled) count++;
		}
		return count;
	}

	/** Size of the touching group (including diagonals) of the same block as this hit. */
	public int clusterSize(Hit start) {
		Set<BlockPos> remaining = new HashSet<>();
		for (List<Hit> list : chunks.values()) {
			for (Hit hit : list) if (hit.block() == start.block()) remaining.add(hit.pos());
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
		return size;
	}
}
