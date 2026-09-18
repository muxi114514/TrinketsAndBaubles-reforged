package xzeroair.trinkets.capabilities;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.trinket.TrinketProperties;

/**
 * 能力令牌持有者。
 * 移植说明：1.12 用 @CapabilityInject 注入静态字段，1.20.1 改为 CapabilityManager.get(CapabilityToken)，
 * 并须在 RegisterCapabilitiesEvent（mod 事件总线）中显式注册实现类。
 */
public class Capabilities {

    public static final Capability<EntityProperties> ENTITY_PROPERTIES =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    public static final Capability<MagicStats> MAGIC_STATS =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    /** 饰品栈的能力实例缓存（不序列化，持久数据在物品 NBT） */
    public static final Capability<TrinketProperties> TRINKET_PROPERTIES =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    // ── 随移植分期补入：方块实体属性 ──
    // 不移植：VIP_STATUS（vip 包）、饰品栏容器（由 Curios 承担）

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(EntityProperties.class);
        event.register(MagicStats.class);
        event.register(TrinketProperties.class);
    }

    private Capabilities() {
    }
}
