package io.github.lwozr.buildhelper.render;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import io.github.lwozr.buildhelper.Reference;
import io.github.lwozr.buildhelper.config.MaterialScope;
import io.github.lwozr.buildhelper.recipe.MaterialUsage;

public class MaterialTooltip
{
    private static final String KEY = Reference.MOD_ID + ".tooltip.";
    private static final int MAX_PRODUCTS = 6;

    private static Component tr(ChatFormatting color, String key, Object... args)
    {
        return Component.literal(StringUtils.translate(KEY + key, args)).withStyle(color);
    }

    public static void append(ItemStack stack, Consumer<Component> list)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || SchematicWorldHandler.getSchematicWorld() == null)
        {
            return;
        }

        MaterialUsage.Usage usage = MaterialUsage.get(stack.getItem());

        if (usage.isEmpty())
        {
            return;
        }

        boolean layer = MaterialUsage.getScope() == MaterialScope.LAYER && BuildScanner.getInstance().isLayerActive();
        String layerLabel = BuildScanner.getInstance().getLayerLabel();

        if (mc.options.advancedItemTooltips == false)
        {
            list.accept(Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
        }

        if (usage.direct() > 0)
        {
            list.accept(layer ? tr(ChatFormatting.GREEN, "needed_layer", layerLabel, usage.direct())
                              : tr(ChatFormatting.GREEN, "needed_schematic", usage.direct()));
        }

        if (usage.products().isEmpty() == false)
        {
            list.accept(layer ? tr(ChatFormatting.GOLD, "makes_layer", layerLabel) : tr(ChatFormatting.GOLD, "makes_schematic"));
            int shown = 0;

            for (MaterialUsage.Product product : usage.products())
            {
                if (shown++ >= MAX_PRODUCTS)
                {
                    list.accept(tr(ChatFormatting.GRAY, "more", usage.products().size() - MAX_PRODUCTS));
                    break;
                }

                String name = new ItemStack(product.item()).getHoverName().getString();
                list.accept(tr(ChatFormatting.YELLOW, "product", product.missing(), name, product.amount()));
            }
        }
    }
}
