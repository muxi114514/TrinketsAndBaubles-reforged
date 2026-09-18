package xzeroair.trinkets.attributes;

import java.util.List;

import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * 一条属性配置解析结果：作用的属性（1.12 的部分名称对应多个 1.20.1 属性，如触及距离）、数值、运算方式、是否存盘。
 * 对应 1.12 ConfigHelper.AttributeEntry。
 *
 * @param name       配置里写的属性名（用于拼修饰器名称）
 * @param operation  0 加法 / 1 乘基础值 / 2 乘总值
 */
public record AttributeEntry(String name, List<Attribute> attributes, double amount, int operation, boolean saved) {
}
