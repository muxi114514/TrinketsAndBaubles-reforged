package xzeroair.trinkets.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.items.potions.BleedEffect;
import xzeroair.trinkets.items.potions.InvigoratedEffect;
import xzeroair.trinkets.items.potions.ParalysisEffect;
import xzeroair.trinkets.items.potions.ResistanceEffect;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.elements.ice.AbilityIceImmunity;
import xzeroair.trinkets.traits.abilities.elements.lightning.AbilityLightningImmunity;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.abilities.IAbilityConfig;

/**
 * 状态效果注册表（对应 1.12 ModPotionTypes.registerPotionEffects 与 registerPotionTypes 中的效果部分）。
 * 移植说明：1.12 的 Potion 在 1.20.1 叫 MobEffect，PotionType 才叫 Potion——两版命名互换，勿搞反。
 * 冰/雷抗性在 1.12 随酿造药水一起注册（registerPotionTypes），此处先注册效果本体，
 * 对应的酿造药水随 P3c 药水体系补入。
 */
public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Reference.MODID);

    public static final RegistryObject<MobEffect> BLEED = EFFECTS.register("bleed", BleedEffect::new);
    public static final RegistryObject<MobEffect> PARALYSIS = EFFECTS.register("paralysis", ParalysisEffect::new);
    public static final RegistryObject<MobEffect> INVIGORATED = EFFECTS.register("invigorated", InvigoratedEffect::new);

    public static final RegistryObject<MobEffect> ICE_RESISTANCE = EFFECTS.register("ice_resistance",
            () -> new ResistanceEffect(15132390, AbilityNames.key(AbilityNames.IMMUNITY_ICE),
                    () -> new AbilityIceImmunity(IAbilityConfig.ALWAYS_ENABLED)));

    public static final RegistryObject<MobEffect> LIGHTNING_RESISTANCE = EFFECTS.register("lightning_resistance",
            () -> new ResistanceEffect(15132390, AbilityNames.key(AbilityNames.IMMUNITY_LIGHTNING),
                    () -> new AbilityLightningImmunity(IAbilityConfig.ALWAYS_ENABLED)));

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }

    private ModEffects() {
    }
}
