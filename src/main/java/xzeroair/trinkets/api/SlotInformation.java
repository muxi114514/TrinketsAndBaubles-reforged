package xzeroair.trinkets.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * 能力来源的槽位标识：来自哪个容器、第几格、对应哪个物品栈。
 * 移植说明：原版是 TrinketHelper 的嵌套类，此处提出为独立类型。
 */
public class SlotInformation {

    public static final SlotInformation EMPTY = new SlotInformation();

    protected int slot = -1;
    protected String handler;
    /** Curios 槽位类型（ring、head…），仅 TRINKETS 容器使用 */
    protected String curioSlot = "";
    protected ItemStack stack;
    protected boolean hasChanged;

    public SlotInformation() {
        this(ItemStack.EMPTY, ItemHandlerType.NONE, -1);
    }

    public SlotInformation(ItemHandlerType handler) {
        this(handler.getName(), -1);
    }

    public SlotInformation(ItemHandlerType handler, int slot) {
        this(handler.getName(), slot);
    }

    public SlotInformation(String handler, int slot) {
        this(ItemStack.EMPTY, handler, slot);
    }

    public SlotInformation(ItemStack stack, ItemHandlerType handler, int slot) {
        this(stack, handler.getName(), slot);
    }

    public SlotInformation(ItemStack stack, String handler, int slot) {
        this.stack = stack == null ? ItemStack.EMPTY : stack;
        this.setChanged(false);
        this.setSlot(slot);
        this.setHandler(handler);
    }

    /** Curios 槽位来源：槽位类型 + 该类型下的序号 */
    public static SlotInformation curio(ItemStack stack, String identifier, int index) {
        final SlotInformation info = new SlotInformation(stack, ItemHandlerType.TRINKETS, index);
        info.curioSlot = identifier == null ? "" : identifier;
        return info;
    }

    public String getCurioSlot() {
        return this.curioSlot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return this.slot;
    }

    public void setHandler(ItemHandlerType handler) {
        this.setHandler(handler.getName());
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getHandler() {
        return this.handler;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack;
    }

    public boolean changed() {
        return this.hasChanged;
    }

    public void setChanged() {
        this.setChanged(true);
    }

    public void setChanged(boolean value) {
        this.hasChanged = value;
    }

    public ItemHandlerType getHandlerType() {
        return ItemHandlerType.byName(this.handler);
    }

    public String getItemID() {
        final ResourceLocation id = ForgeRegistries.ITEMS.getKey(this.stack.getItem());
        return id == null ? "" : id.toString();
    }

    /**
     * 按本槽位的容器类型从实体身上取回对应物品栈。
     * 移植说明：1.12 的 TRINKETS/BAUBLES 两种容器在 1.20.1 由 Curios 统一承担，按「槽位类型 + 序号」定位。
     */
    public ItemStack getStackFromHandler(LivingEntity entity) {
        switch (this.getHandlerType()) {
            case MAINHAND:
                return entity.getMainHandItem();
            case OFFHAND:
                return entity.getOffhandItem();
            case HEAD:
                return entity.getItemBySlot(EquipmentSlot.HEAD);
            case CHEST:
                return entity.getItemBySlot(EquipmentSlot.CHEST);
            case LEGS:
                return entity.getItemBySlot(EquipmentSlot.LEGS);
            case FEET:
                return entity.getItemBySlot(EquipmentSlot.FEET);
            case INVENTORY:
            case HOTBAR:
                if (entity instanceof Player player && this.slot >= 0 && this.slot < player.getInventory().getContainerSize()) {
                    return player.getInventory().getItem(this.slot);
                }
                return ItemStack.EMPTY;
            case TRINKETS:
            case BAUBLES:
                return this.getCurioStack(entity);
            default:
                return ItemStack.EMPTY;
        }
    }

    private ItemStack getCurioStack(LivingEntity entity) {
        if (this.curioSlot.isEmpty() || this.slot < 0) {
            return ItemStack.EMPTY;
        }
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(inventory -> inventory.getStacksHandler(this.curioSlot))
                .map(handler -> this.slot < handler.getSlots() ? handler.getStacks().getStackInSlot(this.slot) : ItemStack.EMPTY)
                .orElse(ItemStack.EMPTY);
    }

    public boolean compare(SlotInformation other) {
        return other != null
                && this.getHandlerType() == other.getHandlerType()
                && this.getSlot() == other.getSlot()
                && this.curioSlot.equals(other.curioSlot);
    }
}
