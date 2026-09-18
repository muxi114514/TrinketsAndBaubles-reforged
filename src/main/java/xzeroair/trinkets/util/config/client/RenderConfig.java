package xzeroair.trinkets.util.config.client;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

/**
 * 客户端渲染开关（对应 1.12 ClientConfig 顶层 RENDERING / CAMERA_HEIGHT）。
 * 以及各饰品的佩戴渲染开关（对应 1.12 ClientConfig.ITEMS.XXX.RENDER）。
 */
public class RenderConfig {

    public final BooleanValue rendering;
    public final BooleanValue cameraHeight;
    public final BooleanValue renderElements;
    public final BooleanValue damageShield;
    public final BooleanValue enderCrown;
    public final BooleanValue enderCrownHelmet;
    public final BooleanValue seaStone;
    public final BooleanValue faelisClaw;

    public RenderConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Rendering settings")
                .translation("xat.config.client.rendering")
                .push("rendering");

        rendering = builder
                .comment("Render this mod's race models and trinket layers")
                .translation("xat.config.client.rendering.enabled")
                .define("rendering", true);

        cameraHeight = builder
                .comment("Adjust camera height to match the transformed race's size")
                .translation("xat.config.client.camera.height")
                .define("cameraHeight", true);

        renderElements = builder
                .comment("Render element indicators on items")
                .translation("xat.config.client.items.elements")
                .define("renderElements", true);

        damageShield = builder
                .comment("Render the Shield of Honor on the wearer")
                .translation("xat.config.client.rendering.damage_shield")
                .define("damageShield", true);

        enderCrown = builder
                .comment("Render the Ender Queen's Crown on the wearer")
                .translation("xat.config.client.rendering.ender_crown")
                .define("enderCrown", true);

        enderCrownHelmet = builder
                .comment("Use the 3D crown model when the crown is worn in the helmet slot")
                .translation("xat.config.client.rendering.ender_crown_helmet")
                .define("enderCrownHelmet", true);

        seaStone = builder
                .comment("Render the Stone of the Sea on the wearer")
                .translation("xat.config.client.rendering.sea_stone")
                .define("seaStone", true);

        faelisClaw = builder
                .comment("Render Faelis Claws on the wearer")
                .translation("xat.config.client.rendering.faelis_claw")
                .define("faelisClaw", true);

        builder.pop();
    }
}
