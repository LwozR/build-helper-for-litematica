package io.github.lwozr.buildhelper.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.lwozr.buildhelper.Reference;

public enum LayerDirection implements IConfigOptionListEntry
{
    UP("up", 1),
    DOWN("down", -1);

    private final String configString;
    private final int step;

    LayerDirection(String configString, int step)
    {
        this.configString = configString;
        this.step = step;
    }

    public int getStep()
    {
        return this.step;
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(Reference.MOD_ID + ".label.layer_direction." + this.configString);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        return values()[(this.ordinal() + (forward ? 1 : values().length - 1)) % values().length];
    }

    @Override
    public IConfigOptionListEntry fromString(String name)
    {
        for (LayerDirection value : values())
        {
            if (value.configString.equalsIgnoreCase(name))
            {
                return value;
            }
        }

        return UP;
    }
}
