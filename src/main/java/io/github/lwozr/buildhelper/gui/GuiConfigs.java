package io.github.lwozr.buildhelper.gui;

import java.util.List;
import java.util.Objects;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.lwozr.buildhelper.Reference;
import io.github.lwozr.buildhelper.config.Configs;

public class GuiConfigs extends GuiConfigsBase
{
    private static ConfigGuiTab tab = ConfigGuiTab.GENERIC;

    public GuiConfigs()
    {
        super(10, 50, Reference.MOD_ID, null, Reference.MOD_ID + ".gui.title.configs", Reference.MOD_VERSION);
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;

        for (ConfigGuiTab t : ConfigGuiTab.values())
        {
            ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, t.getDisplayName());
            button.setEnabled(tab != t);
            this.addButton(button, new ButtonListener(t, this));
            x += button.getWidth() + 2;
        }

        String orderLabel = StringUtils.translate(Reference.MOD_ID + ".gui.button.block_order");
        ButtonGeneric orderButton = new ButtonGeneric(x + 8, y, -1, 20, orderLabel);
        this.addButton(orderButton, (button, mouseButton) -> GuiBase.openGui(new GuiBlockOrder().setParent(this)));
    }

    @Override
    protected int getConfigWidth()
    {
        return switch (tab)
        {
            case GENERIC -> 160;
            case COLORS -> 100;
            case HOTKEYS -> super.getConfigWidth();
        };
    }

    @Override
    protected boolean useKeybindSearch()
    {
        return tab == ConfigGuiTab.HOTKEYS;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        List<? extends IConfigBase> configs = switch (tab)
        {
            case GENERIC -> Configs.Generic.OPTIONS;
            case COLORS -> Configs.Colors.OPTIONS;
            case HOTKEYS -> Configs.Hotkeys.HOTKEY_LIST;
        };

        return ConfigOptionWrapper.createFor(configs);
    }

    private record ButtonListener(ConfigGuiTab tab, GuiConfigs parent) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            GuiConfigs.tab = this.tab;
            this.parent.reCreateListWidget();
            Objects.requireNonNull(this.parent.getListWidget()).resetScrollbarPosition();
            this.parent.initGui();
        }
    }

    public enum ConfigGuiTab
    {
        GENERIC("generic"),
        COLORS("colors"),
        HOTKEYS("hotkeys");

        private final String translationKey;

        ConfigGuiTab(String name)
        {
            this.translationKey = Reference.MOD_ID + ".gui.button.config_gui." + name;
        }

        public String getDisplayName()
        {
            return StringUtils.translate(this.translationKey);
        }
    }
}
