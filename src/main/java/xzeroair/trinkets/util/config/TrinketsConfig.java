package xzeroair.trinkets.util.config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * 配置注册入口。
 *
 * 移植说明：1.12 的 @Config 注解 + Configuration 手写解析（ConfigHelper 43KB）
 * 与 ConstantsConfigLang（1187 个 category/name/comment 字符串常量，118KB）在 1.20.1 全部作废——
 * ForgeConfigSpec 的 .comment()/.translation() 在定义处内联声明，翻译键走 assets/xat/lang/*.json。
 */
public class TrinketsConfig {

    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    public static final CommonConfig COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<ServerConfig, ForgeConfigSpec> server =
                new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER = server.getLeft();
        SERVER_SPEC = server.getRight();

        final Pair<CommonConfig, ForgeConfigSpec> common =
                new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();

        final Pair<ClientConfig, ForgeConfigSpec> client =
                new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
    }

    public static void register(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    private TrinketsConfig() {
    }
}
