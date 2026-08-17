package blxckdog.battletowers.entity;

import blxckdog.battletowers.ClassicBattleTowers;
import blxckdog.battletowers.entity.ai.NotDormantGoalProxy;
import blxckdog.battletowers.entity.ai.TowerGolemShootGoal;
import blxckdog.battletowers.entity.ai.TowerGolemSleepGoal;
import blxckdog.battletowers.entity.ai.TowerGolemStompGoal;
import blxckdog.battletowers.world.BattleTowerDestructionManager;
import blxckdog.battletowers.world.BattleTowerDestructionTask;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Objects;

public class TowerGolemEntity extends Monster implements RangedAttackMob {

    protected static final EntityDataAccessor<BlockPos> TOWER_POS;
    protected static final EntityDataAccessor<Boolean> DORMANT;
    protected static final EntityDataAccessor<Boolean> UNDERGROUND;

    static {
        TOWER_POS = SynchedEntityData.defineId(TowerGolemEntity.class, EntityDataSerializers.BLOCK_POS);
        DORMANT = SynchedEntityData.defineId(TowerGolemEntity.class, EntityDataSerializers.BOOLEAN);
        UNDERGROUND = SynchedEntityData.defineId(TowerGolemEntity.class, EntityDataSerializers.BOOLEAN);
    }

    public static AttributeSupplier.Builder createTowerGolemAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.ATTACK_KNOCKBACK, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 16)
                .add(Attributes.ARMOR, 1)
                .add(Attributes.ARMOR_TOUGHNESS, 2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }


    public TowerGolemEntity(EntityType<TowerGolemEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TOWER_POS, BlockPos.ZERO);
        builder.define(UNDERGROUND, false);
        builder.define(DORMANT, true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));

        // Attacking goals
        goalSelector.addGoal(1, whenAwake(new MeleeAttackGoal(this, 1d, true)));
        goalSelector.addGoal(2, whenAwake(new TowerGolemShootGoal(this, 20 * 3, 20f)));
        goalSelector.addGoal(3, whenAwake(new TowerGolemStompGoal(this, 20 * 5, 6f)));

        // Ambient goals
        goalSelector.addGoal(4, whenAwake(new LookAtPlayerGoal(this, Player.class, 12f)));
        goalSelector.addGoal(5, whenAwake(new TowerGolemSleepGoal(this, 20 * 15)));

        // Targeting
        targetSelector.addGoal(1, whenAwake(new HurtByTargetGoal(this)));
        targetSelector.addGoal(2, whenAwake(new NearestAttackableTargetGoal<>(this, Player.class, false)));
    }

    private Goal whenAwake(Goal goal) {
        return new NotDormantGoalProxy(this, goal);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);

        if (!level().isClientSide && getTowerPosition() != BlockPos.ZERO) {
            // Destroy Tower
            BattleTowerDestructionManager.registerTask(new BattleTowerDestructionTask(level(), getTowerPosition(), isTowerUnderground()));

            Component deathText = Component.translatable("notify.battletowers.golem_defeated");
            Objects.requireNonNull(getServer()).getPlayerList().broadcastSystemMessage(deathText, false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() != null) {
            Entity attacker = source.getDirectEntity();

            if (!source.isDirect()) {
                attacker = source.getEntity();
            }

            if (attacker instanceof LivingEntity) {
                // Wake up Battle Tower Golem when player attacks from further away
                setTarget((LivingEntity) attacker);
                wakeUpGolem();
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        double srcX = getX();
        double srcY = getEyeY();
        double srcZ = getZ();

        double dstX = target.getX() - srcX;
        double dstY = target.getY() + (target.getEyeHeight() * .5) - srcY;
        double dstZ = target.getZ() - srcZ;

        TowerGolemFireballEntity fireball = new TowerGolemFireballEntity(level(), this, dstX, dstY, dstZ, power);
        fireball.setPos(srcX, srcY, srcZ);
        fireball.setOwner(this);

        level().playSound(this, blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, getSoundVolume(), 1f);
        level().addFreshEntity(fireball);
    }

    @Override
    public void tick() {
        super.tick();

        // Check for victims within 6 blocks
        if (isDormant()) {
            Player player = level().getNearestPlayer(getX(), getY(), getZ(), 6d, true);

            if (player != null && this.hasLineOfSight(player)) {
                setTarget(player);
                wakeUpGolem();
            }
        }
    }


    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public int getMaxFallDistance() {
        return 999;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }


    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ClassicBattleTowers.SOUND_GOLEM_HURT_HOLDER.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ClassicBattleTowers.SOUND_GOLEM_DEATH_HOLDER.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ClassicBattleTowers.SOUND_GOLEM_AMBIENT_HOLDER.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 20 * 20;
    }


    public void wakeUpGolem() {
        if (isDormant()) {
            level().playSound(this, blockPosition(), ClassicBattleTowers.SOUND_GOLEM_AWAKEN_HOLDER.get(), SoundSource.HOSTILE, 2f, 1f);
        }

        this.entityData.set(DORMANT, false);
    }

    public void setDormant(boolean value) {
        this.entityData.set(DORMANT, value);
    }

    public boolean isDormant() {
        return this.entityData.get(DORMANT);
    }

    public BlockPos getTowerPosition() {
        return this.entityData.get(TOWER_POS);
    }

    public void setTowerPosition(BlockPos towerPos) {
        this.entityData.set(TOWER_POS, towerPos);
    }

    public void setTowerUnderground(boolean value) {
        this.entityData.set(UNDERGROUND, value);
    }

    public boolean isTowerUnderground() {
        return this.entityData.get(UNDERGROUND);
    }


    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if (nbt.contains("TowerX") && nbt.contains("TowerY") && nbt.contains("TowerZ")) {
            int spawnX = nbt.getInt("TowerX");
            int spawnY = nbt.getInt("TowerY");
            int spawnZ = nbt.getInt("TowerZ");

            setTowerPosition(new BlockPos(spawnX, spawnY, spawnZ));
        }

        if(nbt.contains("TowerUnderground")) {
            setTowerUnderground(nbt.getBoolean("TowerUnderground"));
        }

        if (nbt.contains("Dormant")) {
            setDormant(nbt.getBoolean("Dormant"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("Dormant", isDormant());
        nbt.putBoolean("TowerUnderground", isTowerUnderground());

        BlockPos spawnPos = getTowerPosition();
        nbt.putInt("TowerX", spawnPos.getX());
        nbt.putInt("TowerY", spawnPos.getY());
        nbt.putInt("TowerZ", spawnPos.getZ());
    }

}
