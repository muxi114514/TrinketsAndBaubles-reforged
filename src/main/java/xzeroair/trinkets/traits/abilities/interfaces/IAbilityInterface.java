package xzeroair.trinkets.traits.abilities.interfaces;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import xzeroair.trinkets.traits.AbilityHandler.AbilityHolder;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.elements.Element;

/**
 * 能力的统一契约。
 * 移植说明：1.12 的 getDisplayName 用 String，1.20.1 改为 Component；
 * getDescription 直接拼客户端文本，改为只提供占位符取值的 describe（见 DescriptionVariables）。
 */
public interface IAbilityInterface {

    boolean isAbilityEnabled();

    IAbilityInterface setAbilityEnabled(boolean enabled);

    boolean isFirstUpdate();

    IAbilityInterface setFirstUpdate(boolean firstUpdate);

    ResourceLocation getRegistryName();

    Component getDisplayName();

    String getTranslationKey();

    String getUUID();

    /** 填写说明文本（语言键 能力键.tooltip1~10）里占位符的取值，由客户端 tooltip 层翻译替换 */
    default void describe(DescriptionVariables variables) {
    }

    /**
     * 填写实时状态（语言键 能力键.status1~10）：仅对玩家身上正在生效的实例、或状态存于来源物品的能力调用。
     *
     * @param source 来源物品；种族能力为空栈
     */
    default void describeStatus(DescriptionVariables variables, ItemStack source) {
    }

    /** 主要法力消耗；无消耗返回 null */
    @Nullable
    default ManaCost getManaCost() {
        return null;
    }

    /** 该能力依赖的联动模组 id（说明中归入「联动」分组）；原生能力返回 null */
    @Nullable
    default String getCompatModId() {
        return null;
    }

    /** 是否为主动能力（由按键或特定输入触发）；默认有绑定按键即为主动 */
    default boolean isActiveAbility() {
        return this instanceof IKeyBindInterface keyBind && !keyBind.getKey().isEmpty();
    }

    /** 能力被添加到实体上时调用 */
    default void onAbilityAdded(LivingEntity entity) {
    }

    /** 能力从实体上移除时调用 */
    default void onAbilityRemoved(LivingEntity entity) {
    }

    IAbilityInterface setRequiredElement(Element requiredElement);

    @Nullable
    Element getRequiredElement();

    default boolean shouldRemove() {
        return false;
    }

    boolean hasChanged();

    IAbilityInterface setChanged(boolean value);

    default IAbilityInterface scheduleRemoval() {
        return this;
    }

    default AbilityHolder getAbilityHolder() {
        return null;
    }

    default IAbilityInterface cacheAbilityHolder(AbilityHolder holder) {
        return this;
    }

    default void loadStorage(CompoundTag compound) {
    }

    default CompoundTag saveStorage(CompoundTag compound) {
        return compound;
    }

    /** 变更时下发的临时数据 */
    @Nullable
    CompoundTag sendAbilityData();

    /** 接收变更时下发的临时数据 */
    default void loadDataCache(CompoundTag tag) {
    }
}
