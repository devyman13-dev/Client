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
   `blockradar-jar`. Unzip it to get `blockradar-1.4.2.jar`.

### Option B: build on your computer
1. Install JDK 21 or newer (for example from adoptium.net).
2. Open a terminal in this folder.
3. Windows: `gradlew.bat build`   Mac/Linux: `sh gradlew build`
4. The jar is `build/libs/blockradar-1.4.2.jar` (not the `-sources` one, if present).

## Install
Install Fabric Loader for 1.21.11, then put `blockradar-1.4.2.jar` and the Fabric API jar
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
- The menu follows the supplied Mist Client reference: a left module sidebar, live search,
  favorite filter, and large block cards with counts, category color, favorite star, focus
  click, and a separate watch switch for each block type. The picker includes base-related
  and redstone blocks before they are first discovered.
- The default watch list includes common storage, crafting, utility, and redstone blocks.
  Copper blocks, netherrack, and reinforced deepslate are excluded from the picker and scan.
- Scan range is adjustable in two-chunk steps up to 32 chunks, capped by the game's render
  distance. The activity HUD reports matching blocks and suspicious chunks.
- Every chunk containing a watched suspicious block gets bright blue full-height edges and a
  blue frame around the player's current 16-block vertical section, like F3+G. Individual
  block outlines keep their category colors. The activity HUD reports
  the current suspicious chunk and block totals.
- Middle-click a highlighted block to show its coordinates, category, and count in a compact
  top-right HUD card at half scale. It stays visible until you inspect another block.
- Block dots and world outlines share category colors: red for redstone, purple for wrong
  dimension, orange for wrong biome, and cyan for player-placed candidates.
- **Base blocks** covers ender chests, shulker boxes, concrete,
  beacons and other blocks that never generate, plus deepslate tiles and variants above
  Y=20 or outside the Overworld.
  Normal and copper chests below Y=-35 are included too.
- Click a block card to focus that block type; use its watch switch to choose whether it is
  scanned and highlighted. Scroll the list for more.
- Overworld fire and common ruined-portal blocks such as netherrack, magma, blackstone,
  and portal nether bricks are ignored as natural features.
- Redstone ore is never highlighted. Sculk is ignored where it grows naturally.

## Limits
- Only chunks the game has loaded can be scanned. On servers this may be less than your
  render distance.
- Individual block outlines are limited to the nearest 2000 hits. Blue chunk borders are drawn
  for every scanned chunk that contains an enabled suspicious block; counts include all hits.
- "Native" is decided by the lists in `Rules.java`. Blocks not listed are treated as
  normal. Natural structures (ruined portals, villages, ancient cities) and trees on
  biome borders can be flagged.
- Menu settings are saved in `.minecraft/config/blockradar.properties`.
- Many servers ban see-through highlighting. Use it in single player or where allowed.
