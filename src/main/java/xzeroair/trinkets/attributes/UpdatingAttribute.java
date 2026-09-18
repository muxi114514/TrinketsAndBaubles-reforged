package xzeroair.trinkets.attributes;

import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.compat.firstaid.FirstAidBridge;

/**
 * 一个「按 UUID 定位、数值可变」的属性修饰器：每次以最新数值调用 {@link #addModifier}，
 * 数值或运算方式没变就保持不动，变了才替换。对应 1.12 attributes/UpdatingAttribute。
 *
 * 移植说明：
 * - 1.20.1 的 AttributeInstance#addTransientModifier / addPermanentModifier 遇到重复 UUID 会直接抛异常
 *   （1.12 的 applyModifier 是覆盖语义），故必须先 getModifier 检查、需要时先 removeModifier 再添加——
 *   每 tick 反复调用本类是安全的，直接每 tick add 则会崩。
 * - 1.12 按属性名字符串查找实例；1.20.1 属性是注册表对象，改持有 Supplier 以便引用尚未完成注册的自定义属性。
 * - 1.12 对 stepHeight 属性移除时还要手动回写 entity.stepHeight 字段；1.20.1 改由 ForgeMod.STEP_HEIGHT_ADDITION
 *   原生属性驱动，无需回写，该分支不移植。
 * - 最大生命值变化只在服务端处理，并在降低时把当前血量钳到新上限（对应 1.12 syncMaxHealthState）。
 *   装有 First Aid 时 setHealth 降低血量会被当作伤害分摊到部位上（可能致死），改为让 First Aid 按新上限重算部位血量。
 */
public class UpdatingAttribute {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private final String name;
    private final UUID uuid;
    private final Supplier<Attribute> attribute;
    private boolean savedInNBT;

    public UpdatingAttribute(String name, UUID uuid, Supplier<Attribute> attribute) {
        this.name = name;
        this.uuid = uuid;
        this.attribute = attribute;
    }

    public UpdatingAttribute setSavedInNBT(boolean savedInNBT) {
        this.savedInNBT = savedInNBT;
        return this;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    @Nullable
    private AttributeInstance instanceOf(@Nullable LivingEntity entity) {
        if (entity == null || entity.level() == null || EMPTY_UUID.equals(this.uuid)) {
            return null;
        }
        return entity.getAttribute(this.attribute.get());
    }

    /**
     * 施加或更新修饰器；amount 为 0 视为移除。
     *
     * @param operation 0 加法 / 1 乘基础值 / 2 乘总值（与 1.12 一致）
     */
    public void addModifier(@Nullable LivingEntity entity, double amount, int operation) {
        final AttributeInstance instance = this.instanceOf(entity);
        if (instance == null) {
            return;
        }
        final boolean maxHealth = instance.getAttribute() == Attributes.MAX_HEALTH;
        if (maxHealth && entity.level().isClientSide) {
            return;
        }
        final AttributeModifier.Operation op = AttributeModifier.Operation.fromValue(operation);
        final AttributeModifier existing = instance.getModifier(this.uuid);
        if (existing != null) {
            if (amount != 0 && existing.getAmount() == amount && existing.getOperation() == op) {
                return;
            }
            instance.removeModifier(this.uuid);
        }
        if (amount == 0) {
            if (maxHealth) {
                clampHealth(entity);
            }
            return;
        }
        final AttributeModifier modifier = new AttributeModifier(this.uuid, this.name, amount, op);
        if (this.savedInNBT) {
            instance.addPermanentModifier(modifier);
        } else {
            instance.addTransientModifier(modifier);
        }
        if (maxHealth) {
            clampHealth(entity);
        }
    }

    public void removeModifier(@Nullable LivingEntity entity) {
        final AttributeInstance instance = this.instanceOf(entity);
        if (instance == null || instance.getModifier(this.uuid) == null) {
            return;
        }
        final boolean maxHealth = instance.getAttribute() == Attributes.MAX_HEALTH;
        if (maxHealth && entity.level().isClientSide) {
            return;
        }
        instance.removeModifier(this.uuid);
        if (maxHealth) {
            clampHealth(entity);
        }
    }

    private static void clampHealth(LivingEntity entity) {
        if (entity instanceof Player player && ModCompat.firstAid()) {
            FirstAidBridge.rescale(player);
            return;
        }
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }
}
