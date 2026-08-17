package blxckdog.battletowers;

import static blxckdog.battletowers.ClassicBattleTowers.id;

import blxckdog.battletowers.entity.render.TowerGolemModel;
import blxckdog.battletowers.entity.render.TowerGolemRenderer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * What used to be Fabric's ClientModInitializer.
 *
 * Fabric's EntityRendererRegistry / EntityModelLayerRegistry become the two mod-bus events
 * NeoForge fires for the same purpose. @EventBusSubscriber with Dist.CLIENT replaces @Environment,
 * so nothing here is loaded on a dedicated server.
 */
@EventBusSubscriber(modid = ClassicBattleTowers.MOD_ID, value = Dist.CLIENT)
public final class ClassicBattleTowersClient {

    public static final ModelLayerLocation MODEL_TOWER_GOLEM_LAYER =
            new ModelLayerLocation(id("tower_golem"), "main");

    private ClassicBattleTowersClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register rendering for Battle Tower Golem
        event.registerEntityRenderer(ClassicBattleTowers.battleTowerGolem(), TowerGolemRenderer::new);

        // Register rendering for Battle Tower Golem Fireball projectile
        event.registerEntityRenderer(ClassicBattleTowers.battleTowerGolemFireball(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MODEL_TOWER_GOLEM_LAYER, TowerGolemModel::getTexturedModelData);
    }
}
