package xzeroair.trinkets.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.util.Reference;

/**
 * 自定义属性（对应 1.12 attributes/MagicAttributes、JumpAttribute、FlyingAttribute）。
 *
 * 移植说明：1.12 的 IAttribute 是 new 出来的常量，由实体在构造时 registerAttribute 挂上；
 * 1.20.1 属性是注册表条目，须经 EntityAttributeModificationEvent（mod 事件总线）追加到实体类型上。
 * 描述键沿用 1.12 的 attribute.name.xat.entityMagic.*，以复用已迁移的译文。
 * 1.12 的 setShouldWatch(true) 对应 1.20.1 的 setSyncable(true)：属性值随实体数据同步到客户端，魔力条才能显示上限。
 */
public class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, Reference.MODID);

    public static final RegistryObject<Attribute> MAX_MANA = ATTRIBUTES.register("max_mana",
            () -> new RangedAttribute("attribute.name.xat.entityMagic.maxMana", 100.0D, 0.0D, Double.MAX_VALUE).setSyncable(true));

    public static final RegistryObject<Attribute> MANA_REGEN = ATTRIBUTES.register("mana_regen",
            () -> new RangedAttribute("attribute.name.xat.entityMagic.regen", 1.0D, 0.0D, Double.MAX_VALUE).setSyncable(true));

    public static final RegistryObject<Attribute> MANA_REGEN_COOLDOWN = ATTRIBUTES.register("mana_regen_cooldown",
            () -> new RangedAttribute("attribute.name.xat.entityMagic.regen.cooldown", 1.0D, 0.0D, Double.MAX_VALUE).setSyncable(true));

    public static final RegistryObject<Attribute> MAGIC_AFFINITY = ATTRIBUTES.register("magic_affinity",
            () -> new RangedAttribute("attribute.name.xat.entityMagic.affinity", 0.0D, 0.0D, Double.MAX_VALUE).setSyncable(true));

    /** 跳跃高度倍率（对应 1.12 JumpAttribute.Jump，xat.jump）：影响起跳速度并抵扣坠落距离 */
    public static final RegistryObject<Attribute> JUMP = ATTRIBUTES.register("jump",
            () -> new RangedAttribute("attribute.name.xat.jump", 1.0D, 0.0D, 1024.0D).setSyncable(true));

    /** 创造飞行速度（对应 1.12 FlyingAttribute.Fly_Speed，xat.flyspeed），基础值同原版 0.05 */
    public static final RegistryObject<Attribute> FLY_SPEED = ATTRIBUTES.register("fly_speed",
            () -> new RangedAttribute("attribute.name.xat.flyspeed", 0.05D, 0.0D, 256.0D).setSyncable(true));

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(ModAttributes::onAttributeModification);
    }

    /** 1.12 只给玩家挂魔力属性（MagicStats 也只附加在玩家身上），此处保持一致 */
    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAX_MANA.get());
        event.add(EntityType.PLAYER, MANA_REGEN.get());
        event.add(EntityType.PLAYER, MANA_REGEN_COOLDOWN.get());
        event.add(EntityType.PLAYER, MAGIC_AFFINITY.get());
        event.add(EntityType.PLAYER, JUMP.get());
        event.add(EntityType.PLAYER, FLY_SPEED.get());
    }

    private ModAttributes() {
    }
}
