package io.github.lwozr.buildhelper.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.lwozr.buildhelper.Reference;

public enum MaterialScope implements IConfigOptionListEntry
{
    LAYER("layer"),
    SCHEMATIC("schematic");

    private final String configString;

    MaterialScope(String configString)
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
        return StringUtils.translate(Reference.MOD_ID + ".label.material_scope." + this.configString);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        return values()[(this.ordinal() + (forward ? 1 : values().length - 1)) % values().length];
    }

    @Override
    public IConfigOptionListEntry fromString(String name)
    {
        for (MaterialScope value : values())
        {
            if (value.configString.equalsIgnoreCase(name))
            {
                return value;
            }
        }

        return LAYER;
    }
}
