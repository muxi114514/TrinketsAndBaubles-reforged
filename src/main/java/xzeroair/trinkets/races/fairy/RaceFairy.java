package xzeroair.trinkets.races.fairy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.abilities.AbilityClimbing;
import xzeroair.trinkets.traits.abilities.AbilityCreativeFlight;
import xzeroair.trinkets.traits.abilities.AbilityElytraFlight;
import xzeroair.trinkets.traits.abilities.AbilityHealCloud;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.RaceConfig;
import xzeroair.trinkets.util.config.server.race.FairyAbilitiesConfig;

/**
 * 妖精种族的行为处理器。
 *
 * 移植说明：1.12 的同名类里，「效果增删 / 药水免疫 / 坐骑黑白名单」三段逻辑与其余 8 族逐字相同，
 * 已上移至 EntityRacePropertiesHandler，本类只需交出配置与本族独有内容。
 */
public class RaceFairy extends EntityRacePropertiesHandler {

    public RaceFairy(@Nullable LivingEntity entity, @Nullable EntityProperties properties, @Nonnull RaceCache cache) {
        super(entity, properties, cache);
    }

    @Override
    public RaceConfig<FairyAbilitiesConfig> getConfig() {
        return TrinketsConfig.SERVER.races.fairy;
    }

    @Override
    public void registerRaceAbilities() {
        final FairyAbilitiesConfig abilities = this.getConfig().abilities;
        if (abilities == null) {
            return;
        }
        this.addAbility(new AbilityCreativeFlight(abilities.flight));
        this.addAbility(new AbilityElytraFlight(abilities.elytraFlight));
        this.addAbility(new AbilityClimbing(abilities.climbing));
        this.addAbility(new AbilityHealCloud(abilities.restorationField));
    }
}
