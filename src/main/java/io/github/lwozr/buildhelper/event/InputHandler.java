package io.github.lwozr.buildhelper.event;

import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import io.github.lwozr.buildhelper.Reference;
import io.github.lwozr.buildhelper.config.Configs;

public class InputHandler implements IKeybindProvider
{
    private static final InputHandler INSTANCE = new InputHandler();

    private InputHandler()
    {
    }

    public static InputHandler getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager)
    {
        for (IHotkey hotkey : Configs.Hotkeys.HOTKEY_LIST)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }

        for (IHotkey hotkey : Configs.Generic.HOTKEY_LIST)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager)
    {
        manager.addHotkeysForCategory(Reference.MOD_NAME, Reference.MOD_ID + ".hotkeys.category.hotkeys", Configs.Hotkeys.HOTKEY_LIST);
        manager.addHotkeysForCategory(Reference.MOD_NAME, Reference.MOD_ID + ".hotkeys.category.toggles", Configs.Generic.HOTKEY_LIST);
    }
}
