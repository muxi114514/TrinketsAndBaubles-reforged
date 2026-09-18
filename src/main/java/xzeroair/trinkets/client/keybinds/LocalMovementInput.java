package xzeroair.trinkets.client.keybinds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.traits.abilities.AbilitySkilledSwimmer;

/** 本地玩家移动按键的快照，供需要直接读按键的能力经 DistExecutor 调用（公共类不得直接引用 Minecraft）。 */
@OnlyIn(Dist.CLIENT)
public final class LocalMovementInput {

    public static AbilitySkilledSwimmer.MovementInput sample() {
        final Options options = Minecraft.getInstance().options;
        return new AbilitySkilledSwimmer.MovementInput(options.keyUp.isDown(), options.keyDown.isDown(),
                options.keyLeft.isDown(), options.keyRight.isDown(), options.keyJump.isDown(), options.keyShift.isDown());
    }

    private LocalMovementInput() {
    }
}
