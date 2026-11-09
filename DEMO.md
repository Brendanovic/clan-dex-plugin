# Trying Clan Dex without a clan (reviewer demo)

Clan Dex normally reads the data a clan's Discord bot publishes, which only that clan's members can
use. This repo includes a demo file with made-up data so anyone can see every feature in a few
minutes. Nothing is sent anywhere; the plugin only downloads the demo file from this repository.

## 1. Point the plugin at the demo

Install Clan Dex, open its settings (the cog at the top of the panel, or
**Configuration → Clan Dex**) and set:

| Setting | Value |
| --- | --- |
| Dex data URL | `https://raw.githubusercontent.com/Brendanovic/clan-dex-plugin/main/dev/dex.sample.json` |
| Clan key | `clan-dev-key` |
| Name override | `Fake Test Player` |

You don't need to be logged in: the name override picks the demo player.

## 2. What to try

| Step | What to do | What you should see |
| --- | --- | --- |
| Sync | Open the panel, press **Sync** | A shimmer while loading, then the list settles in. The card reads **14 / 151**, 24 total catches, 2 shiny. |
| Filters | Click **Caught**, **Missing**, **Shiny**; type in the search box | Rows slide in and out with no gaps. **Shiny** leaves two entries. |
| Sort | Choose **Sort by most caught** | The list reorders, and cascades in again. |
| An entry | Click the first entry (#001) | It opens: three catches, #001 to #003, each with a gender symbol and level. |
| Details | Press **Show more** on a catch | Labelled details: caught with, caught on, throws, nature, ability, IVs and moves. Each catch opens on its own. |
| Shiny / hidden | Open #025 and #016 | #025 #002 is shiny (gold); #016 #003 has a hidden ability (gold). |
| Party | Press **+ Party** on a few catches, open the **Party** tab | Cards with level, an HP bar, stats and moves. **x** removes one. Up to 6. |
| Bag | Open the **Bag** tab | "Basic Orb: Unlimited" first (from the demo's display settings), then Great Orb x2, Master Orb x1, Level Treat x1. |
| Party hint | Open the **Party** tab with nothing added | "Empty slot - open a creature in the Dex…": the word "creature" comes from the demo's display settings. |
| Wrong key | Change **Clan key** to anything else, press **Sync** | "That clan key doesn't match your clan's", and nothing is shown. |
| Width | Set **Panel width** to Extra wide | The sidebar widens and the list flows into two columns. |

The demo file has no picture links, so entries show by name only; a real clan's bot can add pictures.

## 3. Running it from source

```bash
./gradlew run      # starts RuneLite in developer mode with Clan Dex loaded
./gradlew test     # the tests, including one that checks this demo file loads
```

## Recording the demo (for the Plugin Hub PR)

Record each clip with macOS **⌘⇧5 → Record Selected Portion** around the Clan Dex panel only
(about 10–20 seconds each), using the demo settings above:

1. **Sync:** press Sync; the shimmer, then the list settling in.
2. **Filtering:** Caught, Shiny, then type a few letters in search.
3. **A catch:** open #001, then Show more on two catches.
4. **Party:** + Party on three catches, then the Party tab.
5. **Bag:** open the Bag tab.
6. **Wrong key:** change the key, Sync, the message.

Upload the clips to the Plugin Hub PR description (drag them into the text box on GitHub).
