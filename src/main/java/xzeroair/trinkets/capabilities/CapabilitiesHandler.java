package xzeroair.trinkets.capabilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.util.Reference;

/**
 * 能力附加处理器（Forge 事件总线）。
 *
 * 移植说明：1.12 原版虽先判 EntityLivingBase，但实际只在 isPlayer 分支内挂载，
 * 故这里直接以 Player 为条件；原版的 isEntityBoss 排除因此成为冗余判断，一并省去。
 * 1.12 的 hasCapability 判重在 1.20.1 改为 getCapability(...).isPresent()。
 */
public class CapabilitiesHandler {

    private static final ResourceLocation RACE = new ResourceLocation(Reference.MODID, "race");
    private static final ResourceLocation MAGIC = new ResourceLocation(Reference.MODID, "magic");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onAttachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        final Entity entity = event.getObject();
        if (!(entity instanceof Player player) || entity.level() == null) {
            return;
        }
        if (!entity.getCapability(Capabilities.ENTITY_PROPERTIES).isPresent()) {
            final CapabilityProviderBase<EntityProperties> provider =
                    new CapabilityProviderBase<>(Capabilities.ENTITY_PROPERTIES, new EntityProperties(player));
            event.addCapability(RACE, provider);
            event.addListener(provider::invalidate);
        }
        if (!entity.getCapability(Capabilities.MAGIC_STATS).isPresent()) {
            final CapabilityProviderBase<MagicStats> provider =
                    new CapabilityProviderBase<>(Capabilities.MAGIC_STATS, new MagicStats(player));
            event.addCapability(MAGIC, provider);
            event.addListener(provider::invalidate);
        }
    }

    // ── 随移植分期补入：方块实体（月光玫瑰 / 泰迪熊）与物品栈（饰品数据）的附加 ──
}
