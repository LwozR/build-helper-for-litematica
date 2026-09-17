package io.github.lwozr.buildhelper.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import io.github.lwozr.buildhelper.config.Configs;
import io.github.lwozr.buildhelper.config.MaterialScope;
import io.github.lwozr.buildhelper.render.BuildScanner;
import io.github.lwozr.buildhelper.render.HudRenderer;

public class MaterialUsage
{
    public record Product(Item item, int missing, long amount)
    {
    }

    public record Usage(int direct, List<Product> products)
    {
        public boolean isEmpty()
        {
            return this.direct <= 0 && this.products.isEmpty();
        }
    }

    private static final Usage EMPTY = new Usage(0, List.of());
    private static final long REFRESH_INTERVAL_MS = 500L;
    private static final Map<Item, Usage> CACHE = new HashMap<>();
    private static Map<Item, Integer> needed = Map.of();
    private static long lastRefresh;
    private static int lastVersion = -1;

    public static MaterialScope getScope()
    {
        return (MaterialScope) Configs.Generic.CONTAINER_SCOPE.getOptionListValue();
    }

    private static void refresh()
    {
        Minecraft mc = Minecraft.getInstance();
        BuildScanner scanner = BuildScanner.getInstance();
        long now = System.currentTimeMillis();

        if (scanner.getVersion() == lastVersion && now - lastRefresh < REFRESH_INTERVAL_MS)
        {
            return;
        }

        lastVersion = scanner.getVersion();
        lastRefresh = now;

        Map<Item, Integer> fresh = new HashMap<>();

        if (mc.player != null && SchematicWorldHandler.getSchematicWorld() != null)
        {
            for (Map.Entry<Item, Integer> e : scanner.getScopeMissing(getScope()).entrySet())
            {
                int left = e.getValue() - HudRenderer.countInInventory(mc, e.getKey());

                if (left > 0)
                {
                    fresh.put(e.getKey(), left);
                }
            }
        }

        if (fresh.equals(needed) == false)
        {
            needed = fresh;
            CACHE.clear();
        }
    }

    public static Usage get(Item item)
    {
        refresh();

        if (needed.isEmpty())
        {
            return EMPTY;
        }

        return CACHE.computeIfAbsent(item, MaterialUsage::compute);
    }

    private static Usage compute(Item item)
    {
        int direct = needed.getOrDefault(item, 0);
        List<Product> products = new ArrayList<>();

        if (Configs.Generic.CONTAINER_INGREDIENTS.getBooleanValue())
        {
            for (Map.Entry<Item, Integer> e : needed.entrySet())
            {
                if (e.getKey() == item)
                {
                    continue;
                }

                long amount = RecipeIndex.amountNeeded(item, e.getKey(), e.getValue());

                if (amount > 0)
                {
                    products.add(new Product(e.getKey(), e.getValue(), amount));
                }
            }

            products.sort(Comparator.comparingLong(Product::amount).reversed());
        }

        return direct <= 0 && products.isEmpty() ? EMPTY : new Usage(direct, Collections.unmodifiableList(products));
    }
}
