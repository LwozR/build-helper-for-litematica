package io.github.lwozr.buildhelper.gui;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.lwozr.buildhelper.Reference;
import io.github.lwozr.buildhelper.config.BlockOrderMode;
import io.github.lwozr.buildhelper.config.Configs;
import io.github.lwozr.buildhelper.data.BlockOrder;
import io.github.lwozr.buildhelper.render.BuildScanner;

public class GuiBlockOrder extends GuiBase
{
    private static final String KEY = Reference.MOD_ID + ".gui.block_order.";
    private static final int LIST_TOP = 54;
    private static final int ROW_HEIGHT = 22;
    private static int page;

    private List<Item> order = List.of();
    private int rowsPerPage = 1;

    private static String tr(String key, Object... args)
    {
        return StringUtils.translate(KEY + key, args);
    }

    @Override
    public void initGui()
    {
        super.initGui();

        BuildScanner scanner = BuildScanner.getInstance();
        String layer = scanner.isLayerActive() ? scanner.getLayerLabel() : tr("all_layers");
        this.setTitle(tr("title", layer));

        this.order = BlockOrder.effectiveOrder();
        this.rowsPerPage = Math.max(1, (this.height - LIST_TOP - 10) / ROW_HEIGHT);
        int pages = Math.max(1, (this.order.size() + this.rowsPerPage - 1) / this.rowsPerPage);
        page = Math.max(0, Math.min(page, pages - 1));

        int x = 10;
        int y = 26;

        BlockOrderMode mode = (BlockOrderMode) Configs.Generic.BLOCK_ORDER_MODE.getOptionListValue();
        String modeLabel = tr("mode", mode.getDisplayName());
        ButtonGeneric modeButton = new ButtonGeneric(x, y, this.getStringWidth(modeLabel) + 10, 20, modeLabel);
        this.addButton(modeButton, (button, mouseButton) -> {
            IConfigOptionListEntry next = Configs.Generic.BLOCK_ORDER_MODE.getOptionListValue().cycle(mouseButton == 0);
            Configs.Generic.BLOCK_ORDER_MODE.setOptionListValue(next);
            Configs.saveToFile();
            this.initGui();
        });
        x += modeButton.getWidth() + 4;

        String resetLabel = tr("reset");
        ButtonGeneric resetButton = new ButtonGeneric(x, y, this.getStringWidth(resetLabel) + 10, 20, resetLabel, tr("reset_hover"));
        resetButton.setEnabled(BlockOrder.hasManualOrder());
        this.addButton(resetButton, (button, mouseButton) -> {
            BlockOrder.reset();
            this.initGui();
        });
        x += resetButton.getWidth() + 4;

        ButtonGeneric prev = new ButtonGeneric(x, y, 20, 20, "<");
        prev.setEnabled(page > 0);
        this.addButton(prev, (button, mouseButton) -> {
            page--;
            this.initGui();
        });
        x += 22;

        ButtonGeneric next = new ButtonGeneric(x, y, 20, 20, ">");
        next.setEnabled(page < pages - 1);
        this.addButton(next, (button, mouseButton) -> {
            page++;
            this.initGui();
        });

        int right = this.width - 10;
        int start = page * this.rowsPerPage;
        int end = Math.min(this.order.size(), start + this.rowsPerPage);

        for (int i = start; i < end; i++)
        {
            Item item = this.order.get(i);
            int rowY = LIST_TOP + (i - start) * ROW_HEIGHT;

            ButtonGeneric down = new ButtonGeneric(right - 20, rowY, 20, 20, "▼");
            down.setEnabled(i < this.order.size() - 1);
            this.addButton(down, (button, mouseButton) -> this.moveItem(item, 1));

            ButtonGeneric up = new ButtonGeneric(right - 42, rowY, 20, 20, "▲");
            up.setEnabled(i > 0);
            this.addButton(up, (button, mouseButton) -> this.moveItem(item, -1));
        }
    }

    private void moveItem(Item item, int offset)
    {
        BlockOrder.move(item, offset);

        if (Configs.Generic.BLOCK_ORDER_MODE.getOptionListValue() != BlockOrderMode.MANUAL)
        {
            Configs.Generic.BLOCK_ORDER_MODE.setOptionListValue(BlockOrderMode.MANUAL);
            Configs.saveToFile();
        }

        this.initGui();
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        BuildScanner scanner = BuildScanner.getInstance();

        if (this.order.isEmpty())
        {
            this.drawString(ctx, tr("empty"), 10, LIST_TOP + 6, 0xFFA0A0A0);
            return;
        }

        Map<Item, Integer> items = scanner.getOrderItems();
        Map<Item, Integer> missing = scanner.getOrderMissing();
        int pages = Math.max(1, (this.order.size() + this.rowsPerPage - 1) / this.rowsPerPage);
        String pageText = tr("page", page + 1, pages);
        this.drawString(ctx, pageText, this.width - 10 - this.getStringWidth(pageText), 32, 0xFFC0C0C0);

        int start = page * this.rowsPerPage;
        int end = Math.min(this.order.size(), start + this.rowsPerPage);

        for (int i = start; i < end; i++)
        {
            Item item = this.order.get(i);
            int rowY = LIST_TOP + (i - start) * ROW_HEIGHT;
            int left = missing.getOrDefault(item, 0);
            int all = items.getOrDefault(item, 0);
            ItemStack stack = new ItemStack(item);

            ctx.fill(8, rowY - 1, this.width - 54, rowY + 21, (i % 2 == 0) ? 0x30FFFFFF : 0x18FFFFFF);
            this.drawString(ctx, (i + 1) + ".", 12, rowY + 6, 0xFFA0A0A0);
            ctx.renderItem(stack, 34, rowY + 2);
            this.drawString(ctx, stack.getHoverName().getString(), 56, rowY + 6, left > 0 ? 0xFFFFFFFF : 0xFF808080);

            String count = left > 0 ? tr("missing", left, all) : tr("done", all);
            this.drawString(ctx, count, this.width - 60 - this.getStringWidth(count), rowY + 6, left > 0 ? 0xFFFFD060 : 0xFF60D060);
        }
    }
}
