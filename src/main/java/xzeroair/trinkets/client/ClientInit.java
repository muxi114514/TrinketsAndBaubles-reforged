package xzeroair.trinkets.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoadingContext;

import xzeroair.trinkets.client.compat.EnhancedVisualsHandler;
import xzeroair.trinkets.client.gui.config.TrinketsConfigScreen;
import xzeroair.trinkets.util.compat.ModCompat;

/**
 * 模组构造期的客户端初始化：配置界面入口与客户端联动接缝。由主类经 DistExecutor 调用，专用服务器不会加载本类。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientInit {

    public static void init(ModLoadingContext context) {
        TrinketsConfigScreen.register(context);
        if (ModCompat.isLoaded(ModCompat.ENHANCED_VISUALS)) {
            EnhancedVisualsHandler.register();
        }
    }

    private ClientInit() {
    }
}
