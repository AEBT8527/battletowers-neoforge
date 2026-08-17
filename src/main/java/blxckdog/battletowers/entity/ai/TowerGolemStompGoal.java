package blxckdog.battletowers.entity.ai;

import blxckdog.battletowers.ClassicBattleTowers;
import blxckdog.battletowers.entity.TowerGolemEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level.ExplosionInteraction;

public class TowerGolemStompGoal extends Goal {

	private final TowerGolemEntity golem;
	private final int intervallTicks;
	private final float minDistance;
	
	private LivingEntity target;
	private boolean doStompAttack = false;
	private int explosionCounter = 0;
	private int rageCounter = 0;
	
	
	public TowerGolemStompGoal(TowerGolemEntity golem, int intervallTicks, float minDistance) {
		this.golem = golem;
		this.intervallTicks = intervallTicks;
		this.minDistance = minDistance;
	}
	
	
	@Override
	public boolean canUse() {
		LivingEntity livingEntity = golem.getTarget();
		
		if (livingEntity != null && livingEntity.isAlive()) {
			target = livingEntity;
			return true;
		} else {
			return false;
		}
	}
	
	public void stop() {
		target = null;
		rageCounter = 0;
		explosionCounter = 0;
		doStompAttack = false;
	}
	
	@Override
	public boolean canContinueToUse() {
		return canUse() || target.isAlive();
	}

	public boolean requiresUpdateEveryTick() {
		return true;
	}
	
	
	@Override
	public void tick() {
		boolean targetNearby = golem.distanceToSqr(target) < minDistance*minDistance;
		
		// Increase rage counter if last successful attack is some time ago???
		rageCounter = (!targetNearby || doStompAttack) ? rageCounter+1 : 0;
		
		// Do stomp attack after interval ticks at distance
		if(rageCounter > intervallTicks && !doStompAttack) {
			golem.setDeltaMovement(golem.getDeltaMovement().add(new Vec3(0, 0.9d, 0)));
			golem.level().playSound(golem, golem.blockPosition(), ClassicBattleTowers.SOUND_GOLEM_SPECIAL_HOLDER.get(), SoundSource.HOSTILE, 4f, 1f);
			doStompAttack = true;
			return;
		}
		
		// Wait max 1 second (20 ticks) or if golem is on ground to create explosion
		if((rageCounter > intervallTicks+20 || golem.onGround()) && doStompAttack) {
			if(golem.getHealth() < golem.getMaxHealth() / 2) {
				golem.setHealth(golem.getHealth() + 20);
			}

			// Make explosions less likely
			if(explosionCounter <= 0) {
				golem.level().explode(golem, golem.getX(), golem.getY(), golem.getZ(), 4f, ExplosionInteraction.MOB);
				explosionCounter = 3;
			}
			doStompAttack = false;
			explosionCounter--;
			rageCounter = 0;
		}
	}

}
