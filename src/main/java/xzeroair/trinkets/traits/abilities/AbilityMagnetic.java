package xzeroair.trinkets.traits.abilities;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.network.StatusMessagePacket;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.base.AbilityItemToggle;
import xzeroair.trinkets.traits.abilities.interfaces.IHeldAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IKeyBindInterface;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableInventoryAbility;
import xzeroair.trinkets.util.KeyNames;
import xzeroair.trinkets.util.config.abilities.MagneticAbilityConfig;
import xzeroair.trinkets.util.helpers.EntityChecks;
import xzeroair.trinkets.util.helpers.Kinetics;

/**
 * 磁力：吸取周围的掉落物与经验球（极化石主能力，佩戴、放在物品栏或手持均生效）。对应 1.12 AbilityMagnetic。
 * 附近有其他开着磁力的玩家离得更近时让给对方，避免互相抢夺抖动。
 */
public class AbilityMagnetic extends AbilityItemToggle implements ITickableAbility, IHeldAbility, ITickableInventoryAbility,
        IKeyBindInterface {

    private final MagneticAbilityConfig config;

    public AbilityMagnetic(@Nonnull MagneticAbilityConfig config) {
        super(AbilityNames.MAGNETIC, Flag.MAIN, false);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (this.isAbilityToggled() && !entity.level().isClientSide) {
            this.collectDrops(entity);
        }
    }

    @Override
    public void onUpdate(ItemStack stack, Level level, Entity entity, int itemSlot, boolean inHand) {
        this.syncFromStack(stack, entity);
    }

    @Override
    protected boolean isSourceItem(ItemStack stack) {
        return stack.is(ModItems.POLARIZED_STONE.get());
    }

    private void collectDrops(LivingEntity entity) {
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity) || !entity.isAlive()
                || !this.payUpkeep(entity, this.config.cost.get().floatValue(), this.config.frequency.get())) {
            return;
        }
        final double horizontal = this.config.rangeHorizontal.get();
        final double vertical = this.config.rangeVertical.get();
        final AABB area = entity.getBoundingBox().inflate(horizontal, vertical, horizontal);
        final boolean xp = this.config.pickupXp.get();
        final List<Entity> loot = entity.level().getEntitiesOfClass(Entity.class, area,
                drop -> drop.isAlive() && (drop instanceof ItemEntity || (xp && drop instanceof ExperienceOrb)));
        if (loot.isEmpty()) {
            return;
        }
        final List<Player> others = entity.level().getEntitiesOfClass(Player.class, area,
                other -> other != entity && other.isAlive() && EntityChecks.hasAbility(other, this.getRegistryName().toString()));
        for (Entity drop : loot) {
            final double distance = drop.distanceToSqr(entity);
            if (others.stream().noneMatch(other -> drop.distanceToSqr(other) < distance)) {
                this.handleLoot(entity, drop);
            }
        }
    }

    private void handleLoot(LivingEntity entity, Entity drop) {
        if (entity instanceof Player player) {
            if (drop instanceof ItemEntity item && this.config.pickupInstant.get()) {
                // 不瞬吸极化石本身，避免丢出去立刻又被吸回
                if (!this.isSourceItem(item.getItem())) {
                    item.playerTouch(player);
                }
                return;
            }
            if (drop instanceof ExperienceOrb orb && this.config.pickupInstantXp.get()) {
                player.takeXpDelay = 0;
                orb.playerTouch(player);
                return;
            }
        }
        Kinetics.applyForce(entity, drop, true, this.config.force.get(), 0.8D, 0.85D);
    }

    @Override
    protected void sendToggleMessage(LivingEntity entity) {
        new StatusMessagePacket(this.getTranslationKey() + ".magnetmode", true)
                .withKey("collecttoggle", this.isAbilityToggled() ? "xat.tooltip.on" : "xat.tooltip.off")
                .send(entity);
    }

    /** 主按键切换磁力（副按键留给排斥） */
    @Override
    public boolean onKeyPress(Entity entity, boolean aux) {
        if (!aux && entity instanceof LivingEntity living && !living.level().isClientSide) {
            this.setToggled(living, !this.isAbilityToggled());
            this.sendToggleMessage(living);
        }
        return true;
    }

    @Override
    public String getKey() {
        return KeyNames.POLARIZED_STONE_ABILITY;
    }

    @Override
    public String getAuxKey() {
        return KeyNames.AUX_KEY;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final boolean instant = this.config.pickupInstant.get();
        final boolean xp = this.config.pickupXp.get();
        final boolean instantXp = this.config.pickupInstantXp.get();
        final double cost = this.config.cost.get();
        variables.keybind("key", KeyNames.POLARIZED_STONE_ABILITY)
                .number("horizontal", this.config.rangeHorizontal.get())
                .number("vertical", this.config.rangeVertical.get())
                .flag("instant", instant)
                .number("force", !instant, this.config.force.get())
                .flag("xpinstant", xp && instantXp)
                .number("xpforce", xp && !instantXp, this.config.force.get())
                .number("cost", cost > 0, cost)
                .seconds("interval", true, this.config.frequency.get());
    }

    @Override
    public void describeStatus(DescriptionVariables variables, ItemStack source) {
        variables.toggle("state", true, this.isAbilityToggled());
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.config.cost.get() * 20.0D / this.config.frequency.get(), ManaCost.Unit.SECOND);
    }
}
