package blxckdog.battletowers.entity.render;

import static blxckdog.battletowers.ClassicBattleTowers.id;

import blxckdog.battletowers.ClassicBattleTowersClient;
import blxckdog.battletowers.entity.TowerGolemEntity;

import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class TowerGolemRenderer extends HumanoidMobRenderer<TowerGolemEntity, TowerGolemModel>{

	private static final ResourceLocation TEXTURE_DORMANT = id("textures/model/tower_golem_dormant.png");
	private static final ResourceLocation TEXTURE_AWAKE = id("textures/model/tower_golem.png");
	
	
	public TowerGolemRenderer(Context context) {
		super(context, new TowerGolemModel(context.bakeLayer(ClassicBattleTowersClient.MODEL_TOWER_GOLEM_LAYER)), 0.95f);
	}

	@Override
	public ResourceLocation getTextureLocation(TowerGolemEntity golem) {
		return golem.isDormant() ? TEXTURE_DORMANT : TEXTURE_AWAKE;
	}

}
