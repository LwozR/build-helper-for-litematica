package io.github.lwozr.buildhelper.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import io.github.lwozr.buildhelper.Reference;
import io.github.lwozr.buildhelper.config.Configs;

public class HudRenderer
{
    private static final String KEY = Reference.MOD_ID + ".hud.";
    private static final int PAD = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int BAR_HEIGHT = 5;
    private static final int THIN_BAR_HEIGHT = 3;
    private static final int MIN_WIDTH = 120;

    private static String tr(String key, Object... args)
    {
        return StringUtils.translate(KEY + key, args);
    }

    private static int countInInventory(Minecraft mc, Item item)
    {
        Inventory inv = mc.player.getInventory();
        boolean shulkers = Configs.Generic.COUNT_SHULKER_CONTENTS.getBooleanValue();
        int count = 0;

        for (int i = 0; i < inv.getContainerSize(); ++i)
        {
            ItemStack stack = inv.getItem(i);

            if (stack.isEmpty())
            {
                continue;
            }

            if (stack.getItem() == item)
            {
                count += stack.getCount();
            }
            else if (shulkers)
            {
                for (ItemStack inner : InventoryUtils.getStoredItems(stack))
                {
                    if (inner.getItem() == item)
                    {
                        count += inner.getCount();
                    }
                }
            }
        }

        return count;
    }

    private static String locateInInventory(Minecraft mc, Item item)
    {
        Inventory inv = mc.player.getInventory();

        for (int i = 0; i < 9; ++i)
        {
            if (inv.getItem(i).getItem() == item)
            {
                return GuiBase.TXT_GREEN + tr("slot_hotbar", i + 1) + GuiBase.TXT_RST;
            }
        }

        boolean inShulker = false;

        for (int i = 0; i < inv.getContainerSize(); ++i)
        {
            ItemStack stack = inv.getItem(i);

            if (stack.getItem() == item)
            {
                return GuiBase.TXT_YELLOW + tr("slot_inventory") + GuiBase.TXT_RST;
            }

            if (inShulker == false && stack.isEmpty() == false)
            {
                for (ItemStack inner : InventoryUtils.getStoredItems(stack))
                {
                    if (inner.getItem() == item)
                    {
                        inShulker = true;
                        break;
                    }
                }
            }
        }

        return inShulker ? GuiBase.TXT_GOLD + tr("slot_shulker") + GuiBase.TXT_RST
                         : GuiBase.TXT_RED + tr("slot_none") + GuiBase.TXT_RST;
    }

    @Nullable
    private static Item pickNextItem(Minecraft mc, BuildScanner scanner)
    {
        Item best = null;
        int bestCount = 0;
        Item bestAvailable = null;
        int bestAvailableCount = 0;

        for (Map.Entry<Item, Integer> e : scanner.getSuggestionMap().entrySet())
        {
            int c = e.getValue();

            if (c > bestCount)
            {
                best = e.getKey();
                bestCount = c;
            }

            if (c > bestAvailableCount && countInInventory(mc, e.getKey()) > 0)
            {
                bestAvailable = e.getKey();
                bestAvailableCount = c;
            }
        }

        return bestAvailable != null ? bestAvailable : best;
    }

    private static void fillRounded(GuiContext ctx, int x1, int y1, int x2, int y2, int color)
    {
        ctx.fill(x1 + 2, y1, x2 - 2, y1 + 1, color);
        ctx.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, color);
        ctx.fill(x1, y1 + 2, x2, y2 - 2, color);
        ctx.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, color);
        ctx.fill(x1 + 2, y2 - 1, x2 - 2, y2, color);
    }

    private static String percent(int part, int whole)
    {
        return String.format("%.1f", 100.0 * part / whole);
    }

    public static void render(GuiContext ctx)
    {
        Minecraft mc = ctx.mc();

        if (mc.player == null || SchematicWorldHandler.getSchematicWorld() == null)
        {
            return;
        }

        BuildScanner scanner = BuildScanner.getInstance();
        Map<Item, Integer> remainingMap = scanner.getRemainingMap();
        Map<Item, Integer> nearby = scanner.getNearbyMissing();
        boolean layerActive = scanner.isLayerActive();
        boolean layerValid = scanner.isLayerValid();
        String layer = scanner.getLayerLabel();

        ItemStack held = mc.player.getMainHandItem();
        Item heldItem = held.isEmpty() ? null : held.getItem();
        boolean heldUseful = heldItem != null && (nearby.containsKey(heldItem) || remainingMap.containsKey(heldItem));

        boolean progressEnabled = Configs.Generic.PROGRESS_BAR.getBooleanValue();
        boolean showLayer = progressEnabled && layerActive && layerValid && scanner.getLayerTotal() > 0;
        boolean showTotal = progressEnabled && scanner.hasFullResult() && scanner.getTotal() > 0;

        Item item = null;
        boolean suggestion = false;

        if (heldUseful)
        {
            item = heldItem;
        }
        else if (Configs.Generic.NEXT_BLOCK_SUGGESTION.getBooleanValue())
        {
            item = pickNextItem(mc, scanner);
            suggestion = item != null;
        }

        if (item == null && showLayer == false && showTotal == false)
        {
            return;
        }

        Font font = mc.font;
        List<String> lines = new ArrayList<>();
        String title = null;
        ItemStack icon = ItemStack.EMPTY;
        int width = MIN_WIDTH;

        if (item != null)
        {
            icon = new ItemStack(item);
            title = GuiBase.TXT_BOLD + icon.getHoverName().getString() + GuiBase.TXT_RST;
            int remainingScope = remainingMap.getOrDefault(item, 0);
            int remainingNear = nearby.getOrDefault(item, 0);
            int remaining = Math.max(remainingScope, remainingNear);

            if (suggestion)
            {
                lines.add(GuiBase.TXT_GRAY + tr("next_block") + GuiBase.TXT_RST);

                if (layerActive && layerValid)
                {
                    lines.add(tr("layer_missing", layer, remainingScope));
                }
                else
                {
                    lines.add(tr("nearby_missing", remainingNear));
                }

                lines.add(tr("location", locateInInventory(mc, item)));
            }
            else
            {
                int have = countInInventory(mc, item);
                lines.add(layerActive ? tr("remaining_layer", layer, remaining) : tr("remaining", remaining));
                lines.add(tr("in_inventory", have));
                lines.add(have >= remaining ? GuiBase.TXT_GREEN + tr("enough") + GuiBase.TXT_RST
                                            : GuiBase.TXT_RED + tr("lacking", remaining - have) + GuiBase.TXT_RST);
            }

            width = Math.max(width, 20 + font.width(title));
        }

        for (String s : lines)
        {
            width = Math.max(width, font.width(s));
        }

        String layerText = null;
        String totalText = null;

        if (showLayer)
        {
            layerText = tr("progress_layer", layer, percent(scanner.getLayerCorrect(), scanner.getLayerTotal()), scanner.getLayerCorrect(), scanner.getLayerTotal());
            width = Math.max(width, font.width(layerText));
        }

        if (showTotal)
        {
            totalText = tr(showLayer ? "progress_total" : "progress", percent(scanner.getCorrect(), scanner.getTotal()), scanner.getCorrect(), scanner.getTotal());
            width = Math.max(width, font.width(totalText));
        }

        int height = 0;

        if (item != null)
        {
            height += 18 + lines.size() * LINE_HEIGHT;
        }

        if (showLayer || showTotal)
        {
            height += item != null ? 4 : 0;

            if (showLayer)
            {
                height += LINE_HEIGHT + BAR_HEIGHT + 3;
            }

            if (showTotal)
            {
                height += LINE_HEIGHT + (showLayer ? THIN_BAR_HEIGHT : BAR_HEIGHT) + 1;
            }
        }

        double scale = Configs.Generic.HUD_SCALE.getDoubleValue();
        int boxW = width + PAD * 2;
        int boxH = height + PAD * 2;
        int screenW = (int) (GuiUtils.getScaledWindowWidth() / scale);
        int screenH = (int) (GuiUtils.getScaledWindowHeight() / scale);
        int offX = Configs.Generic.HUD_OFFSET_X.getIntegerValue();
        int offY = Configs.Generic.HUD_OFFSET_Y.getIntegerValue();
        HudAlignment alignment = (HudAlignment) Configs.Generic.HUD_ALIGNMENT.getOptionListValue();

        int x = switch (alignment)
        {
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - boxW - offX;
            case CENTER -> screenW / 2 - boxW / 2 + offX;
            default -> offX;
        };

        int y = switch (alignment)
        {
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - boxH - offY;
            case CENTER -> screenH / 2 - boxH / 2 + offY;
            default -> offY;
        };

        if (scale != 1.0)
        {
            ctx.pose().pushMatrix();
            ctx.pose().scale((float) scale, (float) scale);
        }

        fillRounded(ctx, x, y, x + boxW, y + boxH, Configs.Colors.HUD_BACKGROUND.getIntegerValue());

        int cx = x + PAD;
        int cy = y + PAD;

        if (item != null)
        {
            ctx.renderItem(icon, cx, cy);
            ctx.drawString(font, title, cx + 20, cy + 4, 0xFFFFFFFF, true);
            cy += 18;

            for (String s : lines)
            {
                ctx.drawString(font, s, cx, cy, 0xFFE0E0E0, false);
                cy += LINE_HEIGHT;
            }
        }

        if (showLayer || showTotal)
        {
            if (item != null)
            {
                ctx.fill(cx, cy + 1, cx + width, cy + 2, 0x40FFFFFF);
                cy += 4;
            }

            if (showLayer)
            {
                ctx.drawString(font, layerText, cx, cy, 0xFFE0E0E0, false);
                cy += LINE_HEIGHT;
                drawBar(ctx, cx, cy, width, BAR_HEIGHT, scanner.getLayerCorrect(), scanner.getLayerTotal(), Configs.Colors.PROGRESS_LAYER_COLOR.getIntegerValue());
                cy += BAR_HEIGHT + 3;
            }

            if (showTotal)
            {
                ctx.drawString(font, totalText, cx, cy, showLayer ? 0xFFA0A0A0 : 0xFFE0E0E0, false);
                cy += LINE_HEIGHT;
                int color = showLayer ? Configs.Colors.PROGRESS_TOTAL_COLOR.getIntegerValue() : Configs.Colors.PROGRESS_LAYER_COLOR.getIntegerValue();
                drawBar(ctx, cx, cy, width, showLayer ? THIN_BAR_HEIGHT : BAR_HEIGHT, scanner.getCorrect(), scanner.getTotal(), color);
            }
        }

        if (scale != 1.0)
        {
            ctx.pose().popMatrix();
        }
    }

    private static void drawBar(GuiContext ctx, int x, int y, int width, int height, int part, int whole, int color)
    {
        int filled = (int) Math.round((double) width * part / whole);
        ctx.fill(x, y, x + width, y + height, 0xFF303030);
        ctx.fill(x, y, x + filled, y + height, color);
    }
}
