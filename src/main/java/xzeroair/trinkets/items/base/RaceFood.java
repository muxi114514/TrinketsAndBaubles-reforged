package xzeroair.trinkets.items.base;

import javax.annotation.Nonnull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.races.IRaceProvider;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.traits.elements.IElementProvider;
import xzeroair.trinkets.traits.elements.ItemElements;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 种族食物：吃下后把「附着种族」设为本食物的种族（带物品上的元素，如龙宝石的火/冰/雷），1 秒冷却。
 * 附着种族在没有药水与变身饰品时生效，由 EntityProperties#updateRace 解析。对应 1.12 items/base/RaceFood。
 *
 * 移植说明：
 * - 物品构造早于种族注册表填充，故只记 raceId，用时再查表。
 * - 1.12 的冷却写在物品实例字段上（所有同类物品共用），改为原版按玩家的物品冷却。
 * - 元素经物品 NBT 保存（见 ItemElements），不再需要物品 capability。
 */
public class RaceFood extends FoodBase implements IRaceProvider, IElementProvider {

    public static final int COOLDOWN_TICKS = 20;

    private final String raceId;

    public RaceFood(Properties properties, int useDuration, UseAnim useAnimation, String raceId) {
        super(properties, useDuration, useAnimation);
        this.raceId = raceId;
    }

    public String getRaceId() {
        return this.raceId;
    }

    @Override
    public EntityRace getRace() {
        final EntityRace race = ModRaces.registry().getValue(new ResourceLocation(Reference.MODID, this.raceId));
        return race != null ? race : ModRaces.NONE.get();
    }

    @Override
    public Element getPrimaryElement(ItemStack stack) {
        return ItemElements.getPrimary(stack, this.getPrimaryElement());
    }

    /** 带元素的变种使用 1.12 迁移来的「.food.元素名」翻译键 */
    @Nonnull
    @Override
    public String getDescriptionId(@Nonnull ItemStack stack) {
        final Element element = this.getPrimaryElement(stack);
        final ResourceLocation id = ModElements.registry().getKey(element);
        if (element.isNone() || id == null) {
            return super.getDescriptionId(stack);
        }
        return this.getDescriptionId() + ".food." + id.getPath();
    }

    @Nonnull
    @Override
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        final Element element = this.getPrimaryElement(stack);
        final ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && TrinketsConfig.SERVER.food.transformationEffects.get()) {
            final EntityProperties properties = EntityProperties.get(entity);
            if (properties != null) {
                properties.setImbuedRaceCache(new RaceCache(this.getRace(), element));
            }
        }
        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return result;
    }
}
