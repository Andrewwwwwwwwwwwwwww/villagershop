# Changelog

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
