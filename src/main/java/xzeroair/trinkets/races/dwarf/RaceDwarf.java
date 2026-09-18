package xzeroair.trinkets.races.dwarf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.abilities.AbilitySkilledMiner;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.RaceConfig;
import xzeroair.trinkets.util.config.server.race.DwarfAbilitiesConfig;

/**
 * 矮人种族的行为处理器。
 *
 * 移植说明：1.12 的同名类里，「效果增删 / 药水免疫 / 坐骑黑白名单」三段逻辑与其余 8 族逐字相同，
 * 已上移至 EntityRacePropertiesHandler，本类只需交出配置与本族独有内容。
 */
public class RaceDwarf extends EntityRacePropertiesHandler {

    public RaceDwarf(@Nullable LivingEntity entity, @Nullable EntityProperties properties, @Nonnull RaceCache cache) {
        super(entity, properties, cache);
    }

    @Override
    public RaceConfig<DwarfAbilitiesConfig> getConfig() {
        return TrinketsConfig.SERVER.races.dwarf;
    }

    @Override
    public void registerRaceAbilities() {
        final DwarfAbilitiesConfig abilities = this.getConfig().abilities;
        if (abilities == null) {
            return;
        }
        this.addAbility(new AbilitySkilledMiner(abilities.skilledMiner));
    }
}
