# CLAUDE.md: Clan Dex (RuneLite plugin)

Guide for AI assistants and humans working on this repo. **Keep it current:** any change to
the structure, the data contract, the rules below or the dev workflow must update this file
in the same commit.

## What this is

A RuneLite sidebar plugin that shows a player's progress in the clan's Discord collecting
game. The game itself is run by the clan's Discord bot, which lives in a separate private repo
run by the clan's bot maintainer. That repo has its own CLAUDE.md with the bot's side of the contract.

How the whole system fits together:

```
OSRS clan chat --(DropTracker plugin)--> Discord game channel --> clan bot (Node, maintainer's PC)
                                                                     |  saves catches locally
                                                                     |  uploads hashed copy
                                                                     v
RuneLite: Clan Dex panel --(Sync button, HTTPS GET)--> GitHub secret gist: dex.json
```

- Players play by typing the bot's commands in clan chat. **This plugin never sends chat or
  Discord messages.** It only downloads the published file when the user presses Sync.
- Sync has a 30-second cooldown (`ClanDexPanel.COOLDOWN_SECONDS`).
- The plugin is only useful to clan members: without the clan key nothing can be looked up.

## File map

| File | Responsibility |
| --- | --- |
| `ClanDexPlugin.java` | Lifecycle, sidebar button, settings cog hook, runs a sync across threads |
| `ClanDexConfig.java` | Settings: clan key, panel width, name override, data URL |
| `DexClient.java` | Downloads and parses the dex file |
| `SpriteClient.java` | Downloads entry images from the bot's link pattern; session cache |
| `DexKey.java` | Name normalisation, the HMAC key, and decrypting the player list; must match the bot exactly |
| `stats/StatFormula.java` | Interface for a stat formula (HP, Atk, Def, Spe, Spc from base stats, IVs, level) |
| `stats/Gen1Formula.java` | The Gen 1 formula (the default) |
| `stats/StatFormulas.java` | Formulas by name; the bot's `statFormula` picks one, unknown names fall back to Gen 1 |
| `SyncResult.java` | Immutable result of one sync: `DexInfo` + `PlayerDex`, no entry, or an error |
| `DexInfo.java` | File-level data: entry names, sprite / shiny / item picture links, item names |
| `PlayerDex.java` | One player's data: catch counts, each `Catch`, and their bag |
| `Catch.java` | One catch: id, number, entry, shiny, gender, nature, ability, date, ball, throws, level, IVs (details may be missing) |
| `ui/ClanDexPanel.java` | The panel: title bar, sync, Dex / Bag tabs; Dex has search, filters (incl. Shiny), sort, list/skeleton |
| `ui/BagPanel.java` | The Bag tab: the unlimited free ball, then items with pictures and counts |
| `ui/PartyPanel.java` | The Party tab: up to 6 cards with sprite, level, HP bar, stats, moves |
| `ui/PartyStore.java` / `ui/PartyControl.java` | Where the party is saved (RuneLite settings, per player) / how catch lines add to it |
| `ui/TabBar.java` | Flat tab row used for Dex / Bag and the filters |
| `ui/Links.java` | Fills in `{n}` / `{id}` in the bot's picture link patterns |
| `ui/AnimatedList.java` | List that animates rows in/out and flows into columns when wide |
| `ui/DexRow.java` | One entry: sprite (shiny if owned), number, name, count; click to see each catch; right-click menu |
| `ui/DexSummary.java` | The stats card |
| `ui/Skeleton.java` | Shimmering placeholder rows shown while syncing |
| `ui/SpriteLoader.java` | Interface the panel uses to ask for images (implemented by `SpriteClient`) |
| `ui/Sprites.java` | Trims and scales sprites, makes silhouettes and loading placeholders |
| `ui/Icons.java` | Sidebar ball (teal and gold) and cog icons, drawn in code |
| `ui/Theme.java` | **All** colours, fonts, sizes and animation timings |
| `ui/Layout.java` | Small Swing helpers |
| `src/test/.../ClanDexPluginTest.java` | Launches a dev client with the plugin loaded |
| `src/test/.../stats/StatFormulasTest.java` | Gen 1 worked examples; picking formulas by name |
| `src/test/.../DexKeyTest.java` | Checks the hash matches a value produced by the bot |
| `src/test/.../DexClientTest.java` | Gist link handling; reading formats 2 and 3, catches, bag, items |
| `dev/dex.sample.json` | The reviewer demo (DEMO.md): made-up names, 24 catches, 2 shinies, a hidden ability, a bag, 2 players; no pictures. `demoSampleLoads` keeps it working |
| `dev/dex.local.json` | **Gitignored** sample with the real names and sprites (see Dev workflow) |
| `DEMO.md` | Reviewer demo: settings, what to try, recording shot list |
| `icon.png` | Plugin Hub icon (48x72), the same ball as `Icons.NAV` |

## Data contract (shared with the bot)

The bot publishes this file. Changing the shape means changing **both** repos together and
bumping `version` (`DexClient.SUPPORTED_VERSION`).

```json
{
  "version": 3,
  "updated": "2026-11-08T20:00:00.000Z",
  "names": ["First entry", "Second entry", "..."],
  "sprite": "https://example.com/sprites/{n}.png",
  "spriteShiny": "https://example.com/sprites/shiny/{n}.png",
  "items": { "great-orb": { "name": "Great Orb" } },
  "itemSprite": "https://example.com/items/{id}.png",
  "players": { "<64 hex chars>": {
    "rev": 7,
    "dex": { "0": 1, "24": 3 },
    "mons": [ { "id": "d82555a16d", "no": 1, "dex": 24, "shiny": true, "gender": "female",
                "nature": "Adamant", "ability": "Static", "caughtAt": "2026-11-09T01:11:52.595Z",
                "caughtWith": "basic-orb", "throws": 3, "level": 14,
                "ivs": { "atk": 2, "def": 5, "spe": 5, "spc": 9 } } ],
    "bag": { "great-orb": 1 } } }
}
```

- `names` and `sprite` come from the bot, so this repo contains no entry names or artwork.
  Keep it that way.
- `sprite` is optional: a link pattern where `{n}` is the 1-based entry number. Only `https`
  links are used. While a sprite loads its row holds a blank space; if it fails the space stays
  blank. With no `sprite` field, rows show names only, with no picture and no gap. The plugin
  has no pictures of its own, by design.
- `players` keys are `HMAC-SHA256(clanKey, playerId)` as lower-case hex. `playerId` is `osrs:` plus
  the in-game name, lower case, keeping only letters, digits, spaces and hyphens, with OSRS
  non-breaking spaces turned into normal spaces. See `DexKey`; `DexKeyTest` pins it to a value
  produced by the bot.
- Each player's record has `dex`, mapping an entry **index** (0 = #001) to a catch count, plus
  optional `mons` (each catch; see `Catch.java`, every detail but `dex` may be missing) and `bag`
  (item id to count). A catch's `no` is the player's number for it within its species (#001,
  #002...); the catch list is ordered by it and shows it, because the bot's commands use it
  (e.g. its level-up command). The bot's `display` object fills in game-specific UI: `display.unlimitedItems` (`[{ id, name }]`)
  are shown first in the Bag as "Unlimited" with any count ignored, and `display.terms.entry` /
  `entries` are the on-screen words for an entry (`DexInfo.entry()` / `entries()`, defaulting to
  "entry" / "entries"). The plugin never names game things itself; new display settings go there.
  plugin **ignores every field it doesn't know** (`rev`, and anything the bot adds later such as a
  nickname), so the bot can add fields without a plugin update. Only breaking changes bump
  `version`. Format 2 (`"dex": { key: { index: count } }`) is still read, for a bot that hasn't
  updated yet; `DexClientTest` covers both.
- **No names are ever published.** Without the clan key, the file can't be matched to players,
  not even by guessing names from the hiscores.

## Party, player list and wrong keys

- **Party** is local: up to 6 catch ids saved in RuneLite's config as `party_<first 16 chars of the
  player key>` (never a name), by `ClanDexPlugin.SavedParty`. Nothing is sent. Catches no longer in
  the player's data are dropped on load. Battles would need the party to reach the bot, which needs
  an authenticated way for the plugin to send requests (see the next item).
- **Trades are postponed.** Accepting a trade must be provably the player, and the plugin can't
  write to the bot yet. Likely design: a one-time link (the plugin makes a token, the player types
  `--link <code>` once in clan chat), then signed requests via a Discord webhook the bot reads.
- **Player list:** the file's `members` map player keys to names encrypted with the clan key
  (AES-256-GCM; `DexKey.decryptName`, mirroring the bot's names.js). A wrong key fails to decrypt
  (null), never garbage. `keyCheck` decrypts to `clan-dex-key-check` with the right key; if it
  doesn't, the sync stops with "That clan key doesn't match your clan's" and shows nothing.
- **Stats:** `baseStats` (by dex number) + a catch's IVs and level go through the `StatFormula`
  named by the file's `statFormula` (default `gen1`: Gen 1 formula, HP IV built from the other IVs'
  low bits). New formula: implement `StatFormula`, register it in `StatFormulas`, then have the bot
  publish its name. HP shows as full until battles exist.

## Threads (the most common source of RuneLite bugs)

- Swing thread: all UI code, including animation timers (`javax.swing.Timer`).
- Client thread: anything reading game state (`client.getLocalPlayer()`). Use `clientThread.invoke`.
- OkHttp thread: network callbacks. Always `SwingUtilities.invokeLater` before touching UI.

## Plugin Hub review risk

Loading images from a link supplied by a remote file is unusual for the Hub. If reviewers push
back, the fallback is names only: the bot omits `sprite` and rows show no pictures. No plugin
change is needed.

## RuneLite workarounds in use

- **Settings cog:** RuneLite has no API to open a plugin's settings. `openSettings()` posts the
  `OverlayMenuClicked` event that an overlay's "Configure" entry sends, with a stand-in overlay
  naming this plugin. If a RuneLite update breaks the cog, look there first.
- **Panel width:** RuneLite fixes panels at 225px via `PluginPanel.getPreferredSize()`. The
  sidebar sizes itself from that, so `ClanDexPanel` overrides it with the configured width.

## Rules

1. **Plugin Hub safe.** No automated chat, input or Discord actions. Network use is limited to
   GET requests the user triggers, to the URL in settings.
2. Use RuneLite's injected `OkHttpClient` and `Gson`. Never construct your own.
3. No third-party dependencies beyond what RuneLite provides.
4. **No franchise material committed to this repo**: no franchise or creature names, no
   official artwork or sprites in code, docs, metadata or assets. Names and sprite links come
   from the bot's file at runtime (a deliberate, accepted fan-project risk on the bot's side).
   Refer to the game as "the clan's collecting game". Real-name test data goes only in
   gitignored `dev/*.local*` files. The icon is a ball in teal and gold, deliberately not
   the classic colours.
5. Never publish or log player names or the clan key. The key is a `secret` config item.
6. Colours, fonts, sizes and timings go in `Theme.java`, never inline in components.
7. Animations use one Swing `Timer` per animating component and stop when settled.
8. Code style follows RuneLite: tabs, braces on their own line, Lombok allowed.
9. Comments explain **why** (threads, cross-repo coupling, RuneLite quirks), not what the next
   line does. Every class gets a short Javadoc saying what it is for.
10. Strings shown to players are short, plain, and say what to do next.
11. Do not commit or push unless asked.

## Dev workflow

Needs JDK 11 (`java -version`). From the repo root:

```bash
./gradlew run          # starts a developer RuneLite client with the plugin loaded
./gradlew test         # runs DexKeyTest and DexClientTest
./gradlew compileJava  # quick compile check
```

Testing without logging in: serve the sample file and point the plugin at it.

```bash
python3 -m http.server 8765 --bind 127.0.0.1 --directory dev
```

Then in the plugin's settings set **Clan key** to `clan-dev-key`, **Dex data URL** to
`http://127.0.0.1:8765/dex.sample.json` and **Name override** to `Fake Test Player`.

For the real names and sprites, point **Dex data URL** at `dex.local.json` instead. It isn't
committed; regenerate it from the bot's entry list (in its `index.js`) if it's missing (same shape as the
sample, plus the bot's `sprite` link pattern, keyed with `clan-dev-key` and `osrs:fake test player`).

Logging in on the dev client with a Jagex account needs a one-off step: launch RuneLite from
the Jagex Launcher with `--insecure-write-credentials` in the client arguments. That writes
`~/.runelite/credentials.properties` (a login token, so delete it when done).

`build.gradle` uses `runeliteVersion = 'latest.release'`, as RuneLite's template does, so the dev
client tracks the live game. If it won't connect after a game update, run
`./gradlew run --refresh-dependencies`. The Plugin Hub swaps in its own `build.gradle` anyway
(`build=standard` in `runelite-plugin.properties`).

## Distribution (Plugin Hub)

Regular RuneLite installs can only load plugins from the Plugin Hub. Sideloading needs
developer mode on every member's client, so it isn't a practical way to share with a clan.

Ready: `LICENSE` (BSD-2), `icon.png`, `runelite-plugin.properties` (`build=standard`), README,
no franchise material, and `ClanDexConfig.DEFAULT_DATA_URL` pointing at our clan's gist
(`modiov2-web/587c835c…`), so members only need the clan key. `DexClient.toFileUrl()` also
accepts a gist's page link, since that's the link people copy. Still to do before submitting:

1. Test end to end with real catches from clan chat.
2. Open a PR to `runelite/plugin-hub` adding `plugins/clan-dex` with `repository=` and
   `commit=` (the full commit hash to publish).
