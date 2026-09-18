package xzeroair.trinkets.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.client.gui.ScreenOpener;
import xzeroair.trinkets.client.keybinds.ClientAbilityInput;
import xzeroair.trinkets.client.particles.ClientEffects;
import xzeroair.trinkets.network.AbilityCacheSyncPacket;
import xzeroair.trinkets.network.AbilityKeyReleasePacket;
import xzeroair.trinkets.network.EffectsRenderPacket;
import xzeroair.trinkets.network.OpenRaceSelectionPacket;
import xzeroair.trinkets.network.StatusMessagePacket;
import xzeroair.trinkets.network.SyncManaCostToHudPacket;
import xzeroair.trinkets.network.SyncManaStatsPacket;
import xzeroair.trinkets.network.SyncRaceDataPacket;
import xzeroair.trinkets.traits.AbilityHandler;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;

/**
 * 客户端侧的数据包处理。
 * 单独成类以隔离 Minecraft 客户端类的引用，避免专用服务器加载到。
 */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void handleSyncRaceData(SyncRaceDataPacket packet) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || packet.getTag() == null) {
            return;
        }
        final Entity entity = mc.level.getEntity(packet.getEntityID());
        if (entity instanceof LivingEntity living) {
            final EntityProperties properties = EntityProperties.get(living);
            if (properties != null) {
                properties.loadFromNBT(packet.getTag());
            }
        }
    }

    /**
     * 能力缓存同步：两种载荷共用一个包——
     * 带 Ability/Source 键的是禁用令变更，其余则是按能力注册名分组的存储数据。
     */
    public static void handleAbilityCacheSync(AbilityCacheSyncPacket packet) {
        final AbilityHandler handler = abilityHandlerOf(packet.getEntityID());
        final CompoundTag tag = packet.getTag();
        if (handler == null || tag == null) {
            return;
        }
        if (tag.contains("Ability") && tag.contains("Source")) {
            final String ability = tag.getString("Ability");
            final String source = tag.getString("Source");
            if (tag.getBoolean("DISABLED")) {
                handler.addKillOrder(source, ability);
            } else {
                handler.removeKillOrder(source, ability);
            }
            return;
        }
        for (String key : tag.getAllKeys()) {
            final IAbilityInterface ability = handler.getAbility(key);
            if (ability != null) {
                ability.loadStorage(tag.getCompound(key));
                continue;
            }
            if (key.endsWith(":data")) {
                final IAbilityInterface target = handler.getAbility(key.substring(0, key.length() - 5));
                if (target != null) {
                    target.loadDataCache(tag.getCompound(key));
                }
            }
        }
    }

    public static void handleSyncManaStats(SyncManaStatsPacket packet) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || packet.getTag() == null) {
            return;
        }
        if (mc.level.getEntity(packet.getEntityID()) instanceof LivingEntity living) {
            final MagicStats magic = MagicStats.get(living);
            if (magic != null) {
                magic.loadFromNBT(packet.getTag());
            }
        }
    }

    public static void handleManaCost(SyncManaCostToHudPacket packet) {
        ClientManaCache.setManaCost(packet.getCost());
    }

    public static void handleKeyRelease(AbilityKeyReleasePacket packet) {
        ClientAbilityInput.forceRelease(packet.getHandlerKey());
    }

    @Nullable
    private static AbilityHandler abilityHandlerOf(int entityId) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        final Entity entity = mc.level.getEntity(entityId);
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        final EntityProperties properties = EntityProperties.get(living);
        return properties != null ? properties.getAbilityHandler() : null;
    }

    private ClientPacketHandler() {
    }

    public static void handleEffect(EffectsRenderPacket packet) {
        ClientEffects.play(packet.getEffectID(), packet.getX(), packet.getY(), packet.getZ(),
                packet.getX2(), packet.getY2(), packet.getZ2(), packet.getColor(), packet.getAlpha(), packet.getIntensity());
    }

    public static void handleStatusMessage(StatusMessagePacket packet) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        final TranslationHelper.KeyEntry[] entries = packet.getArgs().stream()
                .map(arg -> new TranslationHelper.OptionEntry(arg.name(),
                        arg.translate() ? I18n.get(arg.value()).trim() : arg.value()))
                .toArray(TranslationHelper.KeyEntry[]::new);
        final String text = TranslationHelper.formatAddVariables(I18n.get(packet.getKey()), entries);
        if (!TranslationHelper.isBlank(text)) {
            mc.player.displayClientMessage(Component.literal(text), packet.isActionBar());
        }
    }

    public static void handleOpenRaceSelection(OpenRaceSelectionPacket packet) {
        ScreenOpener.requestRaceSelection(packet.isFirstLogin());
    }
}
