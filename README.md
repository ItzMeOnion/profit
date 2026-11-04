# Donut Profit Tracker

A client-side Fabric mod for **Minecraft Java 1.21.8** that keeps a running tally of every payment you send and receive on Donut SMP.

- **Profit / loss / even** status with a `+` / `-` net figure
- **Pop-out HUD panels** (Summary, Recent feed, Gamblers) that float over your game, with adjustable opacity and drag-to-move layout
- **P key menu** with Overview, Transactions, Gamblers and Settings tabs
- **Gamblers board**: ranks players by their profit, by how much they spent with you, or by their loss
- Clean background mode, so menus float without the dark overlay

## Install and build

1. Install **JDK 21**.
2. In this folder run `gradle build` (install Gradle first, or run `gradle wrapper` once to create a wrapper).
3. The mod jar is in `build/libs/donut-profit-tracker-1.0.0.jar`. Drop it into your `.minecraft/mods` folder alongside **Fabric API** for 1.21.8.

Or let GitHub build it for you: push the repo and grab the jar from the **Actions** tab (the build artifact is called `donut-profit-tracker`).

## Controls

| Key | Action |
| --- | --- |
| `P` | Open the profit menu (rebindable in Controls, category "Donut Profit Tracker") |
| `Esc` | Close the current screen |

## How it reads payments

The mod reads **chat / system lines** and matches them against regex patterns in:

`config/donut-profit-tracker/config.json` (created the first time you run the mod)

Default patterns:

| Direction | Default regex (simplified) | Example it matches |
| --- | --- | --- |
| In | `you (received\|got) $AMOUNT from PLAYER` | `You received $5K from Notch` |
| In | `PLAYER (paid\|sent\|gave) you $AMOUNT` | `Notch paid you 2.5M` |
| Out | `you (paid\|sent\|gave) $AMOUNT to PLAYER` | `You sent $1.2M to Notch` |

Amounts understand `K`, `M`, `B` and `T` suffixes and commas.

**If Donut SMP words its messages differently, edit the patterns in config.json and restart the game.** Each pattern needs three named groups: `(?<amount>...)`, `(?<suffix>...)` (optional) and `(?<player>...)`.

## Reading the Gamblers board

Each player you have traded with gets a row:

- **Paid you**: money they sent you. This is what they spent with you.
- **You paid**: money you sent them.
- **Their net**: `You paid - Paid you`. Positive (gold) means they came out ahead, negative (green) means you did.

Sorting: *Their profit* (most they won from you), *Most spent* (most they put in with you), *Their loss* (most they lost to you).

## Files it writes

- `config/donut-profit-tracker/config.json`: patterns, HUD positions, opacity, accent colour, clean background, and so on
- `config/donut-profit-tracker/data.json`: payment history and session start time

Delete `data.json` (or use *Erase ALL payment history* in Settings) to start over.

## Tweaking

- Colours: `Draw.java` (GREEN, RED, GOLD, PINK...)
- Default panel positions: `Settings.defaultPos` in `Settings.java`
- Session length, history cap: `Ledger.MAX_STORED` in `Ledger.java`

## Notes

- Client-side only. It reads chat that your client already receives and doesn't send anything to the server.
- Made for Minecraft 1.21.8 with Yarn mappings. Fabric API version `0.136.0+1.21.8` is set in `gradle.properties`. If the build complains about it, check the latest 1.21.8 version at https://modrinth.com/mod/fabric-api/versions and update the line.
