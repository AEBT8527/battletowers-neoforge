package blxckdog.battletowers.world;

import blxckdog.battletowers.ClassicBattleTowers;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;

public class BattleTowerDestructionTask implements Runnable {

    private static final int INITIAL_DELAY = 20 * 15;
    private static final int PER_FLOOR_DELAY = 20 * 5;
    private static final int MAX_FLOORS = 5;

    private final Level world;
    private final BlockPos startPos;
    private final boolean underground;

    private int tickCounter = 0;
    private int floorCounter = 0;


    public BattleTowerDestructionTask(Level world, BlockPos startPos, boolean underground) {
        this.startPos = startPos;
        this.world = world;
        this.underground = underground;

        world.playSound(null, startPos, ClassicBattleTowers.SOUND_TOWER_BREAK_START_HOLDER.get(), SoundSource.HOSTILE, 4f, 1f);
    }


    @Override
    public void run() {
        int centerX = startPos.getX();
        int centerZ = startPos.getZ();
        tickCounter++;

        if (tickCounter % (INITIAL_DELAY + floorCounter * PER_FLOOR_DELAY) == 0) {
            if (!world.isClientSide) {
            	explodeCircular(centerX, centerZ, 4, 8);
                if(!underground) removeFlyingBlocks();
            }

            floorCounter++;
        }

        if (tickCounter % (INITIAL_DELAY + floorCounter * PER_FLOOR_DELAY + 10) == 0) {
            world.playSound(null, centerX, getAdjustedY(), centerZ, ClassicBattleTowers.SOUND_TOWER_CRUMBLE_HOLDER.get(), SoundSource.HOSTILE, 4f, 1f);
        }
    }

    private void explodeCircular(int centerX, int centerZ, int towerRadius, int explosionCount) {
        float angleIncrement = 360 / explosionCount; // The amount to increment the angle each time (this will determine the "tightness" of the spiral)
        
        for(int i=0; i < explosionCount; i++) {
        	float angle = angleIncrement * i;
        	int x = (int) (towerRadius * Math.cos(angle));
            int z = (int) (towerRadius * Math.sin(angle));
            
            world.explode(null, centerX+x, getAdjustedY()-2, centerZ+z, 4f, ExplosionInteraction.MOB);
        }
    }


    private void removeFlyingBlocks() {
        //int yOffset = underground ? floorCounter * 7 : -floorCounter * 7;
        int yOffset = -floorCounter * 7;

        for (int x = -8; x < 8; x++) {
            for (int z = -8; z < 8; z++) {
                for (int y = 1; y < 9; y++) {
                    BlockPos pos = startPos.offset(x, yOffset + y, z);

                    if (!world.isEmptyBlock(pos)) {
                        world.removeBlock(pos, false);
                    }
                }
            }
        }
    }


    private int getAdjustedY() {
        return underground ? startPos.getY() + floorCounter * 7 : startPos.getY() - floorCounter * 7;
    }


    public boolean isInWorld(Level world) {
        return this.world.dimension().location() == world.dimension().location();
    }

    public boolean isFinished() {
        return tickCounter > INITIAL_DELAY + MAX_FLOORS * PER_FLOOR_DELAY;
    }

}
