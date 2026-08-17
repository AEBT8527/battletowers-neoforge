package blxckdog.battletowers;

import java.util.List;
import java.util.Set;

import blxckdog.battletowers.entity.TowerGolemEntity;
import blxckdog.battletowers.entity.TowerGolemFireballEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * NeoForge entry point, replacing Fabric's ModInitializer.
 *
 * Registration moves from immediate Registry.register(...) calls in static initialisers to
 * DeferredRegister: NeoForge freezes the vanilla registries outside its own registration phase, so
 * the Fabric style would throw. The two gameplay hooks move to game-bus events, and the attribute
 * registration to the mod-bus EntityAttributeCreationEvent.
 */
@Mod(ClassicBattleTowers.MOD_ID)
public class ClassicBattleTowers {

    public static final String MOD_ID = "battletowers";

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    /*
     * Entities
     */
    public static final DeferredHolder<EntityType<?>, EntityType<TowerGolemEntity>> BATTLE_TOWER_GOLEM_HOLDER =
            ENTITY_TYPES.register("battle_tower_golem", () -> EntityType.Builder
                    .of(TowerGolemEntity::new, MobCategory.MONSTER)
                    .sized(1.7f, 4f)
                    .build("battle_tower_golem"));

    public static final DeferredHolder<EntityType<?>, EntityType<TowerGolemFireballEntity>> BATTLE_TOWER_GOLEM_FIREBALL_HOLDER =
            ENTITY_TYPES.register("battle_tower_golem_fireball", () -> EntityType.Builder
                    .<TowerGolemFireballEntity>of(TowerGolemFireballEntity::new, MobCategory.MISC)
                    .sized(.4f, .4f)
                    .build("battle_tower_golem_fireball"));

    /*
     * Sounds
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_GOLEM_AMBIENT_HOLDER = sound("golem_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_GOLEM_AWAKEN_HOLDER = sound("golem_awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_GOLEM_DEATH_HOLDER = sound("golem_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_GOLEM_HURT_HOLDER = sound("golem_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_GOLEM_SPECIAL_HOLDER = sound("golem_special");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_GOLEM_CHARGE_HOLDER = sound("golem_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_TOWER_CRUMBLE_HOLDER = sound("tower_crumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_TOWER_BREAK_START_HOLDER = sound("tower_break_start");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String path) {
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id(path)));
    }

    /*
     * The rest of the mod refers to these as plain values, exactly as it did on Fabric, so the
     * accessors below keep those call sites unchanged.
     */
    public static EntityType<TowerGolemEntity> battleTowerGolem() {
        return BATTLE_TOWER_GOLEM_HOLDER.get();
    }

    public static EntityType<TowerGolemFireballEntity> battleTowerGolemFireball() {
        return BATTLE_TOWER_GOLEM_FIREBALL_HOLDER.get();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public ClassicBattleTowers(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        SOUND_EVENTS.register(modBus);
        modBus.addListener(ClassicBattleTowers::registerAttributes);
    }

    /** Fabric: FabricDefaultAttributeRegistry.register(type, builder). */
    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(battleTowerGolem(), TowerGolemEntity.createTowerGolemAttributes().build());
    }

    @EventBusSubscriber(modid = MOD_ID)
    public static final class GameEvents {
        private GameEvents() {}

        /**
         * Fabric: ServerEntityEvents.ENTITY_LOAD. NeoForge's nearest equivalent fires for both
         * sides and for entities read back from disk, so this guards on !level.isClientSide and
         * lets the marker's own tags do the rest, exactly as upstream did.
         */
        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide()) {
                return;
            }
            if (!(event.getEntity() instanceof Marker marker)) {
                return;
            }

            Set<String> entityTags = marker.getTags();

            if (!entityTags.contains("battletowers.summon.default_golem")
                    && !entityTags.contains("battletowers.summon.default_golem_underground")) {
                return;
            }

            TowerGolemEntity battleTowerGolem =
                    new TowerGolemEntity(battleTowerGolem(), marker.level());
            battleTowerGolem.setPos(marker.position());
            battleTowerGolem.setTowerPosition(marker.blockPosition());

            if (entityTags.contains("battletowers.summon.default_golem_underground")) {
                battleTowerGolem.setTowerUnderground(true);
            }

            // Replace marker with Battle Tower Golem
            marker.level().addFreshEntity(battleTowerGolem);
            marker.remove(RemovalReason.DISCARDED);
        }

        /** Fabric: UseBlockCallback — listen for chest or hopper use to wake up the golem. */
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (event.getLevel().isClientSide()) {
                return;
            }

            BlockPos pos = event.getPos();
            BlockState state = event.getLevel().getBlockState(pos);

            if (!state.is(Blocks.CHEST) && !state.is(Blocks.HOPPER)) {
                return;
            }

            List<TowerGolemEntity> nearbyGolems = event.getLevel().getEntitiesOfClass(
                    TowerGolemEntity.class,
                    new AABB(pos).inflate(10),
                    golem -> true);

            if (!nearbyGolems.isEmpty()) {
                nearbyGolems.forEach(golem -> {
                    golem.wakeUpGolem();
                    golem.setTarget(event.getEntity());
                });

                // Prevent the chest from being opened
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
            }
        }
    }
}
