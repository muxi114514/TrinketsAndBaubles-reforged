package xzeroair.trinkets.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 客户端侧的魔力条显示数据：能力即将消耗的魔力。魔力数值本身存在玩家的 MagicStats 上，这里只存 HUD 附加信息。
 * 写入发生在网络包处理（已切回客户端主线程），读取发生在 HUD 渲染（同一线程），volatile 仅作防御。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientManaCache {

    private static volatile float manaCost;

    public static float getManaCost() {
        return manaCost;
    }

    public static void setManaCost(float cost) {
        manaCost = cost;
    }

    private ClientManaCache() {
    }
}
