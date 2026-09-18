package xzeroair.trinkets.blocks;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import xzeroair.trinkets.items.base.TrinketData;

/**
 * 泰迪熊的彩蛋外观（对应 1.12 enums/CustomTeddyBearTypes）。按物品自定义名称或制作者决定，名称优先。
 *
 * 移植说明：1.12 为每种外观各注册一个方块（teddy_bear_rembo 等 15 个）；1.20.1 合并为一个方块的 variant 属性。
 * 1.12 另按「制作者 UUID」匹配几位赞助者，1.20.1 保留该规则（只在物品未改名时生效）。
 */
public enum TeddyBearType implements StringRepresentable {

    NORMAL("normal"),
    REMBO("rembo"),
    SCARY("scary"),
    SHIVAXI("shivaxi"),
    BEE("bee"),
    PANDA("panda"),
    ARTSY("artsy"),
    TWILIGHT("twilight"),
    RYU("ryu"),
    KEN("ken"),
    NYAN("nyan"),
    NYAN_OLD("nyan_old"),
    BOOM("boom"),
    RIXXI("rixxi"),
    SNOWIE("snowie");

    private final String name;

    TeddyBearType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static TeddyBearType byId(int id) {
        final TeddyBearType[] values = values();
        return id >= 0 && id < values.length ? values[id] : NORMAL;
    }

    /** 判定顺序与 1.12 getTeddyVariant 一致 */
    public static TeddyBearType of(ItemStack stack) {
        if (stack.isEmpty()) {
            return NORMAL;
        }
        final boolean renamed = stack.hasCustomHoverName();
        final String name = (renamed ? stack.getHoverName().getString() : TrinketData.getCrafter(stack)).toLowerCase(Locale.ROOT);
        final String crafter = renamed ? "" : TrinketData.getCrafterUUID(stack);
        if (name.equals("stephanie's snowie")) {
            return SNOWIE;
        }
        if (crafter.equals("b4817e56-db30-4fdb-9c4c-bba3e0378e0a") || name.contains("rixxi")) {
            return RIXXI;
        }
        if (crafter.equals("6e6cc84d-6d4d-41a4-ba8b-47d786b00bae") || name.contains("cowsaysboom")) {
            return BOOM;
        }
        if (name.contains("nyan")) {
            return NYAN_OLD;
        }
        if (crafter.equals("854adc0b-ae55-48d6-b7ba-e641a1eebf42")) {
            return NYAN;
        }
        if (name.contains("ken")) {
            return KEN;
        }
        if (name.contains("ryu")) {
            return RYU;
        }
        if (crafter.equals("b2b629a6-454e-4047-a143-4f357171d639") || name.contains("twilight")) {
            return TWILIGHT;
        }
        if (crafter.equals("14bba455-affa-46d0-9cf0-806cc0f3d454") || name.contains("artsy")) {
            return ARTSY;
        }
        if (crafter.equals("5a215d65-e57d-47c6-a322-9ede12a4a100") || name.contains("potastic") || name.contains("panda")) {
            return PANDA;
        }
        if (crafter.equals("7d50a302-a01c-4e6a-8ea4-03f98662df28") || name.contains("stingin") || name.contains("bee")
                || name.contains("bzzz")) {
            return BEE;
        }
        if (crafter.equals("cdfccefb-1a2e-4fb8-a3b5-041da27fde61") || name.contains("shivaxi")) {
            return SHIVAXI;
        }
        if (crafter.equals("6b5d5e9b-1fe8-4c61-a043-1d84ce95765d") || name.contains("rembo") || name.contains("cool")
                || name.contains("badass")) {
            return REMBO;
        }
        if (name.contains("scary") || name.contains("freddy") || name.contains("snuggles")) {
            return SCARY;
        }
        return NORMAL;
    }
}
