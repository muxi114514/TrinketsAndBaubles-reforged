package xzeroair.trinkets.races;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;

/**
 * 空处理器：未变身（None 种族）或种族没有注册行为工厂时使用。
 * 全部行为走 IRaceHandler 的默认实现，等价于「什么都不做」。
 */
public class EmptyHandler extends EntityRacePropertiesHandler {

    public EmptyHandler(@Nullable LivingEntity entity, @Nullable EntityProperties properties, @Nonnull RaceCache cache) {
        super(entity, properties, cache);
    }
}
