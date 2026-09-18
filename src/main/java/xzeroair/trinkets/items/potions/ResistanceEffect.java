package xzeroair.trinkets.items.potions;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.api.ItemHandlerType;
import xzeroair.trinkets.api.SlotInformation;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.AbilityHandler;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;

/**
 * 元素抗性效果：持续期间让实体获得对应的免疫能力（对应 1.12 IceResistance / LightningResistance）。
 *
 * 效果与能力是一座双向桥：
 * - 效果 → 能力：本效果每 tick 以「药水」为来源注册免疫能力；效果结束后 AbilityHandler 的
 *   POTION 分支发现来源失效，自动移除该能力。
 * - 能力 → 效果：来源是种族或饰品的免疫能力会反过来给出本效果，纯粹为了显示图标；
 *   来源是药水时能力不会再给效果，避免互相续命。
 *
 * 移植说明：1.12 两个类只差 new 的能力类型，此处收敛为一个类 + 能力工厂。
 * 1.12 每 tick 无条件 new 一个能力再交给 registerAbility 去重，此处先查是否已存在再创建，免去每 tick 的临时对象。
 */
public class ResistanceEffect extends MobEffect {

    private final String abilityKey;
    private final Supplier<IAbilityInterface> abilityFactory;

    public ResistanceEffect(int color, String abilityKey, Supplier<IAbilityInterface> abilityFactory) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.abilityKey = abilityKey;
        this.abilityFactory = abilityFactory;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        final EntityProperties properties = EntityProperties.get(entity);
        if (properties == null) {
            return;
        }
        final AbilityHandler handler = properties.getAbilityHandler();
        if (handler.getAbility(this.abilityKey) != null) {
            return;
        }
        final ResourceLocation source = ForgeRegistries.MOB_EFFECTS.getKey(this);
        if (source == null) {
            return;
        }
        handler.registerAbility(entity, source.toString(), new SlotInformation(ItemHandlerType.POTION), this.abilityFactory.get());
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
