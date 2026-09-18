package xzeroair.trinkets.init;

import com.mojang.serialization.Codec;

import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.loot.AbilityBlockDropsModifier;
import xzeroair.trinkets.util.Reference;

/**
 * 全局战利品修改器序列化器。实例由数据包 data/xat/loot_modifiers/ 定义，并列入 data/forge/loot_modifiers/global_loot_modifiers.json。
 */
public final class ModLootModifiers {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Reference.MODID);

    public static final RegistryObject<Codec<AbilityBlockDropsModifier>> ABILITY_BLOCK_DROPS =
            LOOT_MODIFIERS.register("ability_block_drops", AbilityBlockDropsModifier.CODEC);

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIERS.register(modEventBus);
    }

    private ModLootModifiers() {
    }
}
