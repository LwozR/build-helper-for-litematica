package io.github.lwozr.buildhelper.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerMode;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import io.github.lwozr.buildhelper.Reference;
import io.github.lwozr.buildhelper.config.Configs;

public class BuildScanner
{
    private static final BuildScanner INSTANCE = new BuildScanner();
    private static final int MAX_TARGETS = 4096;
    private static final int FULL_SCAN_BUDGET_PER_TICK = 120_000;
    private static final int LAYER_SCAN_MAX_POSITIONS = 600_000;
    private static final int TYPE_SKIP = 0;
    private static final int TYPE_CORRECT = 1;
    private static final int TYPE_MISSING = 2;
    private static final int TYPE_WRONG = 3;
    private static final Set<String> NEIGHBOR_SHAPE_PROPERTIES = Set.of("north", "south", "east", "west", "up", "down", "shape", "waterlogged");

    private final Minecraft mc = Minecraft.getInstance();

    private final List<BlockPos> heldTargets = new ArrayList<>();
    private final Map<Item, Integer> nearbyMissing = new HashMap<>();
    private ItemStack lastHeld = ItemStack.EMPTY;
    private int nearbyTickCounter;
    private Set<Item> ignoredItems = Set.of();

    private final List<int[]> scanBoxes = new ArrayList<>();
    private int scanBoxIndex = -1;
    private int scanX, scanY, scanZ;
    private int passTotal, passCorrect;
    private final Map<Item, Integer> passMissing = new HashMap<>();
    private int total, correct;
    private final Map<Item, Integer> fullMissing = new HashMap<>();
    private boolean hasFullResult;

    private int layerTickCounter;
    @Nullable private String lastLayerKey;
    private int lastLayerRemaining = -1;
    private boolean layerActive;
    private boolean layerValid;
    private int layerTotal, layerCorrect;
    private String layerLabel = "";
    private final Map<Item, Integer> layerMissing = new HashMap<>();

    public static BuildScanner getInstance()
    {
        return INSTANCE;
    }

    public List<BlockPos> getHeldTargets()
    {
        return this.heldTargets;
    }

    public Map<Item, Integer> getNearbyMissing()
    {
        return this.nearbyMissing;
    }

    public boolean isLayerActive()
    {
        return this.layerActive;
    }

    public boolean isLayerValid()
    {
        return this.layerValid;
    }

    public String getLayerLabel()
    {
        return this.layerLabel;
    }

    public int getLayerTotal()
    {
        return this.layerTotal;
    }

    public int getLayerCorrect()
    {
        return this.layerCorrect;
    }

    public boolean hasFullResult()
    {
        return this.hasFullResult;
    }

    public int getTotal()
    {
        return this.total;
    }

    public int getCorrect()
    {
        return this.correct;
    }

    public Map<Item, Integer> getRemainingMap()
    {
        if (this.layerActive)
        {
            return this.layerValid ? this.layerMissing : this.nearbyMissing;
        }

        return this.fullMissing;
    }

    public Map<Item, Integer> getSuggestionMap()
    {
        return this.layerActive && this.layerValid ? this.layerMissing : this.nearbyMissing;
    }

    private static boolean anyEnabled()
    {
        return Configs.Generic.HIGHLIGHT_HELD_BLOCK.getBooleanValue() ||
               Configs.Generic.HUD_ENABLED.getBooleanValue() ||
               Configs.Generic.LAYER_DONE_MESSAGE.getBooleanValue() ||
               Configs.Generic.LAYER_DONE_SOUND.getBooleanValue();
    }

    public void onClientTick(Minecraft mc)
    {
        if (mc.level == null || mc.player == null || anyEnabled() == false)
        {
            return;
        }

        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld == null)
        {
            this.clearAll();
            return;
        }

        ItemStack held = mc.player.getMainHandItem();
        boolean heldChanged = ItemStack.isSameItem(held, this.lastHeld) == false;

        if (heldChanged || ++this.nearbyTickCounter >= 5)
        {
            this.nearbyTickCounter = 0;
            this.lastHeld = held.copy();
            Set<Item> ignored = IgnoredMaterials.collect();

            if (ignored.equals(this.ignoredItems) == false)
            {
                this.ignoredItems = ignored;
                this.scanBoxIndex = -1;
                this.layerTickCounter = 10;
                this.lastLayerRemaining = -1;
            }
            this.scanNearby(schematicWorld, held);
        }

        if (Configs.Generic.HUD_ENABLED.getBooleanValue() && Configs.Generic.PROGRESS_BAR.getBooleanValue())
        {
            this.stepFullScan(schematicWorld);
        }

        this.checkLayer(schematicWorld);
    }

    private void clearAll()
    {
        this.heldTargets.clear();
        this.nearbyMissing.clear();
        this.fullMissing.clear();
        this.scanBoxIndex = -1;
        this.hasFullResult = false;
        this.total = 0;
        this.correct = 0;
        this.lastLayerKey = null;
        this.lastLayerRemaining = -1;
        this.resetLayerStats();
    }

    private void resetLayerStats()
    {
        this.layerActive = false;
        this.layerValid = false;
        this.layerTotal = 0;
        this.layerCorrect = 0;
        this.layerMissing.clear();
    }

    private static List<int[]> collectBoxes()
    {
        List<int[]> list = new ArrayList<>();

        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
        {
            if (placement.isEnabled() == false || placement.isRenderingEnabled() == false)
            {
                continue;
            }

            for (Box box : placement.getSubRegionBoxes(RequiredEnabled.RENDERING_ENABLED).values())
            {
                BlockPos p1 = box.getPos1();
                BlockPos p2 = box.getPos2();

                if (p1 != null && p2 != null)
                {
                    list.add(new int[] {
                            Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()),
                            Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()) });
                }
            }
        }

        return list;
    }

    private static void addCount(Map<Item, Integer> map, Item item)
    {
        map.merge(item, 1, Integer::sum);
    }

    private int classify(WorldSchematic schematicWorld, BlockPos.MutableBlockPos pos)
    {
        BlockState stateSchematic = schematicWorld.getBlockState(pos);

        if (stateSchematic.isAir())
        {
            return TYPE_SKIP;
        }

        Item item = requiredItem(schematicWorld, pos);

        if (item == null)
        {
            return TYPE_SKIP;
        }

        if (this.ignoredItems.contains(item))
        {
            return TYPE_CORRECT;
        }

        BlockState stateClient = this.mc.level.getBlockState(pos);

        if (stateClient == stateSchematic || sameIgnoringNeighborShape(stateClient, stateSchematic))
        {
            return TYPE_CORRECT;
        }

        return (stateClient.isAir() || stateClient.canBeReplaced()) ? TYPE_MISSING : TYPE_WRONG;
    }

    private static boolean sameIgnoringNeighborShape(BlockState stateClient, BlockState stateSchematic)
    {
        if (stateClient.getBlock() != stateSchematic.getBlock())
        {
            return false;
        }

        for (Property<?> property : stateSchematic.getProperties())
        {
            if (NEIGHBOR_SHAPE_PROPERTIES.contains(property.getName()) == false &&
                stateClient.getValue(property).equals(stateSchematic.getValue(property)) == false)
            {
                return false;
            }
        }

        return true;
    }

    @Nullable
    private static Item requiredItem(WorldSchematic schematicWorld, BlockPos pos)
    {
        ItemStack required = MaterialCache.getInstance().getRequiredBuildItemForState(schematicWorld.getBlockState(pos));
        return required.isEmpty() ? null : required.getItem();
    }

    private void scanNearby(WorldSchematic schematicWorld, ItemStack held)
    {
        this.heldTargets.clear();
        this.nearbyMissing.clear();

        int r = Configs.Generic.HIGHLIGHT_RANGE.getIntegerValue();
        BlockPos center = this.mc.player.blockPosition();
        LayerRange range = DataManager.getRenderLayerRange();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Item heldItem = held.isEmpty() ? null : held.getItem();

        for (int[] b : collectBoxes())
        {
            int minX = Math.max(b[0], center.getX() - r), maxX = Math.min(b[3], center.getX() + r);
            int minY = Math.max(b[1], center.getY() - r), maxY = Math.min(b[4], center.getY() + r);
            int minZ = Math.max(b[2], center.getZ() - r), maxZ = Math.min(b[5], center.getZ() + r);

            for (int y = minY; y <= maxY; ++y)
            {
                for (int z = minZ; z <= maxZ; ++z)
                {
                    for (int x = minX; x <= maxX; ++x)
                    {
                        if (range.isPositionWithinRange(x, y, z) == false)
                        {
                            continue;
                        }

                        mutable.set(x, y, z);

                        if (this.classify(schematicWorld, mutable) != TYPE_MISSING)
                        {
                            continue;
                        }

                        Item item = requiredItem(schematicWorld, mutable);

                        if (item == null)
                        {
                            continue;
                        }

                        addCount(this.nearbyMissing, item);

                        if (item == heldItem && this.heldTargets.size() < MAX_TARGETS)
                        {
                            this.heldTargets.add(mutable.immutable());
                        }
                    }
                }
            }
        }
    }

    private void stepFullScan(WorldSchematic schematicWorld)
    {
        if (this.scanBoxIndex < 0)
        {
            this.scanBoxes.clear();
            this.scanBoxes.addAll(collectBoxes());

            if (this.scanBoxes.isEmpty())
            {
                this.hasFullResult = false;
                this.total = 0;
                this.correct = 0;
                this.fullMissing.clear();
                return;
            }

            this.scanBoxIndex = 0;
            int[] b = this.scanBoxes.get(0);
            this.scanX = b[0];
            this.scanY = b[1];
            this.scanZ = b[2];
            this.passTotal = 0;
            this.passCorrect = 0;
            this.passMissing.clear();
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int budget = FULL_SCAN_BUDGET_PER_TICK;

        while (budget-- > 0)
        {
            int[] b = this.scanBoxes.get(this.scanBoxIndex);

            if (this.mc.level.hasChunk(this.scanX >> 4, this.scanZ >> 4))
            {
                mutable.set(this.scanX, this.scanY, this.scanZ);
                int type = this.classify(schematicWorld, mutable);

                if (type != TYPE_SKIP)
                {
                    this.passTotal++;

                    if (type == TYPE_CORRECT)
                    {
                        this.passCorrect++;
                    }
                    else if (type == TYPE_MISSING)
                    {
                        Item item = requiredItem(schematicWorld, mutable);

                        if (item != null)
                        {
                            addCount(this.passMissing, item);
                        }
                    }
                }
            }
            else
            {
                int nextChunkX = ((this.scanX >> 4) + 1) << 4;
                this.scanX = Math.min(nextChunkX, b[3] + 1) - 1;
            }

            if (++this.scanX > b[3])
            {
                this.scanX = b[0];

                if (++this.scanZ > b[5])
                {
                    this.scanZ = b[2];

                    if (++this.scanY > b[4])
                    {
                        if (++this.scanBoxIndex >= this.scanBoxes.size())
                        {
                            this.total = this.passTotal;
                            this.correct = this.passCorrect;
                            this.fullMissing.clear();
                            this.fullMissing.putAll(this.passMissing);
                            this.hasFullResult = true;
                            this.scanBoxIndex = -1;
                            return;
                        }

                        int[] nb = this.scanBoxes.get(this.scanBoxIndex);
                        this.scanX = nb[0];
                        this.scanY = nb[1];
                        this.scanZ = nb[2];
                    }
                }
            }
        }
    }

    private void checkLayer(WorldSchematic schematicWorld)
    {
        LayerRange range = DataManager.getRenderLayerRange();
        LayerMode mode = range.getLayerMode();

        if (mode == LayerMode.ALL)
        {
            this.lastLayerKey = null;
            this.lastLayerRemaining = -1;
            this.resetLayerStats();
            return;
        }

        Direction.Axis axis = range.getAxis();
        int lmin;
        int lmax;

        switch (mode)
        {
            case SINGLE_LAYER -> { lmin = range.getLayerSingle(); lmax = lmin; }
            case LAYER_RANGE -> { lmin = Math.min(range.getLayerRangeMin(), range.getLayerRangeMax()); lmax = Math.max(range.getLayerRangeMin(), range.getLayerRangeMax()); }
            case ALL_ABOVE -> { lmin = range.getLayerAbove(); lmax = Integer.MAX_VALUE; }
            case ALL_BELOW -> { lmin = Integer.MIN_VALUE; lmax = range.getLayerBelow(); }
            default -> { return; }
        }

        String key = mode.name() + ":" + axis.getName() + ":" + lmin + ":" + lmax;
        boolean keyChanged = key.equals(this.lastLayerKey) == false;

        if (keyChanged == false && ++this.layerTickCounter < 10)
        {
            return;
        }

        this.layerTickCounter = 0;
        this.layerActive = true;
        this.layerLabel = lmin == lmax ? String.valueOf(lmin) :
                          (lmin == Integer.MIN_VALUE ? "≤" + lmax : (lmax == Integer.MAX_VALUE ? "≥" + lmin : lmin + "-" + lmax));

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Map<Item, Integer> missing = new HashMap<>();
        int layerTotal = 0;
        int layerCorrect = 0;
        int remaining = 0;
        boolean incomplete = false;
        long volume = 0;
        int ai = axis == Direction.Axis.X ? 0 : (axis == Direction.Axis.Y ? 1 : 2);

        for (int[] b : collectBoxes())
        {
            int[] c = b.clone();
            c[ai] = Math.max(c[ai], lmin);
            c[ai + 3] = Math.min(c[ai + 3], lmax);

            if (c[0] > c[3] || c[1] > c[4] || c[2] > c[5])
            {
                continue;
            }

            volume += (long) (c[3] - c[0] + 1) * (c[4] - c[1] + 1) * (c[5] - c[2] + 1);

            if (volume > LAYER_SCAN_MAX_POSITIONS)
            {
                this.layerValid = false;
                this.layerMissing.clear();
                this.lastLayerKey = key;
                this.lastLayerRemaining = -1;
                return;
            }

            for (int z = c[2]; z <= c[5]; ++z)
            {
                for (int x = c[0]; x <= c[3]; ++x)
                {
                    if (this.mc.level.hasChunk(x >> 4, z >> 4) == false)
                    {
                        incomplete = true;
                        continue;
                    }

                    for (int y = c[1]; y <= c[4]; ++y)
                    {
                        mutable.set(x, y, z);
                        int type = this.classify(schematicWorld, mutable);

                        if (type == TYPE_SKIP)
                        {
                            continue;
                        }

                        layerTotal++;

                        if (type == TYPE_CORRECT)
                        {
                            layerCorrect++;
                            continue;
                        }

                        remaining++;

                        if (type == TYPE_MISSING)
                        {
                            Item item = requiredItem(schematicWorld, mutable);

                            if (item != null)
                            {
                                addCount(missing, item);
                            }
                        }
                    }
                }
            }
        }

        this.layerValid = true;
        this.layerTotal = layerTotal;
        this.layerCorrect = layerCorrect;
        this.layerMissing.clear();
        this.layerMissing.putAll(missing);

        if (keyChanged == false && incomplete == false && this.lastLayerRemaining > 0 && remaining == 0 && layerTotal > 0)
        {
            if (Configs.Generic.LAYER_DONE_MESSAGE.getBooleanValue())
            {
                InfoUtils.showInGameMessage(MessageType.SUCCESS, 3000, Reference.MOD_ID + ".message.layer_done", this.layerLabel);
            }

            if (Configs.Generic.LAYER_DONE_SOUND.getBooleanValue())
            {
                this.mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2f));
            }
        }

        this.lastLayerKey = key;
        this.lastLayerRemaining = incomplete ? -1 : remaining;
    }
}
