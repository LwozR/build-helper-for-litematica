package io.github.lwozr.buildhelper.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import io.github.lwozr.buildhelper.config.Configs;
import io.github.lwozr.buildhelper.recipe.MaterialUsage;

public class ContainerHighlighter
{
    public static void render(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen)
    {
        if (Configs.Generic.CONTAINER_HIGHLIGHT.getBooleanValue() == false || SchematicWorldHandler.getSchematicWorld() == null)
        {
            return;
        }

        int direct = Configs.Colors.CONTAINER_DIRECT_COLOR.getIntegerValue();
        int ingredient = Configs.Colors.CONTAINER_INGREDIENT_COLOR.getIntegerValue();

        for (Slot slot : screen.getMenu().slots)
        {
            if (slot.hasItem() == false || slot.container instanceof Inventory)
            {
                continue;
            }

            MaterialUsage.Usage usage = MaterialUsage.get(slot.getItem().getItem());

            if (usage.isEmpty())
            {
                continue;
            }

            int color = usage.direct() > 0 ? direct : ingredient;
            int x = slot.x;
            int y = slot.y;

            graphics.fill(x, y, x + 16, y + 16, (color & 0x00FFFFFF) | 0x30000000);
            graphics.fill(x - 1, y - 1, x + 17, y, color);
            graphics.fill(x - 1, y + 16, x + 17, y + 17, color);
            graphics.fill(x - 1, y, x, y + 16, color);
            graphics.fill(x + 16, y, x + 17, y + 16, color);
        }
    }
}
