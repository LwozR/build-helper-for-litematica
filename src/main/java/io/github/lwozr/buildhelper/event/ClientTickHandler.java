package io.github.lwozr.buildhelper.event;

import net.minecraft.client.Minecraft;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import io.github.lwozr.buildhelper.printer.Printer;
import io.github.lwozr.buildhelper.render.BuildScanner;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(Minecraft mc)
    {
        BuildScanner.getInstance().onClientTick(mc);
        Printer.onClientTick(mc);
    }
}
