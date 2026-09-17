package io.github.lwozr.buildhelper;

import net.fabricmc.loader.api.FabricLoader;

public class Reference
{
    public static final String MOD_ID = "buildhelper_litematica";
    public static final String MOD_NAME = "Build Helper for Litematica";
    public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
}
