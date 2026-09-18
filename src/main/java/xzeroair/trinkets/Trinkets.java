package xzeroair.trinkets;

import com.mojang.logging.LogUtils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import org.slf4j.Logger;

import xzeroair.trinkets.capabilities.Capabilities;
import xzeroair.trinkets.capabilities.CapabilitiesHandler;
import xzeroair.trinkets.client.ClientInit;
import xzeroair.trinkets.commands.TrinketsCommand;
import xzeroair.trinkets.events.AccessoryEventHandler;
import xzeroair.trinkets.events.BlockBreakHandler;
import xzeroair.trinkets.events.BowHandler;
import xzeroair.trinkets.events.CapabilityEventHandler;
import xzeroair.trinkets.events.CombatHandler;
import xzeroair.trinkets.events.EnderQueenHandler;
import xzeroair.trinkets.events.InteractionHandler;
import xzeroair.trinkets.events.ItemUseHandler;
import xzeroair.trinkets.events.MovementHandler;
import xzeroair.trinkets.events.PlayerEventHandler;
import xzeroair.trinkets.events.PotionEventHandler;
import xzeroair.trinkets.events.RaceSizeHandler;
import xzeroair.trinkets.init.ModAttributes;
import xzeroair.trinkets.init.ModBlockEntities;
import xzeroair.trinkets.init.ModBlocks;
import xzeroair.trinkets.init.ModCreativeTabs;
import xzeroair.trinkets.init.ModEffects;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.init.ModEntities;
import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.init.ModLootModifiers;
import xzeroair.trinkets.init.ModPotions;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.init.ModSounds;
import xzeroair.trinkets.network.NetworkHandler;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.compat.SimpleDifficultyCompat;
import xzeroair.trinkets.util.compat.firstaid.FirstAidBridge;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.recipes.ElementIngredient;

/**
 * 模组主类。
 *
 * 移植说明：1.12 的 preInit/init/postInit 三段式与 @Instance 单例在 1.20.1 已废弃——
 * 注册改由 DeferredRegister 挂到 mod 事件总线完成，运行期事件走 Forge 事件总线，
 * 客户端专属初始化由 FMLClientSetupEvent 承担（取代 CommonProxy/ClientProxy）。
 *
 * 两条总线别搞混：RegisterCapabilitiesEvent 在 mod 总线，AttachCapabilitiesEvent 在 Forge 总线。
 */
@Mod(Reference.MODID)
public class Trinkets {

    public static final Logger LOGGER = LogUtils.getLogger();

    public Trinkets() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 配置须在其他注册之前就绪：注册开关（如食物 registry enabled）在构造期即被读取
        TrinketsConfig.register(ModLoadingContext.get());
        // 配置界面与客户端联动（客户端类，经 DistExecutor 隔离，专用服务器不加载）
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientInit.init(ModLoadingContext.get()));

        // ── 注册表（随移植分期逐步补充：方块 / 实体 / 药水 / 音效 / 网络 ...）──
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        ModElements.register(modEventBus);
        ModRaces.register(modEventBus);
        ModAttributes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModLootModifiers.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        // 自定义配方配料与原版/Forge 配料同一时机登记（Forge 自身在 RECIPE_SERIALIZERS 注册事件里登记）
        modEventBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS)) {
                CraftingHelper.register(ElementIngredient.ID, ElementIngredient.Serializer.INSTANCE);
            }
        });
        modEventBus.addListener(Capabilities::register);
        // 种族的体型与亲和在注册期先用默认值，待 SERVER 配置加载后覆盖
        modEventBus.addListener(ModRaces::onConfigLoad);

        // ── 运行期事件处理器（Forge 总线）──
        MinecraftForge.EVENT_BUS.register(new CapabilitiesHandler());
        MinecraftForge.EVENT_BUS.register(new CapabilityEventHandler());
        MinecraftForge.EVENT_BUS.register(new RaceSizeHandler());
        // 能力与种族钩子的事件分发
        MinecraftForge.EVENT_BUS.register(new CombatHandler());
        MinecraftForge.EVENT_BUS.register(new MovementHandler());
        MinecraftForge.EVENT_BUS.register(new BlockBreakHandler());
        MinecraftForge.EVENT_BUS.register(new PotionEventHandler());
        MinecraftForge.EVENT_BUS.register(new ItemUseHandler());
        MinecraftForge.EVENT_BUS.register(new InteractionHandler());
        MinecraftForge.EVENT_BUS.register(new BowHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
        MinecraftForge.EVENT_BUS.register(new AccessoryEventHandler());
        MinecraftForge.EVENT_BUS.register(new EnderQueenHandler());
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> TrinketsCommand.register(event.getDispatcher()));
        registerCompat();
    }

    /** 联动接缝只在对应模组已加载时调用，接缝类因此不会在缺少该模组时被类加载 */
    private static void registerCompat() {
        if (ModCompat.isLoaded(ModCompat.FIRST_AID)) {
            FirstAidBridge.register();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::init);
        // 其它模组的监听在各自构造期注册，这里（全部构造完成后）才能移除
        event.enqueueWork(SimpleDifficultyCompat::removeBrokenCuriosListener);
        event.enqueueWork(ModPotions::registerBrewing);
    }
}
