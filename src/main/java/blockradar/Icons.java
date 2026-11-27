package blockradar;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** The picture shown for a block in lists. */
public final class Icons {
	private Icons() {}

	private static final Map<Block, ItemStack> KNOWN = new HashMap<>();
	private static final Map<String, Item> ITEMS = new HashMap<>();

	/** Draws a 16 by 16 picture of the block, or a square in its map color if it has none. */
	public static void draw(GuiGraphics g, Block block, int x, int y) {
		ItemStack item = of(block);
		if (!item.isEmpty()) {
			g.renderItem(item, x, y);
			return;
		}
		int mapColor = block.defaultMapColor().col;
		g.fill(x + 3, y + 3, x + 13, y + 13, mapColor != 0 ? 0xFF000000 | mapColor : Theme.MUTED);
	}

	/** The item to draw for this block. Empty if there is none. */
	public static ItemStack of(Block block) {
		return KNOWN.computeIfAbsent(block, Icons::find);
	}

	private static ItemStack find(Block block) {
		Item item = block.asItem();
		if (item == Items.AIR) {
			if (ITEMS.isEmpty()) {
				for (Item each : BuiltInRegistries.ITEM) {
					ITEMS.put(BuiltInRegistries.ITEM.getKey(each).toString(), each);
				}
			}
			String id = BuiltInRegistries.BLOCK.getKey(block).toString();
			String standIn = StandIns.of(id.substring(id.indexOf(':') + 1));
			item = ITEMS.getOrDefault("minecraft:" + standIn, Items.AIR);
		}
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}
}
