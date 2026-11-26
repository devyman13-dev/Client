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
   `Basefinder-jar`. Unzip it to get `blockradar-1.12.0.jar`.

### Option B: build on your computer
1. Install JDK 25 (for example from adoptium.net).
2. Open a terminal in this folder.
3. Windows: `gradlew.bat build`   Mac/Linux: `sh gradlew build`
4. The jar is `build/libs/blockradar-1.12.0.jar` (not the `-sources` one, if present).

## Install
Install Fabric Loader for 1.21.11, then put `blockradar-1.12.0.jar` and the Fabric API jar
in your `.minecraft/mods` folder.

## Controls (changeable in Options > Controls)
| Action | Default |
|---|---|
| Open menu | J |
| Radar on/off | K |
| See a highlighted block's counts | Left click it from a distance, or sneak + left click up close |
| List a chunk's suspicious blocks | Left click a blue chunk when no block is in reach |
| List your own chunk's suspicious blocks | N |

A normal left click on a block in reach always breaks it. A "Left click to see" message
shows above the hotbar when a click would open something. Counts appear in the top right.
The chunk list shows only the highlighted blocks in that chunk (including your picks).
Click a row to show only that block.

## Menu
Press J. Left click a row to use it, right click (or click its +) for settings, drag a title
to move a panel, right click a title to fold it, Esc to close. Positions are remembered.
- **Finder**: Redstone, Storage, Dimension, Biome, Placed, My blocks.
- **Render**: Radar on/off, Block ESP (fill, max boxes), Chunks (SOLID, LINES, OFF; max
  chunks), HUD, Click hints.
- **Settings**: performance preset, scan time, rescan pause, menu color, reset layout.
- **Results**: suspicious count and percent, then every block found. Click one to show only
  it. Scroll for more.
- The HUD shows FPS and counts in the top left.
- **Pick blocks...**: a searchable list of all blocks. Click the ones to look for; they are
  saved and shown under **My blocks**. Ores and ancient debris are left out (x-ray). To see
  only your picks, switch the other categories off. For very common blocks, every one is
  counted but only up to 256 per chunk get positions (group sizes then show a "+").
- Each block has its own highlight color, set in `Colors.java`.
- Every scanned chunk with a highlighted block is marked in blue, from the bottom of the
  world to halfway up (Y=128 in the Overworld). SOLID fills it (walls hide the fill, and chunks within 8 blocks of you
  are not filled) and adds an outline that shows through walls. LINES is the outline only.
- **Storage items**: chests, trapped chests, ender chests, barrels and shulker boxes.
- Copper blocks and lightning rods are never highlighted.
- Redstone ore is never highlighted. Sculk is ignored where it grows naturally.
- **Player-placed** covers concrete, beacons and other blocks that never generate, plus
  deepslate tiles and variants above Y=20 or outside the Overworld.

## Limits
- Only chunks the game has loaded can be scanned. On servers this may be less than your
  render distance.
- Only the nearest blocks and chunks are drawn (600 and 64 by default); counts include all.
- For more FPS pick the Max FPS preset. Changes in the world show after the next scan.
- "Native" is decided by the lists in `Rules.java`. Blocks not listed are treated as
  normal. Natural structures (ruined portals, villages, ancient cities) and trees on
  biome borders can be flagged.
- Menu settings are saved in `.minecraft/config/blockradar.properties`.
- Many servers ban this kind of mod (DonutSMP bans radar and ESP). Use it in single player
  or where allowed.
