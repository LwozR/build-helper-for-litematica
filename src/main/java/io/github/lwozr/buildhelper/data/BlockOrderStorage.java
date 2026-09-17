package io.github.lwozr.buildhelper.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import io.github.lwozr.buildhelper.Reference;

public class BlockOrderStorage
{
    private static final String FILE_NAME = Reference.MOD_ID + "_block_order.json";
    @Nullable private static JsonObject root;

    private static Path getFile()
    {
        return FileUtils.getConfigDirectory().resolve(FILE_NAME);
    }

    private static JsonObject getRoot()
    {
        if (root == null)
        {
            Path file = getFile();
            JsonElement element = Files.exists(file) ? JsonUtils.parseJsonFile(file) : null;
            root = element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        }

        return root;
    }

    private static void save()
    {
        Path dir = FileUtils.getConfigDirectory();

        if (Files.exists(dir) == false)
        {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        JsonUtils.writeJsonToFile(getRoot(), getFile());
    }

    @Nullable
    public static String getSchematicKey()
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        SchematicPlacement placement = manager.getSelectedSchematicPlacement();

        if (placement == null)
        {
            for (SchematicPlacement p : manager.getAllSchematicsPlacements())
            {
                if (p.isEnabled())
                {
                    placement = p;
                    break;
                }
            }
        }

        if (placement == null)
        {
            return null;
        }

        Path file = placement.getSchematicFile();
        return file != null ? file.getFileName().toString() : placement.getName();
    }

    public static List<Item> getOrder(@Nullable String schematicKey, String layerKey)
    {
        List<Item> list = new ArrayList<>();

        if (schematicKey == null)
        {
            return list;
        }

        JsonObject schematic = getRoot().has(schematicKey) ? getRoot().getAsJsonObject(schematicKey) : null;

        if (schematic == null || schematic.has(layerKey) == false)
        {
            return list;
        }

        for (JsonElement element : schematic.getAsJsonArray(layerKey))
        {
            Identifier id = Identifier.tryParse(element.getAsString());
            Item item = id != null ? BuiltInRegistries.ITEM.getValue(id) : Items.AIR;

            if (item != Items.AIR && list.contains(item) == false)
            {
                list.add(item);
            }
        }

        return list;
    }

    public static void setOrder(@Nullable String schematicKey, String layerKey, List<Item> order)
    {
        if (schematicKey == null)
        {
            return;
        }

        JsonObject schematic = getRoot().has(schematicKey) ? getRoot().getAsJsonObject(schematicKey) : new JsonObject();
        JsonArray array = new JsonArray();

        for (Item item : order)
        {
            array.add(BuiltInRegistries.ITEM.getKey(item).toString());
        }

        schematic.add(layerKey, array);
        getRoot().add(schematicKey, schematic);
        save();
    }

    public static void clearOrder(@Nullable String schematicKey, String layerKey)
    {
        if (schematicKey == null || getRoot().has(schematicKey) == false)
        {
            return;
        }

        getRoot().getAsJsonObject(schematicKey).remove(layerKey);
        save();
    }
}
