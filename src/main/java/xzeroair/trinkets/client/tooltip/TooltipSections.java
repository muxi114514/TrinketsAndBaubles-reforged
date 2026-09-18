package xzeroair.trinkets.client.tooltip;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 分组的说明文本：每组有标题、常显行（简短数值，如体型、属性）、详情行与能力条目。
 * 折叠时每组只显示标题（带能力名列表）与常显行，展开时显示全部。
 */
@OnlyIn(Dist.CLIENT)
public final class TooltipSections {

    /** 折叠时每行最多列出的能力名数量 */
    private static final int NAMES_PER_LINE = 4;

    private record Entry(String name, boolean disabled, List<Component> lines) {
    }

    private static final class Group {

        private final String title;
        private final List<Component> summary = new ArrayList<>();
        private final List<Component> details = new ArrayList<>();
        private final List<Entry> entries = new ArrayList<>();

        private Group(String title) {
            this.title = title;
        }

        private boolean isEmpty() {
            return this.summary.isEmpty() && this.details.isEmpty() && this.entries.isEmpty();
        }
    }

    private final Map<String, Group> groups = new LinkedHashMap<>();

    /** 预先登记分组以固定显示顺序（空分组不显示） */
    public TooltipSections group(String title) {
        this.groups.computeIfAbsent(title, Group::new);
        return this;
    }

    public void addSummary(String title, List<Component> lines) {
        this.groups.computeIfAbsent(title, Group::new).summary.addAll(lines);
    }

    public void addDetails(String title, List<Component> lines) {
        this.groups.computeIfAbsent(title, Group::new).details.addAll(lines);
    }

    /** lines 为展开时的完整内容（标题行 + 说明 + 状态） */
    public void addEntry(String title, String name, boolean disabled, List<Component> lines) {
        this.groups.computeIfAbsent(title, Group::new).entries.add(new Entry(name, disabled, lines));
    }

    public void render(List<Component> out, boolean collapsed) {
        boolean hidden = false;
        for (Group group : this.groups.values()) {
            if (group.isEmpty()) {
                continue;
            }
            if (collapsed) {
                renderCollapsed(group, out);
                hidden |= !group.details.isEmpty() || !group.entries.isEmpty();
            } else {
                out.add(Component.literal(TooltipText.SECTION + "◆ " + group.title));
                out.addAll(group.summary);
                out.addAll(group.details);
                group.entries.forEach(entry -> out.addAll(entry.lines()));
            }
        }
        if (hidden) {
            out.add(Component.literal(TooltipText.MUTED + I18n.get("xat.tooltip.expand")));
        }
    }

    private static void renderCollapsed(Group group, List<Component> out) {
        final String title = TooltipText.SECTION + group.title;
        if (group.entries.isEmpty()) {
            out.add(Component.literal(title));
        } else {
            for (int start = 0; start < group.entries.size(); start += NAMES_PER_LINE) {
                final StringBuilder line = new StringBuilder(start == 0 ? title + TooltipText.MUTED + ": " : TooltipText.DETAIL_INDENT);
                final int end = Math.min(start + NAMES_PER_LINE, group.entries.size());
                for (int i = start; i < end; i++) {
                    final Entry entry = group.entries.get(i);
                    if (i > start) {
                        line.append(TooltipText.MUTED).append(" · ");
                    }
                    line.append(entry.disabled() ? TooltipText.MUTED : TooltipText.NAME).append(entry.name());
                }
                out.add(Component.literal(line.toString()));
            }
        }
        out.addAll(group.summary);
    }
}
