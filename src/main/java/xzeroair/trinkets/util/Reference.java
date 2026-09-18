package xzeroair.trinkets.util;

/**
 * 模组全局常量。
 * 移植说明：1.12 版本中的 proxy 类名 / GUIFACTORY / DEPENDENCIES / FINGERPRINT 均为
 * 1.12 FML 专属机制，1.20.1 分别由 DistExecutor、ConfigScreenHandler、mods.toml 取代，故不再保留。
 * 配置文件名沿用 Forge 默认（xat-server.toml / xat-client.toml）——1.12 的 .cfg 与 1.20.1 的 .toml
 * 格式不兼容，无迁移价值，故不再自定义路径。
 */
public class Reference {

    public static final String MODID = "xat";
    public static final String NAME = "Trinkets and Baubles";
    public static final String RESOURCE_PREFIX = MODID + ":";

    private Reference() {
    }
}
