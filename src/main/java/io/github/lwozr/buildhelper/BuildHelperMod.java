package io.github.lwozr.buildhelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.fabricmc.api.ModInitializer;
import fi.dy.masa.malilib.event.InitializationHandler;

public class BuildHelperMod implements ModInitializer
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public void onInitialize()
    {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
