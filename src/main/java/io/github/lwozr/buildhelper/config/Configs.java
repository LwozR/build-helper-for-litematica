package io.github.lwozr.buildhelper.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import io.github.lwozr.buildhelper.BuildHelperMod;
import io.github.lwozr.buildhelper.Reference;

public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

    public static class Generic
    {
        private static final String KEY = Reference.MOD_ID + ".config.generic";

        public static final ConfigBooleanHotkeyed HIGHLIGHT_HELD_BLOCK = new ConfigBooleanHotkeyed("highlightHeldBlock", true, "").apply(KEY);
        public static final ConfigInteger HIGHLIGHT_RANGE = new ConfigInteger("highlightRange", 32, 4, 64).apply(KEY);
        public static final ConfigBoolean HIGHLIGHT_THROUGH_BLOCKS = new ConfigBoolean("highlightThroughBlocks", true).apply(KEY);
        public static final ConfigBooleanHotkeyed HUD_ENABLED = new ConfigBooleanHotkeyed("hudEnabled", true, "").apply(KEY);
        public static final ConfigOptionList HUD_ALIGNMENT = new ConfigOptionList("hudAlignment", HudAlignment.BOTTOM_LEFT).apply(KEY);
        public static final ConfigInteger HUD_OFFSET_X = new ConfigInteger("hudOffsetX", 4, 0, 4000).apply(KEY);
        public static final ConfigInteger HUD_OFFSET_Y = new ConfigInteger("hudOffsetY", 4, 0, 4000).apply(KEY);
        public static final ConfigDouble HUD_SCALE = new ConfigDouble("hudScale", 1.0, 0.25, 3.0).apply(KEY);
        public static final ConfigBoolean PROGRESS_BAR = new ConfigBoolean("progressBar", true).apply(KEY);
        public static final ConfigBoolean NEXT_BLOCK_SUGGESTION = new ConfigBoolean("nextBlockSuggestion", true).apply(KEY);
        public static final ConfigBoolean COUNT_SHULKER_CONTENTS = new ConfigBoolean("countShulkerContents", true).apply(KEY);
        public static final ConfigBoolean LAYER_DONE_MESSAGE = new ConfigBoolean("layerDoneMessage", true).apply(KEY);
        public static final ConfigBoolean LAYER_DONE_SOUND = new ConfigBoolean("layerDoneSound", true).apply(KEY);
        public static final ConfigBooleanHotkeyed AUTO_NEXT_LAYER = new ConfigBooleanHotkeyed("autoNextLayer", true, "").apply(KEY);
        public static final ConfigOptionList AUTO_NEXT_LAYER_DIRECTION = new ConfigOptionList("autoNextLayerDirection", LayerDirection.UP).apply(KEY);
        public static final ConfigOptionList BLOCK_ORDER_MODE = new ConfigOptionList("blockOrderMode", BlockOrderMode.AUTOMATIC).apply(KEY);
        public static final ConfigBooleanHotkeyed CONTAINER_HIGHLIGHT = new ConfigBooleanHotkeyed("containerHighlight", true, "").apply(KEY);
        public static final ConfigBoolean CONTAINER_INGREDIENTS = new ConfigBoolean("containerIngredients", true).apply(KEY);
        public static final ConfigBoolean CONTAINER_TOOLTIP = new ConfigBoolean("containerTooltip", true).apply(KEY);
        public static final ConfigOptionList CONTAINER_SCOPE = new ConfigOptionList("containerScope", MaterialScope.LAYER).apply(KEY);
        public static final ConfigBooleanHotkeyed PRINTER_ENABLED = new ConfigBooleanHotkeyed("printerEnabled", false, "").apply(KEY);
        public static final ConfigInteger PRINTER_SPEED = new ConfigInteger("printerSpeed", 20, 1, 200).apply(KEY);
        public static final ConfigBoolean PRINTER_SNEAK_ONLY = new ConfigBoolean("printerSneakOnly", false).apply(KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                HIGHLIGHT_HELD_BLOCK,
                HIGHLIGHT_RANGE,
                HIGHLIGHT_THROUGH_BLOCKS,
                HUD_ENABLED,
                HUD_ALIGNMENT,
                HUD_OFFSET_X,
                HUD_OFFSET_Y,
                HUD_SCALE,
                PROGRESS_BAR,
                NEXT_BLOCK_SUGGESTION,
                COUNT_SHULKER_CONTENTS,
                LAYER_DONE_MESSAGE,
                LAYER_DONE_SOUND,
                AUTO_NEXT_LAYER,
                AUTO_NEXT_LAYER_DIRECTION,
                BLOCK_ORDER_MODE,
                CONTAINER_HIGHLIGHT,
                CONTAINER_INGREDIENTS,
                CONTAINER_TOOLTIP,
                CONTAINER_SCOPE,
                PRINTER_ENABLED,
                PRINTER_SPEED,
                PRINTER_SNEAK_ONLY
        );

        public static final List<IHotkey> HOTKEY_LIST = ImmutableList.of(
                HIGHLIGHT_HELD_BLOCK,
                HUD_ENABLED,
                AUTO_NEXT_LAYER,
                CONTAINER_HIGHLIGHT,
                PRINTER_ENABLED
        );
    }

    public static class Colors
    {
        private static final String KEY = Reference.MOD_ID + ".config.colors";

        public static final ConfigColor HIGHLIGHT_COLOR = new ConfigColor("highlightColor", "#5030FF30").apply(KEY);
        public static final ConfigColor HUD_BACKGROUND = new ConfigColor("hudBackground", "#B0101010").apply(KEY);
        public static final ConfigColor PROGRESS_LAYER_COLOR = new ConfigColor("progressLayerColor", "#FF3FCF4A").apply(KEY);
        public static final ConfigColor PROGRESS_TOTAL_COLOR = new ConfigColor("progressTotalColor", "#FF2A8FD0").apply(KEY);
        public static final ConfigColor CONTAINER_DIRECT_COLOR = new ConfigColor("containerDirectColor", "#FF3FCF4A").apply(KEY);
        public static final ConfigColor CONTAINER_INGREDIENT_COLOR = new ConfigColor("containerIngredientColor", "#FFE8C33A").apply(KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                HIGHLIGHT_COLOR,
                HUD_BACKGROUND,
                PROGRESS_LAYER_COLOR,
                PROGRESS_TOTAL_COLOR,
                CONTAINER_DIRECT_COLOR,
                CONTAINER_INGREDIENT_COLOR
        );
    }

    public static class Hotkeys
    {
        private static final String KEY = Reference.MOD_ID + ".config.hotkeys";

        public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey("openConfigGui", "M,B").apply(KEY);
        public static final ConfigHotkey OPEN_BLOCK_ORDER_GUI = new ConfigHotkey("openBlockOrderGui", "M,O").apply(KEY);

        public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
                OPEN_CONFIG_GUI,
                OPEN_BLOCK_ORDER_GUI
        );
    }

    public static void loadFromFile()
    {
        Path configFile = FileUtils.getConfigDirectory().resolve(CONFIG_FILE_NAME);

        if (Files.exists(configFile) && Files.isReadable(configFile))
        {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();

                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "Colors", Colors.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            }
            else
            {
                BuildHelperMod.LOGGER.error("Failed to load config file '{}'", configFile.toAbsolutePath());
            }
        }
    }

    public static void saveToFile()
    {
        Path dir = FileUtils.getConfigDirectory();

        if (Files.exists(dir) == false)
        {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        if (Files.isDirectory(dir))
        {
            JsonObject root = new JsonObject();

            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Colors", Colors.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);

            JsonUtils.writeJsonToFile(root, dir.resolve(CONFIG_FILE_NAME));
        }
    }

    @Override
    public void load()
    {
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }
}
