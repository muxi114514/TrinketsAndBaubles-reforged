package xzeroair.trinkets.traits.abilities;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import xzeroair.trinkets.util.config.abilities.RepelAbilityConfig;
import xzeroair.trinkets.util.helpers.EntityChecks;
import xzeroair.trinkets.util.helpers.Kinetics;

/**
 * 排斥：把靠近的名单内实体（默认箭与火球）推开（极化石副能力）。对应 1.12 AbilityRepel。
 *
 * 移植说明：1.12 在两端都推，客户端只是本地预测；此处只在服务端推并标记速度同步。
 */
public class AbilityRepel extends AbilityItemToggle implements ITickableAbility, IHeldAbility, ITickableInventoryAbility,
        IKeyBindInterface {

    private final RepelAbilityConfig config;

    public AbilityRepel(@Nonnull RepelAbilityConfig config) {
        super(AbilityNames.REPEL, Flag.ALT, false);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (this.isAbilityToggled() && !entity.level().isClientSide && entity.isAlive()
                && this.payUpkeep(entity, this.config.cost.get().floatValue(), this.config.frequency.get())) {
            this.repel(entity);
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

    private void repel(LivingEntity entity) {
        final List<? extends String> whitelist = this.config.whitelist.get();
        if (whitelist.isEmpty()) {
            return;
        }
        final double horizontal = this.config.rangeHorizontal.get();
        final double vertical = this.config.rangeVertical.get();
        final AABB area = entity.getBoundingBox().inflate(horizontal, vertical, horizontal);
        final List<Entity> targets = entity.level().getEntitiesOfClass(Entity.class, area,
                target -> target.isAlive() && !(target instanceof Player) && EntityChecks.isListed(target, whitelist));
        final double force = this.config.force.get();
        for (Entity target : targets) {
            Kinetics.applyForce(entity, target, false, force, 0.8D, 0.85D);
        }
    }

    @Override
    protected void sendToggleMessage(LivingEntity entity) {
        new StatusMessagePacket(this.getTranslationKey() + ".repelmode", true)
                .withKey("repeltoggle", this.isAbilityToggled() ? "xat.tooltip.on" : "xat.tooltip.off")
                .send(entity);
    }

    /** 辅助键 + 主按键切换排斥 */
    @Override
    public boolean onKeyPress(Entity entity, boolean aux) {
        if (aux && entity instanceof LivingEntity living && !living.level().isClientSide) {
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
        final double cost = this.config.cost.get();
        variables.keybind("key", KeyNames.POLARIZED_STONE_ABILITY)
                .keybind("aux", KeyNames.AUX_KEY)
                .number("force", this.config.force.get())
                .number("horizontal", this.config.rangeHorizontal.get())
                .number("vertical", this.config.rangeVertical.get())
                .entities("targets", true, this.config.whitelist.get())
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
