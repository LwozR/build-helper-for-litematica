package io.github.lwozr.buildhelper.event;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import io.github.lwozr.buildhelper.config.Configs;
import io.github.lwozr.buildhelper.gui.GuiConfigs;

public class KeyCallbacks
{
    public static void init()
    {
        Configs.Hotkeys.OPEN_CONFIG_GUI.getKeybind().setCallback(new OpenConfigGui());
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
