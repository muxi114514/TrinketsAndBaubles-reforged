package xzeroair.trinkets.capabilities.trinket;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.api.SlotInformation;
import xzeroair.trinkets.capabilities.Capabilities;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.items.base.AccessoryBase;
import xzeroair.trinkets.traits.AbilityHandler;
import xzeroair.trinkets.traits.abilities.compat.SurvivalAbilities;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.abilities.interfaces.IHeldAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableInventoryAbility;
import xzeroair.trinkets.traits.elements.Element;

/**
 * 饰品栈的运行期缓存：本饰品提供的能力实例，并负责把它们登记到持有者的能力处理器。
 *
 * 移植说明：1.12 的 TrinketProperties 同时存 NBT 数据、槽位信息并负责网络同步；1.20.1 中
 * 持久数据已移到 {@link xzeroair.trinkets.items.base.TrinketData}（物品栈 NBT，原版/Curios 自动同步），
 * 槽位由 Curios 的 SlotContext 提供，本类只剩「能力实例缓存 + 登记」一个职责，不参与序列化。
 */
public class TrinketProperties {

    private final ItemStack stack;
    @Nullable
    private List<IAbilityInterface> abilities;
    @Nullable
    private Element abilitiesElement;

    public TrinketProperties(ItemStack stack) {
        this.stack = stack;
    }

    @Nullable
    public static TrinketProperties get(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.TRINKET_PROPERTIES).resolve().orElse(null);
    }

    /** 本饰品提供的能力；元素变化（如龙之眼换形态）时重建 */
    public List<IAbilityInterface> getAbilities() {
        if (!(this.stack.getItem() instanceof AccessoryBase accessory)) {
            return List.of();
        }
        final Element element = accessory.getPrimaryElement(this.stack);
        if (this.abilities == null || this.abilitiesElement != element) {
            final List<IAbilityInterface> built = new ArrayList<>();
            accessory.initAbilities(this.stack, built);
            SurvivalAbilities.addTo(built::add, accessory.getSurvivalConfig(this.stack), null);
            this.abilities = built;
            this.abilitiesElement = element;
        }
        return this.abilities;
    }

    /** 佩戴在饰品栏：登记全部能力，同源已存在则保留旧实例（对应 1.12 onEntityTick） */
    public void registerWorn(LivingEntity entity, SlotInformation info) {
        this.register(entity, info, true);
    }

    /** 放在物品栏或拿在手上：只登记物品栏能力与手持能力（对应 1.12 addAbilitiesToPlayer） */
    public void registerCarried(LivingEntity entity, SlotInformation info) {
        this.register(entity, info, false);
    }

    private void register(LivingEntity entity, SlotInformation info, boolean worn) {
        final EntityProperties properties = EntityProperties.get(entity);
        final ResourceLocation id = ForgeRegistries.ITEMS.getKey(this.stack.getItem());
        if (properties == null || id == null) {
            return;
        }
        final AbilityHandler handler = properties.getAbilityHandler();
        final List<IAbilityInterface> provided = this.getAbilities();
        for (IAbilityInterface ability : provided) {
            final Element required = ability.getRequiredElement();
            if (required != null && required != this.abilitiesElement) {
                continue;
            }
            if (worn) {
                handler.replaceAbility(entity, id.toString(), info, ability);
            } else if (isCarriedAbility(ability, info)) {
                handler.registerAbility(entity, id.toString(), info, ability);
            }
        }
    }

    private static boolean isCarriedAbility(IAbilityInterface ability, SlotInformation info) {
        return switch (info.getHandlerType()) {
            case INVENTORY, HOTBAR -> ability instanceof ITickableInventoryAbility;
            case MAINHAND, OFFHAND -> ability instanceof IHeldAbility;
            default -> false;
        };
    }

    /** 物品栈能力提供者：不序列化，随栈实例创建与销毁 */
    public static class Provider implements ICapabilityProvider {

        private final LazyOptional<TrinketProperties> optional;

        public Provider(ItemStack stack) {
            final TrinketProperties properties = new TrinketProperties(stack);
            this.optional = LazyOptional.of(() -> properties);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == Capabilities.TRINKET_PROPERTIES ? this.optional.cast() : LazyOptional.empty();
        }
    }
}
