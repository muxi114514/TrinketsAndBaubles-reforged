package xzeroair.trinkets.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.util.Reference;

/**
 * 音效注册（对应 1.12 init/ModSounds）。0.33.4 中两段音效只注册、无调用方，保留供附属与后续使用。
 *
 * 移植说明：1.12 注册名是 ara_ara / uwu，sounds.json 却写成 xat.araara / xat.uwu，两者对不上导致播放无声；
 * 此处以注册名为准修正 sounds.json 的键。
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Reference.MODID);

    public static final RegistryObject<SoundEvent> ARA_ARA = register("ara_ara");
    public static final RegistryObject<SoundEvent> UWU = register("uwu");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Reference.MODID, name)));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }

    private ModSounds() {
    }
}
