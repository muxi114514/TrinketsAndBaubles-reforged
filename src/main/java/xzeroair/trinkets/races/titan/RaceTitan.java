package xzeroair.trinkets.races.titan;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.abilities.other.AbilityHeavy;
import xzeroair.trinkets.traits.abilities.other.AbilityLargeHands;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.RaceConfig;
import xzeroair.trinkets.util.config.server.race.TitanAbilitiesConfig;

/**
 * 泰坦种族的行为处理器。
 *
 * 移植说明：「效果增删 / 药水免疫 / 坐骑黑白名单」三段与其余 8 族相同的逻辑已上移至基类。
 */
public class RaceTitan extends EntityRacePropertiesHandler {

    public RaceTitan(@Nullable LivingEntity entity, @Nullable EntityProperties properties, @Nonnull RaceCache cache) {
        super(entity, properties, cache);
    }

    @Override
    public RaceConfig<TitanAbilitiesConfig> getConfig() {
        return TrinketsConfig.SERVER.races.titan;
    }

    @Override
    public void registerRaceAbilities() {
        final TitanAbilitiesConfig abilities = this.getConfig().abilities;
        if (abilities == null) {
            return;
        }
        this.addAbility(new AbilityLargeHands(abilities.largeHands));
        this.addAbility(new AbilityHeavy(abilities.heavy));
    }
}
