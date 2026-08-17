package blxckdog.battletowers.world;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import blxckdog.battletowers.ClassicBattleTowers;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Fabric's ServerTickEvents.START_WORLD_TICK becomes NeoForge's LevelTickEvent.Pre.
 *
 * Upstream's callback only ever ran server-side (Fabric's world tick event is server-only), so the
 * client side is filtered out explicitly here to keep the behaviour identical.
 */
@EventBusSubscriber(modid = ClassicBattleTowers.MOD_ID)
public final class BattleTowerDestructionManager {

    private static final List<BattleTowerDestructionTask> DESTRUCTION_TASKS = new LinkedList<>();

    private BattleTowerDestructionManager() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        Iterator<BattleTowerDestructionTask> iter = DESTRUCTION_TASKS.iterator();

        while (iter.hasNext()) {
            BattleTowerDestructionTask task = iter.next();

            if (task.isInWorld(event.getLevel())) {
                task.run();
            }

            if (task.isFinished()) {
                iter.remove();
            }
        }
    }

    public static boolean registerTask(BattleTowerDestructionTask task) {
        return DESTRUCTION_TASKS.add(task);
    }
}
