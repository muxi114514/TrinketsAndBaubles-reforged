package xzeroair.trinkets.items.potions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.item.ItemStack;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.traits.elements.Element;

/**
 * 变身效果：生效期间把实体的「药水种族」设为本效果的种族（与元素），效果消失即清除。
 * 同一时间只保留一种变身效果——施加时移除其他不同种族的变身效果。对应 1.12 items/potions/TransformationPotion。
 *
 * 移植说明：
 * - 种族与元素来自其他注册表，效果注册时可能尚未就绪，故以 Supplier 延迟取用，颜色也延迟计算。
 * - 1.12 另挂了一个「种族属性」修饰器（RaceAttribute），该自定义属性随种族属性批次移植。
 * - 牛奶等治疗物品无法清除变身（与 1.12 一致）。
 */
public class TransformationEffect extends MobEffect {

    private final Supplier<EntityRace> race;
    private final Supplier<Element> element;
    private final String potionId;
    private final int duration;

    public TransformationEffect(Supplier<EntityRace> race, Supplier<Element> element, String potionId, int duration) {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
        this.race = race;
        this.element = element;
        this.potionId = potionId;
        this.duration = duration;
    }

    /** 本效果代表的临时种族状态（每次新建，避免共享可变实例） */
    public RaceCache raceCache() {
        final Element primary = this.element.get();
        return new RaceCache(this.race.get(), primary, primary, true, this.duration, this.potionId);
    }

    @Override
    public int getColor() {
        final Element primary = this.element.get();
        return primary.isNone() ? this.race.get().getPrimaryColor() : primary.getPrimaryColor();
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        final RaceCache cache = this.raceCache();
        final List<MobEffect> conflicting = new ArrayList<>();
        for (MobEffectInstance active : entity.getActiveEffects()) {
            if (active.getEffect() instanceof TransformationEffect other && other != this && !other.raceCache().compare(cache)) {
                conflicting.add(other);
            }
        }
        conflicting.forEach(entity::removeEffect);
        final EntityProperties properties = EntityProperties.get(entity);
        if (properties != null) {
            properties.setPotionRaceCache(cache);
        }
        super.addAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        final EntityProperties properties = EntityProperties.get(entity);
        if (properties != null) {
            properties.setPotionRaceCache(null);
        }
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
