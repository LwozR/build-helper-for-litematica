package io.github.lwozr.buildhelper;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import io.github.lwozr.buildhelper.config.Configs;
import io.github.lwozr.buildhelper.event.ClientTickHandler;
import io.github.lwozr.buildhelper.event.InputHandler;
import io.github.lwozr.buildhelper.event.KeyCallbacks;
import io.github.lwozr.buildhelper.event.RenderHandler;
import io.github.lwozr.buildhelper.gui.GuiConfigs;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, GuiConfigs::new));

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());

        RenderHandler renderer = new RenderHandler();
        RenderEventHandler.getInstance().registerInGameGuiRenderer(renderer);
        RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);
        RenderEventHandler.getInstance().registerTooltipLastRenderer(renderer);

        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());

        KeyCallbacks.init();
    }
}
