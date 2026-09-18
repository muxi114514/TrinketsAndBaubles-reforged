# Trinkets and Baubles: Reforged (1.20.1)

This is an unofficial port of **Trinkets and Baubles**, a Minecraft mod by **XzeroAir**, from 1.12.2 (v0.33.4) to **Forge 1.20.1**.

I really liked the original mod and wanted to play it on a newer version, so here we are. The goal is to keep things as close to the original as possible. Old bugs got fixed along the way, and a few things were tidied up.

## What's in it

- **Races.** Become a Fairy, Dwarf, Titan, Goblin, Elf, Faelis, Dragon or Taurus. You can wear a race ring, eat a transformation food or drink a transformation potion. Each race has its own size, abilities and quirks. Dragons can also take on a Fire, Ice or Lightning form.
- **Trinkets.** The Dragon's Eye, Ender Queen's Crown, Arcing Orb, Polarized Stone, Stone of the Sea, Shield of Honor, Wither Ring, Poison Stone, Faelis Claw, the inertia stones, the Teddy Bear, and more. They all go into Curios slots.
- **Mana.** A simple mana system that powers the active abilities. You can raise your max mana with Mana Crystals.
- **Race customization.** A menu for choosing your race colors and trait variants, plus a screen that lists all your active abilities.
- **Config screens.** Almost every number in the mod can be changed in-game.

## Changes from the original

- Tooltips were rewritten. They now show the real numbers taken from your config: chances, durations, mana costs and cooldowns. You don't get vague lines like "gives some resistance" anymore. By default they are collapsed; hold Shift to see everything.
- Race resizing is handled by the mod itself, so Pehkui isn't needed.
- A handful of old bugs got fixed. One example: some config rules never actually applied on 1.12.2.

## Compat

These mods are all optional. When they're installed, the mod hooks into them:

- **First Aid**: Hard Head (the Shield of Honor can save you from head shots) and Blessing of Life work with body parts
- **Cold Sweat**: heat and cold immunity for fitting races and trinkets
- **Simple Difficulty**: thirst and parasite immunity
- **Thirst Was Taken**: the Stone of the Sea refills your thirst while you're in water
- **Enhanced Visuals**: some trinkets block certain screen effects
- **Elenai Dodge 2 / Talents**: their dodge (Sidestep) triggers the Arcing Orb's dodge effects
- **Lycanites Mobs / Defiled Lands**: some of their effects are used or blocked where it makes sense

Curios is required.

## Status

It's a full port, but it's still early. Everything compiles and loads, but not everything has been tested in real gameplay yet. If something breaks or behaves differently from the 1.12.2 version, please open an issue.

## Credits

- **XzeroAir**: the original mod. All the ideas, most of the art and the design are theirs.
  - Original source: https://github.com/XzeroAir/Trinkets
  - CurseForge: https://www.curseforge.com/minecraft/mc-mods/trinkets-and-baubles
- Art and translation contributors to the original: NyanMinecrafter, ArtsyDy, 502y, Z-tunic, Cypheriel, TheGamersBrew, KL BanYourself, Nischhelm, KameiB.
- 1.20.1 port: muxi114514.

## License

The original mod is licensed under the **GNU Affero General Public License v3.0**, so this port uses the same license. See [LICENSE](LICENSE) for details.
