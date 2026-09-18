package xzeroair.trinkets.init;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.blocks.entity.MoonRoseBlockEntity;
import xzeroair.trinkets.blocks.entity.TeddyBearBlockEntity;
import xzeroair.trinkets.util.Reference;

/** 方块实体类型注册。对应 1.12 ModBlocks 中的 GameRegistry.registerTileEntity。 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Reference.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<TeddyBearBlockEntity>> TEDDY_BEAR = BLOCK_ENTITIES.register("teddy_bear",
            () -> BlockEntityType.Builder.of(TeddyBearBlockEntity::new, ModBlocks.TEDDY_BEAR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MoonRoseBlockEntity>> MOON_ROSE = BLOCK_ENTITIES.register("moon_rose",
            () -> BlockEntityType.Builder.of(MoonRoseBlockEntity::new, ModBlocks.MOON_ROSE.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    private ModBlockEntities() {
    }
}
