package xzeroair.trinkets.traits.abilities;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.interfaces.IMiningAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IPotionAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IToggleAbility;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.config.abilities.FlightAbilityConfig;
import xzeroair.trinkets.util.handlers.Counter;

/**
 * 创造飞行：种族允许飞行时给予飞行许可（双击跳跃起飞），免疫漂浮，空中挖掘不减速。
 * 有魔力消耗时，飞行中每秒扣一次，魔力不足即收回许可并落地。对应 1.12 traits/abilities/AbilityCreativeFlight。
 *
 * 移植说明：
 * - 1.12 capabilities.allowFlying / isFlying / sendPlayerAbilities → 1.20.1 getAbilities().mayfly / flying / onUpdateAbilities。
 * - 挖掘加成「不在地面时 ×5」抵消的是原版空中挖掘 /5 的惩罚，该惩罚 1.20.1 仍在，故保留；
 *   与大手那个补偿 1.12 体型误判的 ×5 不是一回事。
 * - 1.12 通过 sendAbilityData 把消耗下发客户端；1.20.1 的 SERVER 配置自动同步，不再需要。
 * - 1.12 在药水核心的「飞行」效果生效时让位（不扣魔力、不收回许可）；1.20.1 该效果由 Simple Difficulty Reforge 提供。
 */
public class AbilityCreativeFlight extends Ability implements ITickableAbility, IPotionAbility, IMiningAbility, IToggleAbility {

    private static final ResourceLocation FLIGHT_EFFECT = new ResourceLocation(ModCompat.SIMPLE_DIFFICULTY, "flight");

    private static final String FLY_TIMER = "fly_timer";
    private static final int WARMUP_TICKS = 20;

    private final FlightAbilityConfig config;
    private boolean selfAdded;
    private boolean toggled;

    public AbilityCreativeFlight(@Nonnull FlightAbilityConfig config) {
        super(AbilityNames.CREATIVE_FLIGHT);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        final EntityProperties properties = EntityProperties.get(player);
        final boolean canFly = properties == null || properties.getRaceHandler().canFly();
        if (canFly && player.tickCount > WARMUP_TICKS) {
            this.addFlyingAbility(player);
        } else {
            this.removeCreativeFlight(player);
        }
    }

    /** 飞行者免疫漂浮 */
    @Override
    public boolean potionApplied(LivingEntity entity, MobEffectInstance effect, boolean cancel) {
        return effect.getEffect() == MobEffects.LEVITATION || cancel;
    }

    @Override
    public float breakingBlock(LivingEntity entity, BlockState state, BlockPos pos, float originalSpeed, float newSpeed) {
        if (entity.isEyeInFluid(FluidTags.WATER)) {
            return newSpeed;
        }
        final float speed = entity.onGround() ? originalSpeed : originalSpeed * 5F;
        return Math.max(newSpeed, speed);
    }

    private void addFlyingAbility(Player player) {
        if (this.isCreativePlayer(player)) {
            return;
        }
        final float cost = this.config.cost.get().floatValue();
        if (cost <= 0F) {
            this.giveCreativeFlight(player);
            if (hasFlightEffect(player)) {
                return;
            }
            if (this.selfAdded && player.getAbilities().flying) {
                player.fallDistance = 0F;
            }
            return;
        }
        final MagicStats magic = MagicStats.get(player);
        if (magic == null) {
            return;
        }
        if (magic.getMana() < cost) {
            if (this.selfAdded) {
                this.removeCreativeFlight(player);
                this.flyTimer().resetTick();
            }
            return;
        }
        this.giveCreativeFlight(player);
        if (hasFlightEffect(player)) {
            return;
        }
        if (this.selfAdded && player.getAbilities().flying) {
            player.fallDistance = 0F;
            if (!player.isPassenger() && this.flyTimer().Tick()
                    && !player.level().isClientSide && !magic.spendMana(cost)) {
                this.removeCreativeFlight(player);
            }
        }
    }

    /** 飞行由其它模组的飞行效果接管 */
    private static boolean hasFlightEffect(Player player) {
        return ModCompat.simpleDifficulty() && ModCompat.hasEffect(player, FLIGHT_EFFECT);
    }

    private Counter flyTimer() {
        return this.tickHandler.getCounter(FLY_TIMER, 20, true, true, true, true);
    }

    @Override
    public void onAbilityRemoved(LivingEntity entity) {
        if (entity instanceof Player player) {
            this.removeCreativeFlight(player);
        }
        this.tickHandler.removeCounter(FLY_TIMER);
    }

    /** 只收回由本能力给出的许可；创造/旁观模式不动 */
    private void removeCreativeFlight(Player player) {
        if (this.isCreativePlayer(player) || player.level().isClientSide || !player.getAbilities().mayfly || hasFlightEffect(player)) {
            return;
        }
        this.selfAdded = false;
        player.getAbilities().mayfly = false;
        if (player.getAbilities().flying) {
            player.fallDistance = 0F;
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
    }

    /** 客户端也先行置位，双击跳跃能立即起飞；服务端置位后同步 */
    private void giveCreativeFlight(Player player) {
        if (player.getAbilities().mayfly) {
            return;
        }
        this.selfAdded = true;
        player.getAbilities().mayfly = true;
        if (!player.level().isClientSide) {
            player.onUpdateAbilities();
        }
    }

    // ── 开关（1.12 保留了接口但无触发入口）──

    @Override
    public boolean isAbilityToggled() {
        return this.toggled;
    }

    @Override
    public int getToggleMode() {
        return -1;
    }

    @Override
    public IToggleAbility toggleAbility(boolean enabled) {
        if (this.toggled != enabled) {
            this.toggled = enabled;
            this.setChanged(true);
        }
        return this;
    }

    @Override
    public IToggleAbility toggleAbility(int value) {
        return this;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final double cost = this.config.cost.get();
        variables.number("cost", cost > 0, cost)
                .flag("free", cost <= 0)
                .translated("levitation", true, MobEffects.LEVITATION.getDescriptionId());
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.config.cost.get(), ManaCost.Unit.SECOND);
    }

    @Override
    public boolean isActiveAbility() {
        return true;
    }
}
