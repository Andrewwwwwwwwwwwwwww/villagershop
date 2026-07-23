# Translating Villager Shop

Everything a player sees — the shop setup menu, the stock/payment containers, the trade editor, and
all command messages — is translatable. There is one file to translate: `en_us.json`.

## For translators

1. Start from the English template:
   [`src/main/resources/assets/villagershop/lang/en_us.json`](src/main/resources/assets/villagershop/lang/en_us.json).
2. Translate only the **values**, never the keys.
3. **Keep the placeholders**: `%s` (names/text), `%d` (numbers). Keep them; reorder with `%1$s` etc.
   if your language needs it. A few keys have `_one` / `_many` variants for singular vs plural.
4. Save as `<locale>.json` using your Minecraft language code (e.g. `zh_tw.json`), as **UTF-8**.
   Partial translations are fine — anything untranslated shows English.

### Key groups
| Prefix | What it is |
| --- | --- |
| `villagershop.setup.*` | The owner setup menu (buttons + tooltips) |
| `villagershop.trade.*` | The trade editor and each trade's price/stock lines |
| `villagershop.container.*` | The stock / collected-payments container pages |
| `villagershop.cmd.*` | `/shop` command messages |
| `villagershop.on` / `.off` / `.near_villager` | Small shared words |

Notes:
- A shop's **name** is set by its owner (`/shop rename`), so it is never translated.
- The default name a new shop gets ("<Owner>'s Shop") is the owner's to change and is left as-is.

## For the server owner (installing a translation)

Menus and messages are drawn by the **server**, per player, from the language their client reports.

- **This server only:** drop the file at `<world>/villagershop/lang/<locale>.json` and restart.
- **Bundle for everyone:** send finished translations to the author to be added to the jar.

## Bundled translations

- Traditional Chinese (`zh_tw`) — caprese502
