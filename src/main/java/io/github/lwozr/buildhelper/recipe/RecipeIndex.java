package io.github.lwozr.buildhelper.recipe;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import io.github.lwozr.buildhelper.BuildHelperMod;

public class RecipeIndex
{
    private static final int MAX_DEPTH = 3;
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "minecraft:crafting_shaped",
            "minecraft:crafting_shapeless",
            "minecraft:stonecutting",
            "minecraft:smelting",
            "minecraft:blasting");

    private record Recipe(Item result, int count, List<Set<Item>> slots)
    {
    }

    private static final Map<Item, List<Recipe>> BY_RESULT = new HashMap<>();
    private static boolean loaded;

    private static synchronized void ensureLoaded()
    {
        if (loaded)
        {
            return;
        }

        loaded = true;

        try
        {
            PackResources pack = ServerPacksSource.createVanillaPackSource().fullResources();
            Map<String, JsonObject> tagFiles = new HashMap<>();
            List<JsonObject> recipeFiles = new ArrayList<>();

            pack.listResources(PackType.SERVER_DATA, "minecraft", "tags/item", (id, supplier) -> {
                JsonObject json = read(supplier);

                if (json != null)
                {
                    String path = id.getPath();
                    String name = path.substring("tags/item/".length(), path.length() - ".json".length());
                    tagFiles.put("minecraft:" + name, json);
                }
            });

            pack.listResources(PackType.SERVER_DATA, "minecraft", "recipe", (id, supplier) -> {
                JsonObject json = read(supplier);

                if (json != null)
                {
                    recipeFiles.add(json);
                }
            });

            Map<String, Set<Item>> tagCache = new HashMap<>();

            for (JsonObject json : recipeFiles)
            {
                Recipe recipe = parseRecipe(json, tagFiles, tagCache);

                if (recipe != null)
                {
                    BY_RESULT.computeIfAbsent(recipe.result(), k -> new ArrayList<>()).add(recipe);
                }
            }

            BuildHelperMod.LOGGER.info("Loaded {} recipes for {} items", recipeFiles.size(), BY_RESULT.size());
        }
        catch (Exception e)
        {
            BuildHelperMod.LOGGER.warn("Could not load vanilla recipes, ingredient highlighting is disabled", e);
        }
    }

    private static JsonObject read(net.minecraft.server.packs.resources.IoSupplier<InputStream> supplier)
    {
        try (InputStream stream = supplier.get(); Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
        {
            JsonElement element = JsonParser.parseReader(reader);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static Item item(String id)
    {
        Identifier identifier = Identifier.tryParse(id);
        return identifier != null && BuiltInRegistries.ITEM.containsKey(identifier) ? BuiltInRegistries.ITEM.getValue(identifier) : Items.AIR;
    }

    private static Set<Item> resolveTag(String tag, Map<String, JsonObject> tagFiles, Map<String, Set<Item>> cache, Set<String> visiting)
    {
        Set<Item> cached = cache.get(tag);

        if (cached != null)
        {
            return cached;
        }

        Set<Item> items = new LinkedHashSet<>();
        JsonObject json = tagFiles.get(tag.contains(":") ? tag : "minecraft:" + tag);

        if (json != null && visiting.add(tag) && json.has("values"))
        {
            for (JsonElement value : json.getAsJsonArray("values"))
            {
                String entry = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();

                if (entry.startsWith("#"))
                {
                    items.addAll(resolveTag(entry.substring(1), tagFiles, cache, visiting));
                }
                else
                {
                    Item item = item(entry);

                    if (item != Items.AIR)
                    {
                        items.add(item);
                    }
                }
            }
        }

        cache.put(tag, items);
        return items;
    }

    private static Set<Item> parseIngredient(JsonElement element, Map<String, JsonObject> tagFiles, Map<String, Set<Item>> cache)
    {
        Set<Item> items = new LinkedHashSet<>();

        if (element == null)
        {
            return items;
        }

        if (element.isJsonArray())
        {
            for (JsonElement e : element.getAsJsonArray())
            {
                items.addAll(parseIngredient(e, tagFiles, cache));
            }
        }
        else if (element.isJsonObject())
        {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("item"))
            {
                items.add(item(obj.get("item").getAsString()));
            }
            else if (obj.has("tag"))
            {
                items.addAll(resolveTag(obj.get("tag").getAsString(), tagFiles, cache, new HashSet<>()));
            }
        }
        else
        {
            String value = element.getAsString();

            if (value.startsWith("#"))
            {
                items.addAll(resolveTag(value.substring(1), tagFiles, cache, new HashSet<>()));
            }
            else
            {
                items.add(item(value));
            }
        }

        items.remove(Items.AIR);
        return items;
    }

    private static Recipe parseRecipe(JsonObject json, Map<String, JsonObject> tagFiles, Map<String, Set<Item>> cache)
    {
        String type = json.has("type") ? json.get("type").getAsString() : "";

        if (SUPPORTED_TYPES.contains(type) == false || json.has("result") == false)
        {
            return null;
        }

        JsonElement resultElement = json.get("result");
        Item result;
        int count = 1;

        if (resultElement.isJsonObject())
        {
            JsonObject obj = resultElement.getAsJsonObject();
            result = item(obj.has("id") ? obj.get("id").getAsString() : obj.get("item").getAsString());
            count = obj.has("count") ? obj.get("count").getAsInt() : 1;
        }
        else
        {
            result = item(resultElement.getAsString());
        }

        if (result == Items.AIR)
        {
            return null;
        }

        List<Set<Item>> slots = new ArrayList<>();

        switch (type)
        {
            case "minecraft:crafting_shaped" -> {
                JsonObject key = json.getAsJsonObject("key");
                Map<Character, Set<Item>> keyItems = new HashMap<>();

                for (Map.Entry<String, JsonElement> e : key.entrySet())
                {
                    keyItems.put(e.getKey().charAt(0), parseIngredient(e.getValue(), tagFiles, cache));
                }

                JsonArray pattern = json.getAsJsonArray("pattern");

                for (JsonElement row : pattern)
                {
                    for (char c : row.getAsString().toCharArray())
                    {
                        Set<Item> slot = keyItems.get(c);

                        if (slot != null && slot.isEmpty() == false)
                        {
                            slots.add(slot);
                        }
                    }
                }
            }
            case "minecraft:crafting_shapeless" -> {
                for (JsonElement e : json.getAsJsonArray("ingredients"))
                {
                    Set<Item> slot = parseIngredient(e, tagFiles, cache);

                    if (slot.isEmpty() == false)
                    {
                        slots.add(slot);
                    }
                }
            }
            default -> {
                Set<Item> slot = parseIngredient(json.get("ingredient"), tagFiles, cache);

                if (slot.isEmpty() == false)
                {
                    slots.add(slot);
                }
            }
        }

        for (Set<Item> slot : slots)
        {
            if (slot.contains(result))
            {
                return null;
            }
        }

        return slots.isEmpty() ? null : new Recipe(result, count, slots);
    }

    public static long amountNeeded(Item ingredient, Item product, long productCount)
    {
        ensureLoaded();

        if (ingredient == product)
        {
            return 0;
        }

        return amountOf(ingredient, product, productCount, 1, new HashSet<>());
    }

    private static long amountOf(Item ingredient, Item product, long count, int depth, Set<Item> visited)
    {
        List<Recipe> recipes = BY_RESULT.get(product);

        if (recipes == null || depth > MAX_DEPTH || visited.contains(product))
        {
            return 0;
        }

        visited.add(product);

        long best = 0;

        for (Recipe recipe : recipes)
        {
            long crafts = (count + recipe.count() - 1) / recipe.count();
            long total = 0;

            for (Set<Item> slot : recipe.slots())
            {
                if (slot.contains(ingredient))
                {
                    total += crafts;
                    continue;
                }

                long sub = 0;

                for (Item alternative : slot)
                {
                    long value = amountOf(ingredient, alternative, crafts, depth + 1, visited);

                    if (value > 0 && (sub == 0 || value < sub))
                    {
                        sub = value;
                    }
                }

                total += sub;
            }

            if (total > 0 && (best == 0 || total < best))
            {
                best = total;
            }
        }

        visited.remove(product);
        return best;
    }
}
