package io.github.lwozr.buildhelper.render;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.world.item.Item;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import io.github.lwozr.buildhelper.BuildHelperMod;

public class IgnoredMaterials
{
    @Nullable private static final Field IGNORED_FIELD = findField(MaterialListBase.class, "ignored");
    @Nullable private static final Field PLACEMENT_LIST_FIELD = findField(SchematicPlacement.class, "materialList");

    @Nullable
    private static Field findField(Class<?> clazz, String name)
    {
        try
        {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
        catch (Exception e)
        {
            BuildHelperMod.LOGGER.warn("Could not access {}.{}, ignored materials will not be excluded", clazz.getSimpleName(), name);
            return null;
        }
    }

    public static Set<Item> collect()
    {
        Set<Item> items = new HashSet<>();

        if (IGNORED_FIELD == null)
        {
            return items;
        }

        addFrom(DataManager.getMaterialList(), items);

        if (PLACEMENT_LIST_FIELD != null)
        {
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
            {
                try
                {
                    addFrom((MaterialListBase) PLACEMENT_LIST_FIELD.get(placement), items);
                }
                catch (Exception ignored)
                {
                }
            }
        }

        return items;
    }

    private static void addFrom(@Nullable MaterialListBase list, Set<Item> items)
    {
        if (list == null)
        {
            return;
        }

        try
        {
            for (Object entry : (Collection<?>) IGNORED_FIELD.get(list))
            {
                items.add(((MaterialListEntry) entry).getStack().getItem());
            }
        }
        catch (Exception ignored)
        {
        }
    }
}
