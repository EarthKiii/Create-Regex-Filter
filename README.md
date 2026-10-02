# Create: Regex Filter

An addon for [Create](https://github.com/Creators-of-Create/Create) that adds the **Regex Filter**: a filter that sorts
items and fluids by their **name**, in any language, or by the **mod** that adds them, using a regular expression.

It works everywhere Create accepts a filter: funnels, brass tunnels, deployers, smart chutes, smart fluid pipes,
mechanical arms, stock keeper categories, and more.

## Features

- Match by **item name**, in any language, regardless of the game's own language.
- Match by **mod ID**, e.g. everything from `create`.
- **Allow** or **deny** matches.
- **Ignore case** toggle, on by default.
- **Test slot** that shows the exact text being matched and whether an item would pass.
- Drag items into the test slot from **JEI** or **EMI**.
- Safe on servers: no pattern can freeze the game, however it is written.

## Requirements

| | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219 or newer |
| Create | 6.0.10 or newer 6.0.x |
| JEI or EMI | Optional |

Install the jar on both the client and the server.

## Usage

Craft the filter from a **book** between two **brass nuggets**. It is also in its own creative tab, *Create: Regex
Filter*. Right-click it to configure it, then put it in any Create filter slot.

### The filter screen

| Control | What it does |
| --- | --- |
| Pattern field | The regular expression, up to 32 characters. It turns red when invalid; hover the status text for details. |
| `Aa` button | Ignore case: upper and lower case letters match each other. |
| Language box | The language item names are read in. Scroll to change it, click to go back to your game language. Empty when matching mod IDs. |
| Name tag / `@` buttons | Match the item name or the mod ID. |
| Allow / deny buttons | Let only matching items through, or everything except them. |
| Test slot | Put an item in (click, shift-click, or drag from JEI or EMI) to preview the filter. Not saved. |

### Matching rules

- The pattern matches **anywhere** in the text. Use `^` and `$` to match the whole text.
- Renamed items are matched by their custom name.
- An **invalid pattern lets nothing through**, even in deny mode, so a typo never floods a system.
- The syntax is [RE2's](https://github.com/google/re2/wiki/Syntax): like Java or JavaScript regular expressions,
  without back-references (`\1`) and lookarounds (`(?=...)`). Case is set with the `Aa` button, not `(?i)`.
- Filters can be copied with Create's filter copying recipe, and cleared by crafting them on their own.

### Examples

With Ignore Case on (the default):

| Pattern | Matches | Mode |
| --- | --- | --- |
| `ingot$` | Iron Ingot, Gold Ingot, Brass Ingot, ... | Item name |
| `^(iron\|gold)\b` | Iron Ingot, Iron Sword, Gold Nugget, but not *Golden* Apple | Item name |
| `\bore$` | Iron Ore, Deepslate Gold Ore, but not Heavy *Core* | Item name |
| `^create$` | Every item from Create | Mod ID |
| `^(minecraft\|create)$` + deny | Everything not from Minecraft or Create | Mod ID |

## Languages on dedicated servers

In singleplayer every language is available. A dedicated server reads languages from its mods and data packs, so
names from mods (Create included) work in every language they ship.

Minecraft's own item names are the exception: the server only has them in English, so vanilla items fall back to
English names in other languages. To fix this, add a data pack containing `assets/minecraft/lang/<code>.json`, copied
from a client's `.minecraft/assets` folder, then run `/reload`.

## Performance

Patterns run on [RE2J](https://github.com/google/re2j), Google's Java port of RE2, which matches in time linear in the
length of the name. Java's built-in engine can take exponential time on patterns like `(a+)+$`; RE2J cannot. Patterns
whose compiled form would be huge, like `((a{1000}){1000}){1000}`, are rejected before compiling.

Results are cached so that a filter check usually costs a single map lookup:

- Plain-text patterns (`ingot`, `^Iron`) skip the regex engine entirely.
- Each item's result is cached, including for damaged or enchanted items, whose name does not change.
- Names that come from an item's data (renamed items, potions, written books) are cached by name.
- All filters with the same settings share their caches, across the whole world.

RE2J is bundled inside the mod jar; there is nothing else to install.

## Building from source

Requires a JDK 17 or newer to run Gradle; Java 21 for the mod itself is downloaded automatically.

```sh
./gradlew build                    # the mod jar, in build/libs
./gradlew test                     # unit tests of the regex engine
./gradlew runGameTestServer        # in-game tests on a server with Create
./gradlew runClient                # development client with Create
./gradlew runClient -Pviewer=jei   # ... with JEI (or -Pviewer=emi)
```

## License

[MIT](LICENSE)
