package xzeroair.trinkets.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.entity.AlphaWolf;
import xzeroair.trinkets.entity.BreathProjectile;
import xzeroair.trinkets.entity.area.AreaEffectEntity;
import xzeroair.trinkets.util.Reference;

/**
 * 实体注册。对应 1.12 init/ModEntities。
 * 移植说明：1.12 注册名含大写（AlphaWolf 等），1.20.1 注册名只允许小写，改为下划线命名。
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Reference.MODID);

    public static final RegistryObject<EntityType<AlphaWolf>> ALPHA_WOLF = ENTITY_TYPES.register("alpha_wolf",
            () -> EntityType.Builder.<AlphaWolf>of(AlphaWolf::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.2F)
                    .clientTrackingRange(10)
                    .build(Reference.MODID + ":alpha_wolf"));

    public static final RegistryObject<EntityType<BreathProjectile>> DRAGON_BREATH = ENTITY_TYPES.register("dragon_breath",
            () -> EntityType.Builder.<BreathProjectile>of(BreathProjectile::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build(Reference.MODID + ":dragon_breath"));

    public static final RegistryObject<EntityType<AreaEffectEntity>> AREA_EFFECT = ENTITY_TYPES.register("area_effect",
            () -> EntityType.Builder.<AreaEffectEntity>of(AreaEffectEntity::new, MobCategory.MISC)
                    .sized(6.0F, 3.0F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(5)
                    .build(Reference.MODID + ":area_effect"));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributeCreation);
    }

    private static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ALPHA_WOLF.get(), Wolf.createAttributes().build());
    }

    private ModEntities() {
    }
}
