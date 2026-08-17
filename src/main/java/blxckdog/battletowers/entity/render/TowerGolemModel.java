package blxckdog.battletowers.entity.render;

import blxckdog.battletowers.entity.TowerGolemEntity;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import com.mojang.blaze3d.vertex.PoseStack;

public class TowerGolemModel extends HumanoidModel<TowerGolemEntity> {

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition modelData = createMesh(CubeDeformation.NONE, 0);
		return LayerDefinition.create(modelData, 64, 32);
	}
	
	public TowerGolemModel(ModelPart root) {
		super(root);
	}

	@Override
	public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		matrices.scale(2f, 2f, 2f);
		matrices.translate(0f, -0.75f, 0f);

		super.renderToBuffer(matrices, vertices, light, overlay, color);
	}

}
