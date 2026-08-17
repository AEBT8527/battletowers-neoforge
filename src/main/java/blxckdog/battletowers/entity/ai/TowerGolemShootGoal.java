package blxckdog.battletowers.entity.ai;

import blxckdog.battletowers.ClassicBattleTowers;
import blxckdog.battletowers.entity.TowerGolemEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.sounds.SoundSource;

public class TowerGolemShootGoal extends Goal {

	private final TowerGolemEntity golem;
	private final float maxShootRange;
	private final int intervalTicks;
	
	private LivingEntity target;
	private int updateCountdownTicks = 0;
	
	
	public TowerGolemShootGoal(TowerGolemEntity golem, int intervalTicks, float maxShootRange) {
		this.golem = golem;
		this.maxShootRange = maxShootRange;
		this.intervalTicks = intervalTicks;
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
		updateCountdownTicks = 0;
	}
	
	@Override
	public boolean canContinueToUse() {
		return canUse() || target.isAlive();
	}

	public boolean requiresUpdateEveryTick() {
		return true;
	}

	public void tick() {
		double delta = golem.distanceToSqr(target);
		boolean visible = golem.getSensing().hasLineOfSight(target);
		
		if(--updateCountdownTicks == 10) {
			if(delta - maxShootRange*maxShootRange <= 0) {
				golem.level().playSound(golem, golem.blockPosition(), ClassicBattleTowers.SOUND_GOLEM_CHARGE_HOLDER.get(), SoundSource.HOSTILE, 1f, 1f);
			}
		} else if (updateCountdownTicks == 0) {
			if(delta - maxShootRange*maxShootRange <= 0) {
				golem.performRangedAttack(target, visible ? 1f : 1.5f);
			}
			
			updateCountdownTicks = intervalTicks;
		} else if (updateCountdownTicks < 0) {
			updateCountdownTicks = intervalTicks;
		}

	}
	
}
