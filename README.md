# Villager Shop

A **server-side** Fabric mod for Minecraft 26.1.2 that lets players run player-owned shops
backed by villagers. No client mod required.

## Concept

- `/shop create` spawns an **invulnerable, no-AI, silent, persistent** villager. There is no
  block backing it (no barrel/chest to break) — all shop data is stored server-side, keyed to
  the villager's entity UUID and saved in the world folder.
- The **owner** right-clicks the villager to open a chest-style **setup GUI**: define trades,
  deposit stock, withdraw collected payments, tweak cosmetics, and remove the shop.
- **Customers** right-click the villager to open the **vanilla merchant trade screen**, backed
  by a custom merchant so the economy is fully controlled.

## Economy

Player-stocked, limited. A trade is a *sell item* plus *one or two price items* (any items).
Sales pull from the shop's stock and add the payment to the owner's collected balance; a trade
greys out when stock runs out.

## Ownership & safety

- Only the owner (or an op) can remove a shop. On removal, leftover stock and collected
  payments are returned to the owner.
- The villager cannot be damaged, pushed, despawned, or converted.

## Commands

| Command | Description |
| --- | --- |
| `/shop create` | Spawn a shop villager you own (subject to the per-player cap). |
| `/shop remove` | Remove the shop you are standing near (owner/op only). |
| `/shop rename <name>` | Rename the shop you are standing near. |
| `/shop list` | List your shops and their coordinates. |
| `/shop admin list` | (Op) List every shop on the server. |
| `/shop admin remove` | (Op) Remove the nearest shop regardless of owner. |

In the setup GUI you can also **rename** the shop, toggle the villager's **glow**, cycle its
**profession** and **biome variant** (e.g. spruce/taiga), and **rotate** it to face any cardinal
direction. The **stock** and **collected payment** screens hold 90 items across two pages and have
a Back button.

## Config

`config/villagershop.json`: per-player shop cap (default **3**), and related toggles.

## Status

Implemented and in active use (v0.3.1): shop creation, the owner setup GUI, vanilla merchant
buying, player-stocked trades, paged stock/payment screens, cosmetics, and admin tools.

## License

All Rights Reserved. See the [LICENSE](LICENSE) file — these mods are proprietary.
