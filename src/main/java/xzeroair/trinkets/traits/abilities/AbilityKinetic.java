package xzeroair.trinkets.traits.abilities;

import javax.annotation.Nonnull;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IJumpAbility;
import xzeroair.trinkets.util.config.abilities.KineticAbilityConfig;

/**
 * 动能抵消 / 动能削减：按倍率缩放坠落、落体砸伤与撞墙伤害，倍率 0 时直接免疫；有魔力花费时每次生效都要付费。
 *
 * 移植说明：1.12 的 AbilityNullKinetic 与 AbilityReduceKinetic 逻辑逐字相同、只差注册名，
 * 合并为一个类并以两个工厂方法区分（注册名仍与 1.12 一致，NBT 与语言键不受影响）。
 * 1.12 的 FALLING_BLOCK 在 1.20.1 细分出钟乳石，一并计入；铁砧在 1.12 就是独立伤害源，不计入。
 */
public class AbilityKinetic extends Ability implements IAttackAbility, IJumpAbility {

    private final KineticAbilityConfig config;

    protected AbilityKinetic(String name, @Nonnull KineticAbilityConfig config) {
        super(name);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    public static AbilityKinetic nullify(@Nonnull KineticAbilityConfig config) {
        return new AbilityKinetic(AbilityNames.NULLIFY_KINETIC, config);
    }

    public static AbilityKinetic reduce(@Nonnull KineticAbilityConfig config) {
        return new AbilityKinetic(AbilityNames.REDUCE_KINETIC, config);
    }

    @Override
    public boolean fall(LivingEntity entity, float distance, float multiplier, boolean cancel) {
        if (this.amount() <= 0) {
            return this.pay(entity);
        }
        return cancel;
    }

    @Override
    public float fallDamageMultiplier(LivingEntity entity, float multiplier) {
        if (this.amount() != 1F && this.pay(entity)) {
            return this.amount();
        }
        return multiplier;
    }

    @Override
    public float hurt(LivingEntity attacked, DamageSource source, float dmg) {
        if (this.amount() != 1F && isKinetic(source) && this.pay(attacked)) {
            return dmg * this.amount();
        }
        return dmg;
    }

    @Override
    public boolean attacked(LivingEntity attacked, DamageSource source, float dmg, boolean cancel) {
        if (this.amount() <= 0 && isKinetic(source)) {
            return this.pay(attacked);
        }
        return cancel;
    }

    private float amount() {
        return this.config.amount.get().floatValue();
    }

    /** 无花费恒成功；有花费则尝试支付 */
    private boolean pay(LivingEntity entity) {
        final float cost = this.config.cost.get().floatValue();
        if (cost <= 0) {
            return true;
        }
        final MagicStats magic = MagicStats.get(entity);
        return magic == null || magic.spendMana(cost);
    }

    private static boolean isKinetic(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypes.FALLING_BLOCK)
                || source.is(DamageTypes.FALLING_STALACTITE)
                || source.is(DamageTypes.FLY_INTO_WALL);
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final double amount = this.config.amount.get();
        final double cost = this.config.cost.get();
        variables.flag("immune", amount <= 0)
                .number("multiplier", amount > 0 && amount != 1, amount)
                .number("cost", cost > 0, cost);
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.config.cost.get(), ManaCost.Unit.USE);
    }
}
