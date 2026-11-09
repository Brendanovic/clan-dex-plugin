# Clan Dex

A RuneLite sidebar panel for clans that run a collecting game through their Discord bot. Members
play in clan chat as usual; Clan Dex shows them what they've collected, their party and their bag.

**It only works for members of a clan that runs the matching bot, and needs that clan's key.**
Reviewers and anyone curious can try every feature with the built-in demo: see [DEMO.md](DEMO.md).

## Features

- **Progress card:** entries collected, completion %, total catches and shinies.
- **Dex:** every entry, with search, All / Caught / Missing / Shiny filters, and sorting by number,
  name or most caught. Rows animate in and out as you filter, and the list settles in after each sync.
- **Each catch:** click an entry to see every catch of it (Name #001, #002...) with its gender and
  level. **Show more** opens its details: what it was caught with, when, throws, nature, ability
  (gold if hidden), IVs and moves. Right-click an entry to copy its name.
- **Shinies** get their own picture, a gold sparkle and their own filter.
- **Party:** press **+ Party** on up to 6 catches to see their level, HP, stats and moves. The party
  is saved in your RuneLite settings.
- **Bag:** anything your clan's game gives without limit, then the items you've found.
- **Pictures and wording** come from your clan's bot (for example its word for an entry), with
  neutral defaults. Pictures, if it provides them, show in full once caught and as a silhouette until
  then; without them, entries are listed by name.
- A settings cog in the panel, and a **Panel width** setting for a wider, two-column view.

## Settings

| Setting | What it does |
| --- | --- |
| Clan key | Your clan's shared key. Ask in your clan's Discord. A wrong key shows nothing. |
| Panel width | Standard, Wide or Extra wide |
| Name override | Look up another name instead of your logged-in one |
| Dex data URL | Where your clan's bot publishes the data (preset for our clan; a gist page link works too) |

## Privacy and network use

- The plugin only **reads**. It never sends chat or Discord messages, and never uploads anything.
- **Sync** runs only when you press it, with a 30-second cooldown. It downloads one file from the
  **Dex data URL** (a GitHub gist for our clan) and the pictures that file links to (from GitHub).
  Those hosts can see your IP address, as with any download.
- The file holds no readable player names: each player is listed under a code made from their name
  and the clan key, and the clan's player list is encrypted with that key. Without the key, the
  file can't be matched to anyone.
- Your party is stored only in your own RuneLite settings, under a code rather than your name.

## Developing

See [CLAUDE.md](CLAUDE.md) for the architecture, the data format shared with the bot, the rules,
and how to run a dev client. `./gradlew test` runs the tests.
