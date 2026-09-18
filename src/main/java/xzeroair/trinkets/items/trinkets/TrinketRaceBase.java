package xzeroair.trinkets.items.trinkets;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.items.base.AccessoryBase;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.races.IRaceProvider;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.items.AccessoryConfig;

/**
 * 变身戒指：佩戴恰好一枚时变为对应种族（由 EntityProperties#resolveRace 读取），能力与属性由种族处理器提供。
 * 巨龙之戒有火/冰/雷三种元素变种。对应 1.12 TrinketRaceBase。
 *
 * 移植说明：物品构造早于种族注册表填充，只记种族 id，用时查表（同 RaceFood）。
 */
public class TrinketRaceBase extends AccessoryBase implements IRaceProvider {

    private final String raceId;
    private final boolean elementVariants;

    public TrinketRaceBase(String raceId, boolean elementVariants) {
        super("892cfd1f-25c5-44a0-9154-f3b630538c82");
        this.raceId = raceId;
        this.elementVariants = elementVariants;
    }

    @Override
    public EntityRace getRace() {
        final EntityRace race = ModRaces.registry().getValue(new ResourceLocation(Reference.MODID, this.raceId));
        return race != null ? race : ModRaces.NONE.get();
    }

    @Nullable
    @Override
    public AccessoryConfig<?> getAccessoryConfig() {
        return null;
    }

    @Override
    public boolean isAccessoryEnabled() {
        return TrinketsConfig.SERVER.items.isTransformationRingEnabled(this.raceId);
    }

    @Override
    public Element[] getSubElements() {
        return this.elementVariants
                ? new Element[] {ModElements.FIRE.get(), ModElements.ICE.get(), ModElements.LIGHTNING.get()}
                : ModElements.EMPTY;
    }
}
