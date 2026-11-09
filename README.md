# Clan Dex

A RuneLite sidebar panel for clans that run a collecting game through their Discord bot.
Play in clan chat as usual, then open Clan Dex and press **Sync** to see everything you've
collected, how many times, and what's still out there.

**It only works for members of a clan that runs the matching bot.** You need your clan's key.

## Features

- A progress card: entries collected, completion %, and total catches.
- Search, All / Caught / Missing tabs, and sorting by number, name or most caught.
- Rows animate in and out as you filter, and the list cascades in after each sync.
- Click an entry for details; right-click to copy its name.
- Entry pictures come from your clan's bot: shown in full once caught, as a silhouette until then.
  If your clan's bot doesn't provide pictures, entries are listed by name only.
- A settings cog in the panel, and a **Panel width** setting for a wider, two-column view.

## Settings

| Setting | What it does |
| --- | --- |
| Clan key | Your clan's shared key. Ask in your clan's Discord. |
| Panel width | Standard, Wide or Extra wide |
| Name override | Look up someone else instead of your logged-in name |
| Dex data URL | Where your clan's bot publishes the data (preset for our clan) |

## Privacy

- Sync only runs when you press it, with a 30-second cooldown. The plugin never sends chat or
  Discord messages for you.
- Pressing Sync downloads one file from the **Dex data URL**. That host can see your IP address,
  as with any download. Nothing about you is uploaded.
- Entry pictures, if your clan's bot provides them, are downloaded from the address it lists.
- The file contains no player names. Each player is listed under a code made from their name
  and the clan key, so without the key the file can't be matched to anyone.

## Developing

See [CLAUDE.md](CLAUDE.md) for the architecture, the data format shared with the bot,
the rules, and how to run a dev client.
