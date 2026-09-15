# Changelog

## [1.1.0] - 2026-09-15
### Added
- **Minecraft 26.3 build.** The mod now ships a build for Minecraft 26.3 (Fabric Loader 0.19.3 -> 0.19.5, Fabric API 0.152.1+26.2 -> 0.160.5+26.3). The 26.2 and 26.1.2 builds carry on alongside it.
### Changed
- Inventory placement and invulnerability calls use the 26.3 API on the 26.3 build.

## [1.0.8] - 2026-07-24
### Changed
- **New mod icon** — replaced the placeholder with proper artwork (a villager shopkeeper with a "SHOP OPEN" sign).

## [1.0.7] - 2026-07-24
### Added
- **Edit a trade's price.** Left-click a trade in the setup menu to open a price editor and adjust the
  quantity of each price item with +/- buttons (left = 1, right = 8). No item juggling, so it can't
  duplicate items. (To change the price item type, delete and re-add the trade.)
### Fixed
- **Shop villagers no longer re-open the trade GUI repeatedly.** A single right-click could reach the
  interaction callback more than once (and while the crosshair rested on the villager), reopening the
  menu so it looked like it opened "on hover." Interactions are now debounced and consumed cleanly.
- **Finished localization.** The trade editor's slot labels ("Item to sell", "Price item 1/2",
  "Confirm trade", "Cancel") were still hardcoded English; they now use the language files, with
  matching en_us and zh_tw entries (plus keys for the new price editor).

### Note
- Owners open the **setup** menu (to manage), not the buy screen, so you can't buy from your own shop
  — this is by design.
### Added
- **Traditional Chinese (zh_tw) translation.** Thanks to caprese502 for the translation.

## [1.0.5] - 2026-07-21
### Added
- **Full localization support.** Every player-facing string — the shop setup menu, the stock and collected-payment containers, the trade editor, and all `/shop` command messages — is now translatable. English is the built-in default; see `TRANSLATING.md`.

## 1.0.4
- **Quieter trade sound.** The post-trade villager sound was played once per trade at full volume, so
  a bulk shift-click stacked many overlapping sounds into a loud burst. It's now throttled to at most
  once per tick (a bulk buy makes a single sound) and plays at reduced volume.

## 1.0.3
- **Fixed a duplication exploit where a paying customer could buy far past the shop's stock.** The
  trade's use count was never advanced, so the per-stock trade limit was never enforced — once the
  real stock ran out, the shop kept handing over goods for as long as the buyer could pay (goods
  conjured from nothing). Trades now correctly count toward their stock limit and grey out when the
  stock is actually gone.

## 1.0.2
- **Fixed a stock-duplication exploit via the Stock/Payments screen.** Those screens used to show a
  *copy* of the container and write it back when closed. If the owner kept the Stock screen open while
  a customer bought goods, closing it overwrote the live stock with the stale copy — restoring (and
  thus duplicating) everything the customer had taken. The screens are now a live window directly onto
  the real container: purchases show up immediately and can never be reverted by a stale save.

## 1.0.1
- **Fixed a duplication exploit when buying via shift-click.** Vanilla's merchant screen plays a
  trade sound by casting the merchant to an entity; our shops aren't entities, so shift-clicking the
  result crashed that path *after* handing over the goods but *before* charging the buyer — the
  customer kept their payment, the shop's stock and profits were untouched, and the goods were free.
  Buying now goes through a custom merchant menu that completes the trade correctly on shift-click.
  (Single-click buying was unaffected.)

## 1.0.0
- **Stable 1.0.0 release.** No gameplay changes from 0.4.0 — marks the mod stable and aligns it with the
  unified release across the mod suite.
- Jar filenames now include the Minecraft version (e.g. `villagershop-1.0.0+mc26.2.jar`).
- A parallel **MC 26.1.2** build is now published (`villagershop-1.0.0+mc26.1.2.jar`).

## 0.4.0
- Updated to **Minecraft 26.2** (Fabric loader 0.19.3, Fabric API 0.152.1+26.2; Loom 1.16.2 and
  Gradle 9.4.0 unchanged). No gameplay changes.

## 0.3.1
- Fixed the customer trade screen showing up **blank**: a `MerchantMenu` opened with a custom
  merchant doesn't auto-sync its offers, so we now use `Merchant.openTradingScreen(...)` which
  both opens the menu and sends the offers packet to the client.

## 0.3.0
- Setup GUI gained: **Rename** (opens a chat rename prompt), **Variant** cycle (plains, spruce/
  taiga, snow, desert, jungle, savanna, swamp), and **Rotate** (turn the villager 90° through the
  four cardinal directions).
- **Bigger stock & payment screens** — now 90 slots each, shown as **two pages** (45 per page)
  with Previous/Next navigation.
- Stock and payment screens now have a **Back to Setup** button.

## 0.2.0
- Setup GUI cosmetics: toggle the villager's **glow** and cycle its **profession** (14 looks).
  Both apply to the nearby villager and persist with the shop.
- New commands: `/shop rename <name>` (renames the shop you're standing near and its name tag)
  and `/shop admin list` / `/shop admin remove` for operators.

## 0.1.0
- Initial implementation: server-side player-owned villager shops for Minecraft 26.1.2.
- `/shop create` spawns an invulnerable, no-AI, silent, persistent shop villager (no backing
  block); `/shop remove` (owner/op, stand near it) returns items and deletes it; `/shop list`
  lists your shops with coordinates.
- Owner right-click → chest-style setup GUI (add/delete trades, deposit stock, withdraw
  collected payments, remove shop with confirm). Customer right-click → vanilla merchant screen
  backed by a custom merchant that pulls goods from stock and banks payments.
- Player-stocked, limited economy: a trade is a sell item + 1–2 price items (any items);
  trades grey out when stock runs out. (v1: price matching is by item type + count only.)
- Shops persist in `<world>/villagershop/shops.dat`; config `config/villagershop.json` with
  `maxShopsPerPlayer` (default 3, ops unlimited).
