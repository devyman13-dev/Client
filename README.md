# Block Radar (Fabric, Minecraft 1.21.11)

Highlights redstone blocks and out-of-place blocks through walls, in the chunks your
client has loaded.

## Get the .jar

### Option A: let GitHub build it (nothing to install)
1. Make a free GitHub account and create a new repository.
2. Click "uploading an existing file" and drag in everything inside this folder.
3. Check that `.github/workflows/build.yml` is in the repository. Hidden folders are often
   skipped: if it is missing, use Add file > Create new file, type
   `.github/workflows/build.yml` as the name, and paste in that file's text.
4. Open the Actions tab, wait for "Build mod jar", open the run and download
   `Basefinder-jar`. Unzip it to get `blockradar-1.20.0.jar`.

### Option B: build on your computer
1. Install JDK 25 (for example from adoptium.net).
2. Open a terminal in this folder.
3. Windows: `gradlew.bat build`   Mac/Linux: `sh gradlew build`
4. The jar is `build/libs/blockradar-1.20.0.jar` (not the `-sources` one, if present).

## Install
Install Fabric Loader for 1.21.11, then put `blockradar-1.20.0.jar` and the Fabric API jar
in your `.minecraft/mods` folder.

## Controls (changeable in Options > Controls)
| Action | Default |
|---|---|
| Open menu | J |
| Radar on/off | K |
| See a highlighted block's counts | Left click it from a distance, or sneak + left click up close |
| List a chunk's suspicious blocks | Left click a blue chunk when no block is in reach |
| List your own chunk's suspicious blocks | N |
| Freecam on/off | H |
| Entity ESP on/off | G |

A normal left click on a block in reach always breaks it. A "Left click to see" message
shows above the hotbar when a click would open something. Counts appear in the top right.
The chunk list shows only the highlighted blocks in that chunk (including your picks).
Click a row to show only that block.

## Entity ESP
Press G (or click **Entity ESP** in the Render panel) to draw blue lines around players and mobs.
It works even when the radar is off. Right click **Entity ESP** for its settings: players,
hostile mobs and passive mobs on or off, range (8 to 256 blocks), line width, through walls, and
"By type" (players blue, hostile red, other mobs green). Only entities your client has loaded
can be shown, and in freecam the range is measured from the camera.

## Freecam
Press H (or click **Freecam** in the Render panel) to fly the camera through walls and
underground while your character stays where it is. WASD moves along where you look, Space
goes up, Shift goes down, and holding sprint multiplies the speed. The radar keeps working and
scans around the camera, so you can fly down to highlighted blocks and look at them. Left click
a highlighted block or blue chunk from the camera to open its counts. Digging, hitting and using
items are blocked while it is on. Right click **Freecam** in the menu for its settings: speed,
sprint boost, smooth movement, leave on damage, and keeping your view. Your character is still
in the world, so it can be hurt, and turning the mouse also turns your character.

## Menu
Press J. Left click a row to use it, right click (or click its +) for settings, drag a title
to move a panel, right click a title to fold it, Esc to close. Positions are remembered.
- **Finder**: Redstone, Storage, Placed, My blocks, Pick blocks, Hide blocks. Blocks are not
  filtered by dimension or biome.
- **Render**: Radar on/off, Block ESP (fill, max boxes), Chunks (SOLID, LINES, OFF; max
  chunks), HUD, Click hints.
- **Settings**: performance preset, scan time, rescan pause, menu color, reset layout.
- **Results**: suspicious count and percent, then every block found. Click one to show only
  it. Scroll for more.
- The HUD shows FPS and counts in the top left.
- **Pick blocks...**: a searchable Xray-style grid with an icon for every non-air block in the
  registry, including ores and modded blocks. Blocks with no item (such as water) show a
  stand-in item or a color square. Hover an icon for its name and click to toggle that block.
  The clicked-chunk details also show each block's icon and count. Picks are saved under
  **My blocks**. To see only your picks, switch the other categories off. For very common
  blocks, every one is counted but only up to 256 per chunk get positions (group sizes then
  show a "+").
- **Base / loot chance**: the chunk list starts with a percentage, a rating and the biggest
  reasons. It uses every block the radar's own rules flag in that chunk, including categories
  you switched off, but not blocks you only picked. Things like shulker boxes, beacons,
  enchanting tables, hoppers and large amounts of storage count a lot. Chests, pistons and
  other blocks that structures also contain count less. It is a rough score, not a real
  probability, and it never goes above 95%. The numbers are in `BaseChance.java`.
- **Hide blocks...**: the same kind of grid for blocks you do not want to see. Click a block to
  hide it (red), click again to show it, or use **Show all**. Hidden blocks get no boxes, do not
  mark chunks blue, and are left out of counts, chunk lists and the base chance. Blocks the
  radar flags are listed first. Hiding a torch, sign or banner also hides its wall version, and
  hiding either piston also hides piston heads. A block cannot be both picked and hidden.
- Each block has its own highlight color, set in `Colors.java`.
- Every scanned chunk with a highlighted block is marked in blue, from the bottom of the
  world to 64 blocks above sea level (Y=128). To change the height, press J, right click
  **Chunks** and drag **Height**: 0 blocks stops at sea level (Y=64), the middle is 64 blocks
  above it and the end is 128 blocks above it (Y=192). SOLID fills the walls (other blocks
  hide the fill, and chunks within 8 blocks of you are not filled) and adds an outline that
  shows through walls. LINES is the outline only.
- **Storage items**: chests, trapped chests, ender chests, barrels and shulker boxes.
- Copper blocks and lightning rods are only highlighted if you pick them.
- Redstone ore is only highlighted if you pick it. Sculk is not highlighted, apart from
  calibrated sculk sensors, which count as redstone.
- **Player-placed** covers concrete, beacons and other blocks that never generate, plus
  deepslate tiles and variants above Y=20.

## Limits
- Only chunks the game has loaded can be scanned. On servers this may be less than your
  render distance.
- Only the nearest blocks and chunks are drawn (600 and 64 by default); counts include all.
- For more FPS pick the Max FPS preset (Settings > Preset). Boxes, chunks and entity lines that
  are clearly behind you are not drawn, and chunks already scanned that are more than 4 chunks
  away are only scanned again every 4th scan. Changes in the world show after the next scan
  (up to about four scans for far chunks).
- Blocks not listed in `Rules.java` are treated as normal. Natural structures may contain
  blocks that the finder flags.
- Menu settings are saved in `.minecraft/config/blockradar.properties`.
- Many servers ban this kind of mod (DonutSMP bans radar and ESP). Use it in single player
  or where allowed.
