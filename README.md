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
   `blockradar-jar`. Unzip it to get `blockradar-1.8.0.jar`.

### Option B: build on your computer
1. Install JDK 25 (for example from adoptium.net).
2. Open a terminal in this folder.
3. Windows: `gradlew.bat build`   Mac/Linux: `sh gradlew build`
4. The jar is `build/libs/blockradar-1.8.0.jar` (not the `-sources` one, if present).

## Install
Install Fabric Loader for 1.21.11, then put `blockradar-1.8.0.jar` and the Fabric API jar
in your `.minecraft/mods` folder.

## Controls (changeable in Options > Controls)
| Action | Default |
|---|---|
| Open menu | J |
| Radar on/off | K |
| Inspect a highlighted block | Look at it and left click (your Attack button) |

The counts appear in a panel in the top right corner.

While the radar is on, left clicking a highlighted block opens its counts instead of
breaking it (press K to break it). Mobs can still be hit and normal blocks mined. If a
normal block in reach is in front of a highlight, sneak and left click to inspect it.

## Menu
Three dropdown panels. Drag a title to move a panel, click it to fold it, press Esc to close.
Their positions are remembered.
- **Radar**: on/off switch, blue chunk outlines on/off, number of suspicious blocks and their percent of all scanned
  (non-air) blocks.
- **Categories**: Redstone, Wrong dimension, Wrong biome, Player-placed, My blocks.
- **Pick blocks...**: a searchable list of all blocks. Click the ones to look for; they are
  saved and shown under **My blocks**. Ores and ancient debris are left out (x-ray). To see
  only your picks, switch the other categories off. For very common blocks, every one is
  counted but only up to 256 per chunk get positions (group sizes then show a "+").
- **Blocks**: every block found, with count and share. Click one to show only it. Scroll for more.
- Each block has its own highlight color, set in `Colors.java`.
- Every scanned chunk with a highlighted block is outlined in blue, from the bottom of the
  world to the top.
- Redstone ore is never highlighted. Sculk is ignored where it grows naturally.
- **Player-placed** covers ender chests (outside the End), shulker boxes, concrete,
  beacons and other blocks that never generate, plus deepslate tiles and variants above
  Y=20 or outside the Overworld, and normal and copper chests below Y=-35.

## Limits
- Only chunks the game has loaded can be scanned. On servers this may be less than your
  render distance.
- Outlines are drawn for the nearest 2000 hits; counts include all hits.
- "Native" is decided by the lists in `Rules.java`. Blocks not listed are treated as
  normal. Natural structures (ruined portals, villages, ancient cities) and trees on
  biome borders can be flagged.
- Menu settings are saved in `.minecraft/config/blockradar.properties`.
- Many servers ban see-through highlighting. Use it in single player or where allowed.
