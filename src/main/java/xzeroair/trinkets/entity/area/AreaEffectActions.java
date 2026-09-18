package xzeroair.trinkets.entity.area;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.entity.area.action.DamageAreaAction;
import xzeroair.trinkets.entity.area.action.FreezeLiquidAreaAction;
import xzeroair.trinkets.entity.area.action.GrowBlockAreaAction;
import xzeroair.trinkets.entity.area.action.PlaceFireAreaAction;
import xzeroair.trinkets.entity.area.action.PlaceSnowAreaAction;
import xzeroair.trinkets.entity.area.action.PotionAreaAction;
import xzeroair.trinkets.entity.area.action.RepairItemAreaAction;

/**
 * 区域效果动作的读档注册表：类型名 → 从 NBT 重建动作的工厂。
 *
 * 移植说明：1.12 用 instanceof 链写档、switch 读档，新增动作要改实体本身；
 * 这里改为登记表，附属模组调用 {@link #register} 即可让自定义动作随实体存读档。
 */
public final class AreaEffectActions {

    private static final String TYPE_TAG = "Type";
    private static final Map<String, Function<CompoundTag, AreaEffectAction>> FACTORIES = new ConcurrentHashMap<>();

    static {
        register(PotionAreaAction.TYPE, PotionAreaAction::load);
        register(DamageAreaAction.TYPE, DamageAreaAction::load);
        register(RepairItemAreaAction.TYPE, RepairItemAreaAction::load);
        register(GrowBlockAreaAction.TYPE, GrowBlockAreaAction::load);
        register(PlaceFireAreaAction.TYPE, PlaceFireAreaAction::load);
        register(FreezeLiquidAreaAction.TYPE, FreezeLiquidAreaAction::load);
        register(PlaceSnowAreaAction.TYPE, tag -> new PlaceSnowAreaAction());
    }

    public static void register(String type, Function<CompoundTag, AreaEffectAction> factory) {
        FACTORIES.put(type, factory);
    }

    public static CompoundTag write(AreaEffectAction action) {
        final CompoundTag tag = new CompoundTag();
        tag.putString(TYPE_TAG, action.type());
        action.save(tag);
        return tag;
    }

    @Nullable
    public static AreaEffectAction read(CompoundTag tag) {
        final Function<CompoundTag, AreaEffectAction> factory = FACTORIES.get(tag.getString(TYPE_TAG));
        if (factory == null) {
            return null;
        }
        try {
            return factory.apply(tag);
        } catch (RuntimeException e) {
            Trinkets.LOGGER.error("Failed to load area effect action {}", tag, e);
            return null;
        }
    }

    private AreaEffectActions() {
    }
}
