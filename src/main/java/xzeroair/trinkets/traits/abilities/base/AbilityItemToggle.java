package xzeroair.trinkets.traits.abilities.base;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.items.base.TrinketData;
import xzeroair.trinkets.traits.AbilityHandler.AbilityHolder;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.interfaces.IToggleAbility;

/**
 * 开关状态跟随饰品栈 NBT（主/副能力开关）的能力基类：极化石的磁力/排斥、末影王冠的传送。
 *
 * 移植说明：1.12 三个能力各自复制了一遍「与 TrinketProperties 的 main/alt 开关比对并提示」的代码，此处上移。
 * 另外 1.12 魔力不足关闭能力时只改能力、不改物品，下一 tick 又被物品开关打开并重复提示；此处同时写回物品。
 */
public abstract class AbilityItemToggle extends Ability implements IToggleAbility {

    public enum Flag {
        MAIN,
        ALT;

        boolean read(ItemStack stack) {
            return this == MAIN ? TrinketData.isMainAbility(stack) : TrinketData.isAltAbility(stack);
        }

        void write(ItemStack stack, boolean enabled) {
            if (this == MAIN) {
                TrinketData.setMainAbility(stack, enabled);
            } else {
                TrinketData.setAltAbility(stack, enabled);
            }
        }
    }

    private final Flag flag;
    private boolean toggled;
    private int mode = -1;

    protected AbilityItemToggle(String name, Flag flag, boolean defaultToggled) {
        super(name);
        this.flag = flag;
        this.toggled = defaultToggled;
    }

    /** 该栈是否是本能力的来源物品 */
    protected abstract boolean isSourceItem(ItemStack stack);

    /** 开关变化时提示玩家（仅服务端调用） */
    protected abstract void sendToggleMessage(LivingEntity entity);

    /** 物品开关与能力不一致时以物品为准 */
    protected void syncFromStack(ItemStack stack, @Nullable Entity entity) {
        if (!this.isSourceItem(stack)) {
            return;
        }
        final boolean enabled = this.flag.read(stack);
        if (enabled != this.toggled) {
            this.toggleAbility(enabled);
            if (entity instanceof LivingEntity living && !living.level().isClientSide) {
                this.sendToggleMessage(living);
            }
        }
    }

    /** 改变开关并写回来源物品（按键切换、魔力耗尽时使用） */
    protected void setToggled(LivingEntity entity, boolean enabled) {
        this.toggleAbility(enabled);
        final ItemStack stack = this.sourceStack(entity);
        if (this.isSourceItem(stack)) {
            this.flag.write(stack, enabled);
        }
    }

    /** 开启期间的魔力消耗：付不起直接关闭并提示；每 frequency tick 扣一次。返回本 tick 能否生效 */
    protected boolean payUpkeep(LivingEntity entity, float cost, int frequency) {
        final MagicStats magic = MagicStats.get(entity);
        if (cost <= 0F || magic == null) {
            return true;
        }
        final boolean due = entity.tickCount % frequency == 0;
        if (!magic.canSpendMana(cost) || (due && !magic.spendMana(cost))) {
            this.setToggled(entity, false);
            this.sendToggleMessage(entity);
            return false;
        }
        return true;
    }

    protected ItemStack sourceStack(LivingEntity entity) {
        final AbilityHolder holder = this.getAbilityHolder();
        return holder == null ? ItemStack.EMPTY : holder.getInfo().getStackFromHandler(entity);
    }

    @Override
    public boolean isAbilityToggled() {
        return this.toggled;
    }

    @Override
    public int getToggleMode() {
        return this.mode;
    }

    @Override
    public IToggleAbility toggleAbility(boolean enabled) {
        if (this.toggled != enabled) {
            this.toggled = enabled;
            this.setChanged(true);
        }
        return this;
    }

    @Override
    public IToggleAbility toggleAbility(int value) {
        if (this.mode != value) {
            this.mode = value;
            this.setChanged(true);
        }
        return this.toggleAbility(value > 0);
    }
}
