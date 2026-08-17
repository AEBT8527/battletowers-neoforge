# Classic Battle Towers — NeoForge port, Minecraft 1.21.1

Unofficial **NeoForge** build of **Classic Battle Towers `1.2.1`** for **Minecraft 1.21.1**.
Upstream ([blxckdog/ClassicBattleTowers](https://github.com/blxckdog/ClassicBattleTowers), branch
`dev-1.21.1`, commit `c17e216`) ships Fabric only — Modrinth lists no NeoForge build at all, and its
newest release there is Fabric/1.21.5.

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.217 (`[21.1.0,)`) |
| Java | 21 |
| Build | ModDevGradle 2.0.141, Gradle 9.2.1 |
| Jar | `battletowers-1.2.1+mc1.21.1.jar` |
| License | CC BY-NC 3.0 (upstream's) |
| Status | Verified in-game — mod loads, golem summons and renders, towers generate |

No dependencies: upstream needed Fabric API, this build needs nothing but NeoForge.

## What the port involved

Two separate jobs, because upstream builds against **Yarn** mappings while NeoForge uses **Mojang
official** names. So on top of the loader swap, every Minecraft reference in the 12 source files had
to be re-mapped. Error count per wave: **300 → 210 → 182 → 102 → 68 → 16 → 0**.

**Loader layer**
- `ModInitializer` / `ClientModInitializer` → `@Mod` class plus `@EventBusSubscriber` handlers.
- **Registration moved to `DeferredRegister`.** Upstream registered entity types and sound events
  with immediate `Registry.register(...)` calls in static initialisers; NeoForge freezes the vanilla
  registries outside its own registration phase, so that style throws. The old field names are kept
  available through `battleTowerGolem()` / `..._HOLDER.get()` accessors so the rest of the mod reads
  the same as before.
- `FabricDefaultAttributeRegistry.register(...)` → mod-bus `EntityAttributeCreationEvent`.
- `ServerEntityEvents.ENTITY_LOAD` → `EntityJoinLevelEvent`. NeoForge's event fires on both sides,
  so the handler guards on `!level.isClientSide()` to keep upstream's server-only behaviour.
- `UseBlockCallback` → `PlayerInteractEvent.RightClickBlock`; the `ActionResult.FAIL` that stopped
  the chest opening becomes `event.setCanceled(true)` + `setCancellationResult(FAIL)`.
- `ServerTickEvents.START_WORLD_TICK` → `LevelTickEvent.Pre` (client side filtered out).
- `EntityRendererRegistry` / `EntityModelLayerRegistry` → `EntityRenderersEvent.RegisterRenderers`
  and `RegisterLayerDefinitions`. `@Environment(CLIENT)` → `@EventBusSubscriber(Dist.CLIENT)`.
- `fabric.mod.json` → `META-INF/neoforge.mods.toml`, plus a `pack.mcmeta` — Fabric mods do not need
  one, but without it NeoForge will not read the bundled `assets/` and `data/`.
- Deleted `battletowers.mixins.json`: it declared an empty mixin list and no mixin class ever
  existed, and `fabric.mod.json` never referenced it either.

**Yarn → Mojang**, everything verified against `neoforge-21.1.217-sources.jar` before being applied.
A sample of the renames: `World`→`Level`, `Identifier`→`ResourceLocation`, `Box`→`AABB`,
`SpawnGroup`→`MobCategory`, `MarkerEntity`→`Marker`, `dataTracker`→`entityData`,
`initGoals`→`registerGoals`, `canStart`/`shouldContinue`→`canUse`/`canContinueToUse`,
`Goal.Control`→`Goal.Flag`, `writeCustomDataToNbt`→`addAdditionalSaveData`,
`onCollision`→`onHit`, `getVisibilityCache`→`getSensing`, `setVelocity`→`setDeltaMovement`,
`shootAt`→`performRangedAttack`, `getTexture`→`getTextureLocation`,
`BipedEntityModel`→`HumanoidModel`, `LayerDefinition.of`→`create`, `render`→`renderToBuffer`,
`EntityAttributes.GENERIC_X`→`Attributes.X`, `broadcast`→`broadcastSystemMessage`.

**One trap worth recording.** Yarn's `DamageSource.getSource()` is Mojang's `getDirectEntity()`, and
Yarn's `getAttacker()` is Mojang's `getEntity()` — they cross over. A bulk rename that maps
`getSource()`→`getEntity()` **compiles**, because `getEntity()` exists, and silently swaps
direct-attacker for owner in the golem's damage handling. It was only caught because the other half
of the pair failed to compile.

## Verified in-game

- **Dedicated server**: starts clean, `Done (4.452s)`, **zero errors**; the datapack (10 tower
  structures, structure set, biome tags, loot tables) loads and a world generates.
- **Client**: loads with zero errors, `/summon battletowers:battle_tower_golem` spawns the golem and
  it renders correctly at its custom 2× scale with the right texture.
- **Worldgen**: `/locate structure battletowers:stone_tower` →
  `[-1792, ~, 2000] (2785 blocks away)`, so towers really do place.

The only warnings are eight `Missing subtitle translation` lines for the mod's sounds. Those are
upstream's: its `en_us.json` ships exactly two keys and never had `subtitles.*` entries.

## Loot rebalance (this port only)

Upstream's chest loot is flat: every floor hands out a near-guaranteed kit, nothing is ever
enchanted, and the boss chest is barely better than floor 1. The tables here were rewritten
following **RLCraft's** tower progression, read out of its own `config/battletowers.cfg`:

- **the climb is a gamble** — low floors are mostly supplies and empty rolls, so a tower is a risk
  rather than a vending machine;
- **gear steps hard per floor pair** — leather/stone → chainmail + iron tools → iron armour and the
  first *enchanted* pieces → diamond tools and enchanted kit → enchanted diamond at the boss;
- **the best items sit at 1–10% weight**, so a jackpot is possible on any floor and expected on none
  (RLCraft puts `nether_star` at 1%; the equivalent here is also 1%);
- **the middle floors are the exploration-kit tier** — RLCraft hands out belts, backpacks and
  atlases, so the vanilla stand-ins are maps, compass, spyglass, name tags, leads and ender pearls;
- **vanilla tables are pulled in as scaling filler**, which is exactly what RLCraft does with its
  `ChestGenHook` entries, with the roll count rising per floor.

| Chest | Character |
|---|---|
| Floors 1–2 | supplies and junk; a leather/stone piece at ~60% *not* to appear; 1% saddle |
| Floors 3–4 | chainmail, first iron tools, `simple_dungeon` filler, 5% random-enchanted book |
| Floors 5–6 | exploration kit, iron armour, first `enchant_with_levels` gear (5–15), mineshaft filler |
| Floors 7–8 | diamond tools, enchanted kit (15–25), golden apples, horse armour, 3% enchanted golden apple |
| Top / underground bottom | guaranteed enchanted diamond weapon (20–28), 70% chance of an enchanted diamond armour piece, treasure pool with enchanted book plus the rare slots below, and 2 rolls of dungeon filler |

### Rarity of the headline items

The run-defining items are deliberately uncommon — per roll of the boss chest:

| Item | Chance |
|---|---|
| Enchanted golden apple | 5.2% |
| Diamond block | 5.2% |
| Netherite upgrade template | 2.6% |
| Totem of undying | 2.6% |
| Nether star | 1.3% |

On floors 7–8 an enchanted golden apple or a totem is 1% each. The boss armour piece is a 70%
chance rather than a certainty, and boss enchants sit at levels 20–28 rather than near-max.

### How often towers generate

The structure set was thinned from `spacing 40 / separation 15` to **`spacing 68 / separation 24`**.
Density on a `random_spread` placement scales with 1/spacing², so that leaves roughly **a third as
many towers** (0.35×) — still findable, but a tower is now a landmark rather than scenery. Both
numbers live in `data/battletowers/worldgen/structure_set/battletower.json` if you want to retune;
raising `spacing` makes them rarer, and `separation` must stay below it.

The underground tower mirrors the same curve with a cave flavour (raw ores, amethyst, lanterns, a 2%
ancient debris in the boss chest); since it is descended, `bottom_floor` is its boss chest.

The golem itself now drops diamonds, an iron block, redstone and a small chance of XP bottles,
emeralds or a diamond block — worth killing, without upstaging the chest.

### Modded loot, the way RLCraft did it

RLCraft doesn't fill its towers with vanilla items — its floors are full of Spartan Weaponry stone
weapons low down, Ice and Fire silver armour in the middle, baubles and rings higher up, and xp books
plus 1% jackpots at the top. That layer is reproduced here with the **1.21.1 equivalents**, with every
id read out of the installed jars rather than assumed — note the Spartan namespace on 1.21.1 is
`spartan_weaponry_unofficial`, not RLCraft's `spartanweaponry`.

| Mod | Where it lands | RLCraft's original |
|---|---|---|
| Spartan Weaponry | stone melee on 1–2, stone thrown on 3–4, enchanted iron on 5–6, enchanted diamond on 7–8 and the boss | stone weapons at ~2% each on floors 1–2 |
| Ice and Fire | silver armour at 5% on 3–4 and 10% on 5–6; dragonbone and sea serpent parts higher; enchanted dragonbone at the boss | `armor_silver_metal_*` at 5–10% |
| Trinkets and Baubles | rings and stones from 5–6 upward | `baubles:ring` at ~10% |
| Quality Equipment | emerald ring and amulet on 7–8 and the boss | `qualitytools:emerald_ring` / `_amulet` at 10% |
| XP Tome | xp book at 6% on 7–8, book and tome at the boss | `xpbook:xp_book` at 3% |
| Bountiful Baubles | the boss jackpot slot | `bountifulbaubles:spectralsilt` at 1% |

**None of this makes the mod depend on those mods.** Each modded table carries a whole-file
`"neoforge:conditions"` `mod_loaded` gate, so when a mod is absent NeoForge skips the file entirely
instead of failing to parse an unknown item, and a `neoforge:add_table` global loot modifier — gated
the same way — injects it into the matching chests. Verified both ways: with the mods absent a
dedicated server boots with **zero errors**, and with XP Tome installed a single roll of
`battletowers:chests/top_floor` returned an xpbook item, which can only arrive through the modifier.

Everything stays **vanilla-only**, so the mod still has no dependencies. All 83 item ids and both
nested table references were checked against the 1.21.1 registry, and the tables were rolled in game
with `/loot give` to confirm the curve: two floor-1–2 chests plus a floor-5–6 produced supplies and a
plain bow, while the top-floor chest produced an enchanted bow, enchanted diamond boots and an
enchanted golden apple.

## Building

```bash
# JDK 21 required
./gradlew build      # -> build/libs/battletowers-1.2.1+mc1.21.1.jar
```

## Credits

Classic Battle Towers is by **_blackdog**, **N1ck1903** and **Bozmund** —
[GitHub](https://github.com/blxckdog/ClassicBattleTowers) ·
[Modrinth](https://modrinth.com/mod/classic-battle-towers). It revives Kodaichi's and
AtomicStryker's original Battle Tower mods. This port is unofficial and unendorsed.
