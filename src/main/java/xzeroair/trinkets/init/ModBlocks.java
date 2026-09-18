package xzeroair.trinkets.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.blocks.CooledMagmaBlock;
import xzeroair.trinkets.blocks.MoonRoseBlock;
import xzeroair.trinkets.blocks.TeddyBearBlock;
import xzeroair.trinkets.util.Reference;

/** 方块注册。对应 1.12 init/ModBlocks。 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Reference.MODID);

    /** 仅由能力临时放置，没有对应物品 */
    public static final RegistryObject<Block> COOLED_MAGMA = BLOCKS.register("cooled_magma",
            () -> new CooledMagmaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(0.5F)
                    .randomTicks()
                    .sound(SoundType.STONE)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .pushReaction(PushReaction.NORMAL)));

    /** 放下的泰迪熊；物品是饰品 TrinketTeddyBear，方块本身不对应物品 */
    public static final RegistryObject<TeddyBearBlock> TEDDY_BEAR = BLOCKS.register("teddy_bear",
            () -> new TeddyBearBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .strength(0.8F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistryObject<MoonRoseBlock> MOON_ROSE = BLOCKS.register("moon_rose",
            () -> new MoonRoseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.NONE)
                    .pushReaction(PushReaction.DESTROY)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private ModBlocks() {
    }
}
