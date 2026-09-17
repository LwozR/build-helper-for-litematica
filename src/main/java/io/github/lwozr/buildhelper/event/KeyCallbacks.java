package io.github.lwozr.buildhelper.event;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import io.github.lwozr.buildhelper.config.Configs;
import io.github.lwozr.buildhelper.gui.GuiBlockOrder;
import io.github.lwozr.buildhelper.gui.GuiConfigs;

public class KeyCallbacks
{
    public static void init()
    {
        Configs.Hotkeys.OPEN_CONFIG_GUI.getKeybind().setCallback(new OpenConfigGui());
        Configs.Hotkeys.OPEN_BLOCK_ORDER_GUI.getKeybind().setCallback((action, key) -> {
            GuiBase.openGui(new GuiBlockOrder());
            return true;
        });
    }

    private static class OpenConfigGui implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            GuiBase.openGui(new GuiConfigs());
            return true;
        }
    }
}
