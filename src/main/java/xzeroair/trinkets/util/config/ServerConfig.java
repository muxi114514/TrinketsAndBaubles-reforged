package xzeroair.trinkets.util.config;

import net.minecraftforge.common.ForgeConfigSpec;

import xzeroair.trinkets.util.config.server.BlocksConfig;
import xzeroair.trinkets.util.config.server.CompatConfig;
import xzeroair.trinkets.util.config.server.FoodConfig;
import xzeroair.trinkets.util.config.server.ItemsConfig;
import xzeroair.trinkets.util.config.server.ManaConfig;
import xzeroair.trinkets.util.config.server.MiscConfig;
import xzeroair.trinkets.util.config.server.RacesConfig;

/**
 * 服务端配置根（对应 1.12 ServerConfig）。
 *
 * 移植说明：1.12 没有原生配置同步，原版靠 writeConfigMap/readConfigMap 手写 NBT 下发，
 * 客户端再从 ClientConfigStore 读副本，于是业务代码里到处是
 * 「world.isRemote ? getClientStore().X : SERVER.X」的双读。
 * 1.20.1 的 ModConfig.Type.SERVER 由 Forge 在玩家登录时自动同步，两端 .get() 读到同一份值，
 * 故 ClientConfigStore 与那套同步代码整套不移植。
 */
public class ServerConfig {

    public final MiscConfig misc;
    public final FoodConfig food;
    public final RacesConfig races;
    public final ManaConfig magic;
    public final ItemsConfig items;
    public final BlocksConfig blocks;
    public final CompatConfig compat;

    // ── 随移植分期补入：GUI（1.12 的 ELEMENTS 伤害名表已改为伤害类型标签）──

    ServerConfig(ForgeConfigSpec.Builder builder) {
        this.misc = new MiscConfig(builder);
        this.food = new FoodConfig(builder);
        this.races = new RacesConfig(builder);
        this.magic = new ManaConfig(builder);
        this.items = new ItemsConfig(builder);
        this.blocks = new BlocksConfig(builder);
        this.compat = new CompatConfig(builder);
    }
}
