package io.github.lwozr.buildhelper.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.lwozr.buildhelper.Reference;

public enum BlockOrderMode implements IConfigOptionListEntry
{
    AUTOMATIC("automatic"),
    MANUAL("manual");

    private final String configString;

    BlockOrderMode(String configString)
    {
        this.configString = configString;
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(Reference.MOD_ID + ".label.block_order_mode." + this.configString);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        return values()[(this.ordinal() + (forward ? 1 : values().length - 1)) % values().length];
    }

    @Override
    public IConfigOptionListEntry fromString(String name)
    {
        for (BlockOrderMode value : values())
        {
            if (value.configString.equalsIgnoreCase(name))
            {
                return value;
            }
        }

        return AUTOMATIC;
    }
}
