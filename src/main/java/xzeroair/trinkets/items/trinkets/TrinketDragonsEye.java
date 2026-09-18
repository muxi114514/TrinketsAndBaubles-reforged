package xzeroair.trinkets.items.trinkets;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.items.base.AccessoryBase;
import xzeroair.trinkets.traits.abilities.AbilityGreedyEyes;
import xzeroair.trinkets.traits.abilities.AbilityNightVision;
import xzeroair.trinkets.traits.abilities.elements.fire.AbilityFireImmunity;
import xzeroair.trinkets.traits.abilities.elements.ice.AbilityFrostWalker;
import xzeroair.trinkets.traits.abilities.elements.ice.AbilityIceImmunity;
import xzeroair.trinkets.traits.abilities.elements.lightning.AbilityLightningImmunity;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.SurvivalConfig;
import xzeroair.trinkets.util.config.server.items.AccessoryAbilitiesConfigs.DragonsEye;
import xzeroair.trinkets.util.config.server.items.AccessoryConfig;
import xzeroair.trinkets.util.config.server.race.ElementOverrideConfig;

/**
 * 龙之眼：夜视 + 寻宝；中性形态可选火焰免疫，火/冰/雷三种元素形态各有免疫能力并整体替换效果、伤害规则与属性配置。
 * 对应 1.12 TrinketDragonsEye。
 */
public class TrinketDragonsEye extends AccessoryBase {

    public TrinketDragonsEye() {
        super("6a345136-49b7-4b71-88dc-87301e329ac1");
    }

    @Override
    public AccessoryConfig<DragonsEye> getAccessoryConfig() {
        return TrinketsConfig.SERVER.items.dragonsEye;
    }

    @Override
    public Element[] getSubElements() {
        return new Element[] {ModElements.FIRE.get(), ModElements.ICE.get(), ModElements.LIGHTNING.get()};
    }

    @Override
    public void initAbilities(ItemStack stack, List<IAbilityInterface> abilities) {
        final DragonsEye config = this.getAccessoryConfig().abilities;
        if (config == null) {
            return;
        }
        abilities.add(new AbilityNightVision(config.nightVision));
        final Element element = this.getPrimaryElement(stack);
        if (element == ModElements.FIRE.get()) {
            abilities.add(new AbilityFireImmunity(config.fire.fireImmunity).setRequiredElement(element));
        } else if (element == ModElements.ICE.get()) {
            abilities.add(new AbilityIceImmunity(config.ice.iceImmunity).setRequiredElement(element));
            abilities.add(new AbilityFrostWalker(config.ice.frostWalker).setRequiredElement(element));
        } else if (element == ModElements.LIGHTNING.get()) {
            abilities.add(new AbilityLightningImmunity(config.lightning.lightningImmunity).setRequiredElement(element));
        } else {
            abilities.add(new AbilityFireImmunity(config.fireImmunity));
        }
        abilities.add(new AbilityGreedyEyes(config.greedyEyes));
    }

    @Nullable
    @Override
    public SurvivalConfig getSurvivalConfig(ItemStack stack) {
        final DragonsEye config = this.getAccessoryConfig().abilities;
        final Element element = this.getPrimaryElement(stack);
        if (config != null && element == ModElements.FIRE.get()) {
            return config.fire.survival;
        }
        if (config != null && element == ModElements.ICE.get()) {
            return config.ice.survival;
        }
        if (config != null && element == ModElements.LIGHTNING.get()) {
            return config.lightning.survival;
        }
        return super.getSurvivalConfig(stack);
    }

    /** 元素形态的替换配置；中性形态返回 null（使用本饰品的通用配置） */
    @Nullable
    private ElementOverrideConfig overrides(ItemStack stack) {
        final DragonsEye config = this.getAccessoryConfig().abilities;
        if (config == null) {
            return null;
        }
        final Element element = this.getPrimaryElement(stack);
        if (element == ModElements.FIRE.get()) {
            return config.fire.overrides;
        }
        if (element == ModElements.ICE.get()) {
            return config.ice.overrides;
        }
        return element == ModElements.LIGHTNING.get() ? config.lightning.overrides : null;
    }

    @Override
    public List<? extends String> getEffectsToAdd(ItemStack stack) {
        final ElementOverrideConfig overrides = this.overrides(stack);
        return overrides != null ? overrides.effectsToAdd.get() : super.getEffectsToAdd(stack);
    }

    @Override
    public List<? extends String> getEffectsToRemove(ItemStack stack) {
        final ElementOverrideConfig overrides = this.overrides(stack);
        return overrides != null ? overrides.effectsToRemove.get() : super.getEffectsToRemove(stack);
    }

    @Override
    public List<? extends String> getDamageTypesToIgnore(ItemStack stack) {
        final ElementOverrideConfig overrides = this.overrides(stack);
        return overrides != null ? overrides.damageTypesToIgnore.get() : super.getDamageTypesToIgnore(stack);
    }

    @Override
    public List<? extends String> getAttributeConfig(ItemStack stack) {
        final ElementOverrideConfig overrides = this.overrides(stack);
        return overrides != null ? overrides.attributes.get() : super.getAttributeConfig(stack);
    }
}
