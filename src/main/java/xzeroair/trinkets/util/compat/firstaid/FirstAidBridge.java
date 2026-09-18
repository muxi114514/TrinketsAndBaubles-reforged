package xzeroair.trinkets.util.compat.firstaid;

import ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem;
import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.AbilityHandler.AbilityHolder;
import xzeroair.trinkets.traits.abilities.interfaces.IFirstAidAbility;
import xzeroair.trinkets.util.compat.ModCompat;

/**
 * First Aid 联动接缝：唯一直接引用 First Aid 类的地方，只在 First Aid 已加载时才会被调用（从而才被类加载）。
 * 对应 1.12 FirstAidCompat + FirstAidDamageEvent。
 */
public final class FirstAidBridge {

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(FirstAidBridge::onFirstAidDamage);
    }

    /** 分部位伤害结算后交给拥有 First Aid 钩子的能力，任一能力要求取消即取消（1.12 FirstAidDamageEvent） */
    private static void onFirstAidDamage(FirstAidLivingDamageEvent event) {
        if (!ModCompat.firstAid()) {
            return;
        }
        final Player player = event.getEntity();
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null) {
            return;
        }
        final FirstAidDamage damage = new ModelView(event.getBeforeDamage(), event.getAfterDamage());
        for (AbilityHolder holder : properties.getAbilityHandler().getActiveAbilities().values()) {
            if (!(holder.getAbility() instanceof IFirstAidAbility ability)) {
                continue;
            }
            try {
                if (ability.firstAidHit(player, event.getSource(), event.getUndistributedDamage(), damage)) {
                    event.setCanceled(true);
                    return;
                }
            } catch (RuntimeException e) {
                Trinkets.LOGGER.error("Trinkets had an error with ability: {}", ability.getRegistryName(), e);
            }
        }
    }

    /**
     * 最大生命值变化后立即按新上限重算各部位血量（First Aid 自身每 tick 也会做，这里只是不等下一 tick）。
     * 装有 First Aid 时不能直接 setHealth 压低血量——会被当作一次伤害分摊到部位上。
     */
    public static void rescale(Player player) {
        player.getCapability(CapabilityExtendedHealthSystem.INSTANCE).ifPresent(model -> model.runScaleLogic(player));
    }

    private record ModelView(AbstractPlayerDamageModel before, AbstractPlayerDamageModel after) implements FirstAidDamage {

        @Override
        public float headHealth() {
            return this.after.HEAD.currentHealth;
        }

        @Override
        public float bodyHealth() {
            return this.after.BODY.currentHealth;
        }

        @Override
        public void restoreHead() {
            this.after.HEAD.currentHealth = this.before.HEAD.currentHealth;
        }

        @Override
        public void healAllParts() {
            for (AbstractDamageablePart part : this.after) {
                part.currentHealth = part.getMaxHealth();
            }
        }

        @Override
        public void scheduleResync() {
            this.after.scheduleResync();
        }
    }

    private FirstAidBridge() {
    }
}
