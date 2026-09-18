package xzeroair.trinkets.traits.abilities.compat;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.compat.survival.SurvivalCompat;
import xzeroair.trinkets.util.config.abilities.ThirstAbsorptionAbilityConfig;

/**
 * 吸水（海洋之石，Thirst Was Taken 联动）：身处水中时每 frequency tick 回复 amount 点水分。对应 1.12 AbilityThirstAbsorption。
 */
public class AbilityWaterAbsorption extends Ability implements ITickableAbility {

    private final ThirstAbsorptionAbilityConfig config;

    public AbilityWaterAbsorption(@Nonnull ThirstAbsorptionAbilityConfig config) {
        super(AbilityNames.ABSORPTION_THIRST);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (!entity.level().isClientSide && entity.isInWater() && entity.tickCount % this.config.frequency.get() == 0) {
            SurvivalCompat.addThirst(entity, this.config.amount.get());
        }
    }

    @Override
    public void describe(DescriptionVariables variables) {
        variables.number("amount", this.config.amount.get())
                .seconds("interval", true, this.config.frequency.get());
    }

    @Override
    public String getCompatModId() {
        return ModCompat.THIRST_WAS_TAKEN;
    }
}
