package blxckdog.battletowers.entity.ai;

import blxckdog.battletowers.entity.TowerGolemEntity;

import net.minecraft.world.entity.ai.goal.Goal;

public class TowerGolemSleepGoal extends Goal {

	private final TowerGolemEntity golem;
	private final int maxNoTargetTicks;
	
	private int noTargetCounter;
	
	
	public TowerGolemSleepGoal(TowerGolemEntity golem, int maxNoTargetTicks) {
		this.golem = golem;
		this.maxNoTargetTicks = maxNoTargetTicks;
		noTargetCounter = maxNoTargetTicks;
	}
	
	
	@Override
	public boolean canUse() {
		return !golem.isDormant();
	}
	
	@Override
	public void start() {
		noTargetCounter = maxNoTargetTicks;
	}
	
	@Override
	public void tick() {
		if(!golem.isDormant()) {
			// Set dormant after maxNoTargetTicks without victims
			if(golem.getTarget() == null || !golem.getTarget().isAlive()) {
				noTargetCounter--;
				
				if(noTargetCounter < 1) {
					golem.setDormant(true);
				}
			} else {
				noTargetCounter = maxNoTargetTicks;
			}
		}
	}

}
