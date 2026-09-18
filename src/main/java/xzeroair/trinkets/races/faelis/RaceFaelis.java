package xzeroair.trinkets.races.faelis;

import java.util.OptionalDouble;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.attributes.UpdatingAttribute;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.init.ModAttributes;
import xzeroair.trinkets.init.ModEffects;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.abilities.AbilityClimbing;
import xzeroair.trinkets.traits.abilities.AbilityNightVision;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.RaceConfig;
import xzeroair.trinkets.util.config.server.race.FaelisAbilitiesConfig;
import xzeroair.trinkets.util.helpers.DamageHelper;
import xzeroair.trinkets.util.helpers.EquipmentWeights;
import xzeroair.trinkets.util.helpers.PotionHelper;

/**
 * 法埃利斯种族的行为处理器：夜视、攀爬；徒手格斗加成；喝奶获得活力与增益；身披重甲时减速并降低跳跃。
 *
 * 移植说明：
 * - 「效果增删 / 药水免疫 / 坐骑黑白名单」三段与其余 8 族相同的逻辑已上移至基类。
 * - 重甲与徒手名单由 1.12 的 ConfigEquipmentObject（含 metadata 与材质枚举）改为 EquipmentWeights。
 * - 1.12 在「惩罚为 0 且未开启活力抵消」时不会撤掉旧惩罚（残留旧值），这里改为按当前状态立即更新。
 */
public class RaceFaelis extends EntityRacePropertiesHandler {

    private static final UUID PENALTY_UUID = UUID.fromString("1c9ba72a-a558-4ccc-a997-777bf3a9859a");

    private final UpdatingAttribute movementPenalty = new UpdatingAttribute("xat.faelis.heavy_armor", PENALTY_UUID,
            () -> Attributes.MOVEMENT_SPEED);
    private final UpdatingAttribute jumpPenalty = new UpdatingAttribute("xat.faelis.heavy_armor", PENALTY_UUID, ModAttributes.JUMP);

    public RaceFaelis(@Nullable LivingEntity entity, @Nullable EntityProperties properties, @Nonnull RaceCache cache) {
        super(entity, properties, cache);
    }

    @Override
    public RaceConfig<FaelisAbilitiesConfig> getConfig() {
        return TrinketsConfig.SERVER.races.faelis;
    }

    @Override
    public void registerRaceAbilities() {
        final FaelisAbilitiesConfig abilities = this.getConfig().abilities;
        if (abilities == null) {
            return;
        }
        this.addAbility(new AbilityNightVision(abilities.nightVision));
        this.addAbility(new AbilityClimbing(abilities.climbing));
    }

    @Override
    public void whileTransformed() {
        super.whileTransformed();
        final LivingEntity entity = this.getEntity();
        final FaelisAbilitiesConfig abilities = this.getConfig().abilities;
        if (entity == null || entity.level().isClientSide || abilities == null) {
            return;
        }
        final boolean invigorated = entity.hasEffect(ModEffects.INVIGORATED.get()) && abilities.milkInvigorated.get();
        double penalty = 0;
        if (abilities.heavyArmorPenalty.get() && !invigorated) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                final OptionalDouble weight = EquipmentWeights.weightOf(abilities.heavyArmor.get(), entity.getItemBySlot(slot), slot);
                if (weight.isPresent()) {
                    penalty -= weight.getAsDouble();
                }
            }
        }
        // amount 为 0 时 UpdatingAttribute 会撤掉修饰器
        this.movementPenalty.addModifier(entity, penalty, 2);
        this.jumpPenalty.addModifier(entity, penalty, 2);
    }

    @Override
    public void endTransformation() {
        super.endTransformation();
        this.movementPenalty.removeModifier(this.getEntity());
        this.jumpPenalty.removeModifier(this.getEntity());
    }

    @Override
    public void describeTraits(DescriptionVariables variables) {
        final FaelisAbilitiesConfig abilities = this.getConfig().abilities;
        if (abilities == null) {
            return;
        }
        final boolean milk = abilities.milkBonus.get();
        final boolean heavy = abilities.heavyArmorPenalty.get();
        variables.number("barehand", abilities.barehandCombat.get(), abilities.barehandCombatBonus.get())
                .items("barehands", abilities.barehandCombat.get(), abilities.bareHands.get())
                .items("milk", milk, abilities.milk.get())
                .seconds("milkduration", milk, abilities.milkBonusDuration.get())
                .translated("invigorated", milk, ModEffects.INVIGORATED.get().getDescriptionId())
                .effects("milkbuffs", milk, abilities.milkBuffs.get(), true)
                .flag("heavy", heavy)
                .flag("invigoratedheavy", heavy && milk && abilities.milkInvigorated.get());
    }

    /** 直接近战且双手都算徒手时追加伤害；名单内的手持物也算徒手并附加其数值 */
    @Override
    public float hurtEntity(LivingEntity target, DamageSource source, float dmg) {
        final FaelisAbilitiesConfig abilities = this.getConfig().abilities;
        final LivingEntity entity = this.getEntity();
        if (abilities == null || entity == null || !abilities.barehandCombat.get() || dmg <= 0 || DamageHelper.isIndirect(source)) {
            return dmg;
        }
        final float[] bonus = {0};
        final boolean mainBare = countsAsBare(abilities, entity.getMainHandItem(), EquipmentSlot.MAINHAND, bonus);
        final boolean offBare = countsAsBare(abilities, entity.getOffhandItem(), EquipmentSlot.OFFHAND, bonus);
        float result = dmg + bonus[0];
        if (mainBare && offBare) {
            result += abilities.barehandCombatBonus.get().floatValue();
        }
        return result;
    }

    private static boolean countsAsBare(FaelisAbilitiesConfig abilities, ItemStack stack, EquipmentSlot slot, float[] bonus) {
        if (stack.isEmpty()) {
            return true;
        }
        final OptionalDouble weight = EquipmentWeights.weightOf(abilities.bareHands.get(), stack, slot);
        if (weight.isPresent()) {
            bonus[0] += (float) weight.getAsDouble();
            return true;
        }
        return false;
    }

    /** 喝下名单中的奶：给予活力（等级/时长可在条目中指定）与额外增益 */
    @Override
    public void itemUseFinished(ItemStack stack) {
        final FaelisAbilitiesConfig abilities = this.getConfig().abilities;
        final LivingEntity entity = this.getEntity();
        if (abilities == null || entity == null || entity.level().isClientSide || !abilities.milkBonus.get()) {
            return;
        }
        final ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return;
        }
        for (String entry : abilities.milk.get()) {
            final String[] parts = entry.trim().split(";");
            if (!parts[0].trim().equalsIgnoreCase(id.toString())) {
                continue;
            }
            final int level = parts.length > 1 ? parseInt(parts[parts.length > 2 ? parts.length - 2 : 1], 0) : 0;
            final int duration = parts.length > 2 ? parseInt(parts[parts.length - 1], abilities.milkBonusDuration.get())
                    : abilities.milkBonusDuration.get();
            entity.addEffect(new MobEffectInstance(ModEffects.INVIGORATED.get(), duration, Math.max(0, level), false, false));
            PotionHelper.applyAllFromConfig(entity, abilities.milkBuffs.get());
            return;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
