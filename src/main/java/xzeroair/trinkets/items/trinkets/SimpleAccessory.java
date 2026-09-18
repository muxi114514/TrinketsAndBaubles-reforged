package xzeroair.trinkets.items.trinkets;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;

import xzeroair.trinkets.items.base.AccessoryBase;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.config.server.items.AccessoryConfig;

/**
 * 只由「配置 + 元素 + 能力清单」决定行为的饰品。
 *
 * 移植说明：1.12 的失重之石、发光戒指、剧毒之石等 11 个 TrinketXxx 类除 UUID、元素、配置引用与 initAbilities 外完全相同，
 * 收敛为本类，差异在 ModItems 注册处以参数给出；有额外行为的饰品（龙之眼、末影王冠、变身戒指）另有子类。
 *
 * @param <A> 该饰品的能力配置段类型
 */
public class SimpleAccessory<A> extends AccessoryBase {

    private final Supplier<Element> element;
    private final Supplier<AccessoryConfig<A>> config;
    private final BiConsumer<A, List<IAbilityInterface>> abilities;

    public SimpleAccessory(String attributeUUID, Supplier<Element> element, Supplier<AccessoryConfig<A>> config,
            BiConsumer<A, List<IAbilityInterface>> abilities) {
        super(attributeUUID);
        this.element = element;
        this.config = config;
        this.abilities = abilities;
    }

    @Override
    public AccessoryConfig<A> getAccessoryConfig() {
        return this.config.get();
    }

    @Override
    public Element getPrimaryElement() {
        return this.element.get();
    }

    @Override
    public void initAbilities(ItemStack stack, List<IAbilityInterface> list) {
        final A section = this.getAccessoryConfig().abilities;
        if (section != null) {
            this.abilities.accept(section, list);
        }
    }
}
