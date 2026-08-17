package blxckdog.battletowers.entity.ai;

import java.util.EnumSet;

import blxckdog.battletowers.entity.TowerGolemEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class NotDormantGoalProxy extends Goal {

	private final Goal wrapped;
	private final TowerGolemEntity towerGolem;
	
	public NotDormantGoalProxy(TowerGolemEntity golem, Goal wrapped) {
		this.towerGolem = golem;
		this.wrapped = wrapped;
	}
	
	@Override
	public boolean canUse() {
		return towerGolem.isDormant() ? false : wrapped.canUse();
	}
	
	@Override
	public boolean canContinueToUse() {
		return towerGolem.isDormant() ? false : wrapped.canContinueToUse();
	}
	
	@Override
	public boolean isInterruptable() {
		return wrapped.isInterruptable();
	}
	
	@Override
	public EnumSet<Goal.Flag> getFlags() {
		return wrapped.getFlags();
	}
	
	@Override
	public void setFlags(EnumSet<Goal.Flag> controls) {
		wrapped.setFlags(controls);
	}
	
	@Override
	public boolean requiresUpdateEveryTick() {
		return wrapped.requiresUpdateEveryTick();
	}
	
	@Override
	public void start() {
		wrapped.start();
	}
	
	@Override
	public void stop() {
		wrapped.stop();
	}
	
	@Override
	public void tick() {
		wrapped.tick();
	}
	
	@Override
	public String toString() {
		return wrapped.toString();
	}
	
	@Override
	public int hashCode() {
		return wrapped.hashCode();
	}

}
