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
   `blockradar-jar`. Unzip it to get `blockradar-1.4.0.jar`.

### Option B: build on your computer
1. Install JDK 21 or newer (for example from adoptium.net).
2. Open a terminal in this folder.
3. Windows: `gradlew.bat build`   Mac/Linux: `sh gradlew build`
4. The jar is `build/libs/blockradar-1.4.0.jar` (not the `-sources` one, if present).

## Install
Install Fabric Loader for 1.21.11, then put `blockradar-1.4.0.jar` and the Fabric API jar
in your `.minecraft/mods` folder.

## Controls (changeable in Options > Controls)
| Action | Default |
|---|---|
| Open menu | J |
| Radar on/off | K |
| Inspect a highlighted block | Look at it and middle click (your Pick Block button) |

## Menu
- The top shows how many suspicious blocks were found and what percent of all scanned
  (non-air) blocks that is.
- The menu has a dark category sidebar and a scrollable block list. Click a category to
  filter its blocks; use the switch beside it to enable or disable that category.
- Block dots and world outlines share category colors: red for redstone, purple for wrong
  dimension, orange for wrong biome, and cyan for player-placed candidates.
- **Player-placed** covers ender chests (outside the End), shulker boxes, concrete,
  beacons and other blocks that never generate, plus deepslate tiles and variants above
  Y=20 or outside the Overworld.
  Normal and copper chests below Y=-35 are included too.
- Click a block row to highlight only that block type; scroll the list for more.
- Redstone ore is never highlighted. Sculk is ignored where it grows naturally.

## Limits
- Only chunks the game has loaded can be scanned. On servers this may be less than your
  render distance.
- Outlines are drawn for the nearest 2000 hits; counts include all hits.
- "Native" is decided by the lists in `Rules.java`. Blocks not listed are treated as
  normal. Natural structures (ruined portals, villages, ancient cities) and trees on
  biome borders can be flagged.
- Menu settings are saved in `.minecraft/config/blockradar.properties`.
- Many servers ban see-through highlighting. Use it in single player or where allowed.
