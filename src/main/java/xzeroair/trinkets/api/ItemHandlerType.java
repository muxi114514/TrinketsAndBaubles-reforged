package xzeroair.trinkets.api;

/**
 * 能力来源的容器类型（对应 1.12 TrinketHelper.SlotInformation.ItemHandlerType）。
 * 移植说明：原版是 TrinketHelper 的嵌套枚举；此处提出为独立类型（SRP），
 * 且 BAUBLES 项因 Curios 接管饰品栏而语义合并到 TRINKETS，保留常量以兼容旧存档读取。
 */
public enum ItemHandlerType {

    NONE(0, "None"),
    RACE(1, "Race"),
    TRINKETS(2, "Trinkets"),
    BAUBLES(3, "Baubles"),
    INVENTORY(4, "Inventory"),
    HOTBAR(5, "Hotbar"),
    HEAD(6, "Head"),
    CHEST(7, "Chest"),
    LEGS(8, "Legs"),
    FEET(9, "Feet"),
    OFFHAND(10, "OffHand"),
    MAINHAND(11, "MainHand"),
    POTION(12, "Potion"),
    OTHER(13, "Other");

    private final int id;
    private final String name;

    ItemHandlerType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public static ItemHandlerType byName(String name) {
        for (ItemHandlerType type : values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return NONE;
    }

    public static ItemHandlerType byID(int value) {
        if (value < 0 || value >= values().length) {
            value = 0;
        }
        return values()[value];
    }
}
