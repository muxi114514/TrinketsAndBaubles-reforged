package xzeroair.trinkets.entity.ai;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;

/**
 * 末影骑士：攻击女王的仇人或女王正在攻击的目标，距离较远时先瞬移过去。对应 1.12 EnderQueensKnightAI。
 */
public class EnderQueenKnightGoal extends TargetGoal {

    private static final TargetingConditions CONDITIONS = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

    private final EnderMan knight;
    private final EnderQueens.Locator locator;
    @Nullable
    private Player queen;
    @Nullable
    private LivingEntity attacker;
    private int revengeTimestamp;
    private int attackTimestamp;
    private boolean fromRevenge;

    public EnderQueenKnightGoal(EnderMan knight) {
        super(knight, false);
        this.knight = knight;
        this.locator = new EnderQueens.Locator(knight);
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        this.queen = this.locator.locate();
        if (this.queen == null) {
            return false;
        }
        final LivingEntity revenge = this.queen.getLastHurtByMob();
        if (this.queen.getLastHurtByMobTimestamp() != this.revengeTimestamp && this.isQueenTarget(revenge)) {
            this.attacker = revenge;
            this.fromRevenge = true;
            return true;
        }
        final LivingEntity attacked = this.queen.getLastHurtMob();
        if (this.queen.getLastHurtMobTimestamp() != this.attackTimestamp && this.isQueenTarget(attacked)) {
            this.attacker = attacked;
            this.fromRevenge = false;
            return true;
        }
        return false;
    }

    private boolean isQueenTarget(@Nullable LivingEntity target) {
        if (target == null || target == this.queen || !this.canAttack(target, CONDITIONS)) {
            return false;
        }
        return !(target instanceof Player player) || this.queen.canHarmPlayer(player);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.attacker);
        if (this.queen != null) {
            if (this.fromRevenge) {
                this.revengeTimestamp = this.queen.getLastHurtByMobTimestamp();
            } else {
                this.attackTimestamp = this.queen.getLastHurtMobTimestamp();
            }
        }
        if (this.attacker != null && this.knight.distanceToSqr(this.attacker) > 6.0D) {
            EnderQueens.teleportTowards(this.knight, this.attacker);
        }
        super.start();
    }
}
