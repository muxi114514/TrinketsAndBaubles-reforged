package xzeroair.trinkets.client;

import java.util.List;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import xzeroair.trinkets.blocks.TeddyBearType;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.client.curios.AccessoryRenderers;
import xzeroair.trinkets.client.hud.ManaBarOverlay;
import xzeroair.trinkets.client.keybinds.ClientAbilityInput;
import xzeroair.trinkets.client.keybinds.ModKeyMappings;
import xzeroair.trinkets.client.renderer.AlphaWolfRenderer;
import xzeroair.trinkets.client.renderer.AreaEffectRenderer;
import xzeroair.trinkets.client.renderer.BreathProjectileRenderer;
import xzeroair.trinkets.entity.AlphaWolf;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.init.ModEntities;
import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.items.base.TrinketData;
import xzeroair.trinkets.items.trinkets.TrinketCosmetic;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.traits.elements.ItemElements;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 客户端事件（仅在客户端加载）。对应 1.12 client/events/EventHandlerClient 中的按键与变身禁移部分。
 * 以 @Mod.EventBusSubscriber(value = Dist.CLIENT) 自动注册，专用服务器不会加载本类。
 */
@Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    /** 游戏内（无界面打开）每 tick 末尾：采样能力按键，并在变身途中锁住移动 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            return;
        }
        ClientAbilityInput.tick(player);
        blockMovementWhileTransforming(player, mc.options);
        if (player.getVehicle() instanceof AlphaWolf wolf && player.input.jumping) {
            wolf.setJumping(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAbilityInput.reset();
    }

    /**
     * 配置开启时，变身渐变期间把玩家拉回上一 tick 的水平位置并松开全部移动键。
     * 该配置是 SERVER 类型、已随登录同步到客户端，故客户端可直接读取。
     */
    private static void blockMovementWhileTransforming(LocalPlayer player, Options options) {
        if (!TrinketsConfig.SERVER.misc.preventMovementWhileTransforming.get()) {
            return;
        }
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null || !properties.getRaceHandler().isTransforming()) {
            return;
        }
        if (player.getX() != player.xo || player.getZ() != player.zo) {
            player.setPos(player.xo, player.getY(), player.zo);
        }
        for (KeyMapping key : new KeyMapping[]{options.keyUp, options.keyDown, options.keyLeft, options.keyRight, options.keyJump, options.keyShift}) {
            key.setDown(false);
        }
    }

    /** mod 事件总线上的客户端事件 */
    @Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            for (KeyMapping mapping : ModKeyMappings.all()) {
                event.register(mapping);
            }
        }

        /**
         * 物品模型覆盖属性：龙宝石 / 龙之眼 / 巨龙之戒按元素切换贴图（火 0.25 / 冰 0.5 / 雷 0.75）；
         * 极化石按主副能力开关切换（磁力 0.33 / 排斥 0.66 / 两者 1）；泰迪熊按彩蛋外观（序号/100）；装饰品按特征序号；魔力糖果按堆叠占比。
         */
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                final ResourceLocation element = new ResourceLocation(Reference.MODID, "element");
                for (Item item : List.of(ModItems.DRAGON_GEM.get(), ModItems.DRAGONS_EYE.get(), ModItems.DRAGON_RING.get())) {
                    ItemProperties.register(item, element,
                            (stack, level, entity, seed) -> elementPredicate(ItemElements.getPrimary(stack, ModElements.NEUTRAL.get())));
                }
                ItemProperties.register(ModItems.POLARIZED_STONE.get(), new ResourceLocation(Reference.MODID, "polarity"),
                        (stack, level, entity, seed) -> polarityPredicate(TrinketData.isMainAbility(stack), TrinketData.isAltAbility(stack)));
                ItemProperties.register(ModItems.TEDDY_BEAR.get(), new ResourceLocation(Reference.MODID, "teddy"),
                        (stack, level, entity, seed) -> TeddyBearType.of(stack).ordinal() / 100.0F);
                // 魔力糖果按堆叠占比切换贴图（1.12 Mana_Candy#registerModels）
                // 末影王冠戴在头盔位时换成 3D 冠冕模型（1.12 按槽位切换 _model 变种）
                ItemProperties.register(ModItems.ENDER_TIARA.get(), new ResourceLocation(Reference.MODID, "worn"),
                        (stack, level, entity, seed) -> entity != null && entity.getItemBySlot(EquipmentSlot.HEAD) == stack
                                && TrinketsConfig.CLIENT.render.enderCrownHelmet.get() ? 1.0F : 0.0F);
                AccessoryRenderers.register();
                ItemProperties.register(ModItems.MANA_CANDY.get(), new ResourceLocation(Reference.MODID, "candy"),
                        (stack, level, entity, seed) -> (float) stack.getCount() / stack.getMaxStackSize());
                ItemProperties.register(ModItems.COSMETIC.get(), new ResourceLocation(Reference.MODID, "cosmetic"),
                        (stack, level, entity, seed) -> TrinketCosmetic.getFeature(stack).ordinal());
            });
        }

        private static float polarityPredicate(boolean magnet, boolean repel) {
            if (magnet && repel) {
                return 1.0F;
            }
            return magnet ? 0.33F : repel ? 0.66F : 0.0F;
        }

        private static float elementPredicate(Element element) {
            if (element == ModElements.FIRE.get()) {
                return 0.25F;
            }
            if (element == ModElements.ICE.get()) {
                return 0.5F;
            }
            return element == ModElements.LIGHTNING.get() ? 0.75F : 0.0F;
        }

        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("mana_bar", ManaBarOverlay.INSTANCE);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.ALPHA_WOLF.get(), AlphaWolfRenderer::new);
            event.registerEntityRenderer(ModEntities.DRAGON_BREATH.get(), BreathProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.AREA_EFFECT.get(), AreaEffectRenderer::new);
        }

        private ModBus() {
        }
    }

    private ClientEvents() {
    }
}
