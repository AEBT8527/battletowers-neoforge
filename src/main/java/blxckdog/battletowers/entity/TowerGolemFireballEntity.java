package blxckdog.battletowers.entity;

import blxckdog.battletowers.ClassicBattleTowers;

import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;

public class TowerGolemFireballEntity extends Fireball {

	private final float explosionPower;
	
	
	public TowerGolemFireballEntity(EntityType<TowerGolemFireballEntity> type, Level world) {
		super(type, world);
		explosionPower = 1f;
	}
	
	protected TowerGolemFireballEntity(Level world, LivingEntity owner, double velocityX, double velocityY, double velocityZ, float explosionPower) {
		super(ClassicBattleTowers.battleTowerGolemFireball(), owner, new Vec3(velocityX, velocityY, velocityZ), world);
		this.explosionPower = explosionPower;
	}
	
	
	@Override
	public void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);

		if (level().isClientSide) {
			return;
		}
		
		Entity hitEntity = entityHitResult.getEntity();
		Entity thrower = this.getOwner();
		
		BlockPos targetPos = hitEntity.blockPosition();
		createExplosion(targetPos);
	}
	
	@Override
	public void onHitBlock(BlockHitResult blockHitResult) {
		super.onHitBlock(blockHitResult);
		
		if (level().isClientSide) {
			return;
		}
		
		BlockPos targetPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
		createExplosion(targetPos);
		
		if (level().isEmptyBlock(targetPos)) {
			level().setBlockAndUpdate(targetPos, BaseFireBlock.getState(level(), targetPos));
		}
	}
	
	private void createExplosion(BlockPos pos) {
		Entity thrower = this.getOwner();
		
		if (!(thrower instanceof Mob) || level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
			level().explode(thrower, pos.getX(), pos.getY(), pos.getZ(), explosionPower, ExplosionInteraction.MOB);
		}
	}
	
	
	public void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		
		if (!level().isClientSide) {
			this.discard();
		}

	}

	public boolean canHit() {
		return false;
	}

	public boolean hurt(DamageSource source, float amount) {
		return false;
	}
	
}
