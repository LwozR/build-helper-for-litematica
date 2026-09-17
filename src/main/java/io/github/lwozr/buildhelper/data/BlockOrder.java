package io.github.lwozr.buildhelper.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import io.github.lwozr.buildhelper.render.BuildScanner;

public class BlockOrder
{
    public static List<Item> effectiveOrder()
    {
        BuildScanner scanner = BuildScanner.getInstance();
        Map<Item, Integer> items = scanner.getOrderItems();
        Map<Item, Integer> missing = scanner.getOrderMissing();
        List<Item> order = new ArrayList<>();

        for (Item item : BlockOrderStorage.getOrder(BlockOrderStorage.getSchematicKey(), scanner.getLayerOrderKey()))
        {
            if (items.containsKey(item))
            {
                order.add(item);
            }
        }

        List<Item> rest = new ArrayList<>();

        for (Item item : items.keySet())
        {
            if (order.contains(item) == false)
            {
                rest.add(item);
            }
        }

        rest.sort(Comparator.<Item>comparingInt(i -> missing.getOrDefault(i, 0)).reversed()
                            .thenComparing(i -> new ItemStack(i).getHoverName().getString()));
        order.addAll(rest);
        return order;
    }

    public static boolean hasManualOrder()
    {
        BuildScanner scanner = BuildScanner.getInstance();
        return BlockOrderStorage.getOrder(BlockOrderStorage.getSchematicKey(), scanner.getLayerOrderKey()).isEmpty() == false;
    }

    public static void move(Item item, int offset)
    {
        List<Item> order = effectiveOrder();
        int index = order.indexOf(item);
        int target = index + offset;

        if (index < 0 || target < 0 || target >= order.size())
        {
            return;
        }

        order.remove(index);
        order.add(target, item);
        BlockOrderStorage.setOrder(BlockOrderStorage.getSchematicKey(), BuildScanner.getInstance().getLayerOrderKey(), order);
    }

    public static void reset()
    {
        BlockOrderStorage.clearOrder(BlockOrderStorage.getSchematicKey(), BuildScanner.getInstance().getLayerOrderKey());
    }
}
