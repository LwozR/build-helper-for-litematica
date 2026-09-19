package io.github.lwozr.buildhelper.printer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import io.github.lwozr.buildhelper.Reference;
import io.github.lwozr.buildhelper.config.Configs;
import io.github.lwozr.buildhelper.render.BuildScanner;

public class Printer
{
    private static final float[] YAWS = {0f, 90f, 180f, -90f};
    private static final float[] PITCHES = {0f, 75f, -75f};
    private static final double[] SIDE_HIT_HEIGHTS = {0.5, 0.25, 0.75};
    private static final double MAX_BUDGET = 16.0;
    private static final Set<String> IGNORED_PROPERTIES = Set.of("north", "south", "east", "west", "up", "down", "shape", "waterlogged", "open", "powered");

    private static double budget;
    private static boolean lastEnabled;

    private record Placement(BlockHitResult hit, float yaw, float pitch)
    {
    }

    public static boolean isEnabled()
    {
        return Configs.Generic.PRINTER_ENABLED.getBooleanValue();
    }

    public static void onClientTick(Minecraft mc)
    {
        boolean enabled = isEnabled();

        if (enabled && lastEnabled == false)
        {
            InfoUtils.showInGameMessage(MessageType.WARNING, 6000, Reference.MOD_ID + ".message.printer_warning");
        }

        lastEnabled = enabled;
        LocalPlayer player = mc.player;

        if (enabled == false || player == null || mc.level == null || mc.gameMode == null || fi.dy.masa.malilib.util.GuiUtils.getCurrentScreen() != null)
        {
            budget = 0;
            return;
        }

        boolean sneakOnly = Configs.Generic.PRINTER_SNEAK_ONLY.getBooleanValue();
        Vec3 motion = player.getDeltaMovement();

        if ((sneakOnly && player.isShiftKeyDown() == false) || player.onGround() == false ||
            (sneakOnly == false && motion.x * motion.x + motion.z * motion.z > 0.0025))
        {
            budget = 0;
            return;
        }

        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        ItemStack held = player.getMainHandItem();

        if (schematicWorld == null || held.getItem() instanceof BlockItem == false)
        {
            budget = 0;
            return;
        }

        BuildScanner.getInstance().requestNearbyScan();
        budget = Math.min(MAX_BUDGET, budget + Configs.Generic.PRINTER_SPEED.getIntegerValue() / 20.0);

        if (budget < 1.0)
        {
            return;
        }

        BlockItem blockItem = (BlockItem) held.getItem();
        Vec3 eye = player.getEyePosition();
        double reach = player.blockInteractionRange();
        List<BlockPos> placed = new ArrayList<>();
        List<BlockPos> targets = new ArrayList<>(BuildScanner.getInstance().getHeldTargets());
        targets.sort(Comparator.comparingDouble(p -> Vec3.atCenterOf(p).distanceToSqr(eye)));

        for (BlockPos pos : targets)
        {
            if (placed.contains(pos))
            {
                continue;
            }

            if (budget < 1.0 || player.getMainHandItem().isEmpty())
            {
                break;
            }

            if (Vec3.atCenterOf(pos).distanceTo(eye) > reach + 1.0)
            {
                break;
            }

            BlockState current = mc.level.getBlockState(pos);
            BlockState wanted = schematicWorld.getBlockState(pos);

            if ((current.isAir() == false && current.canBeReplaced() == false) || wanted.getBlock() != blockItem.getBlock())
            {
                continue;
            }

            Placement placement = findPlacement(mc.level, player, held, blockItem.getBlock(), pos, wanted, eye, reach);

            if (placement != null)
            {
                place(mc, player, placement);

                if (needsOpening(wanted) && mc.level.getBlockState(pos).getBlock() == wanted.getBlock())
                {
                    open(mc, player, pos);
                }

                placed.add(pos);
                budget -= 1.0;
            }
        }
    }

    private static boolean isInteractive(Block block)
    {
        return block instanceof BaseEntityBlock || block instanceof DoorBlock || block instanceof TrapDoorBlock ||
               block instanceof FenceGateBlock || block instanceof ButtonBlock || block instanceof LeverBlock ||
               block instanceof BedBlock || block instanceof NoteBlock || block instanceof RepeaterBlock ||
               block instanceof ComparatorBlock || block instanceof CakeBlock || block instanceof CraftingTableBlock;
    }

    private static Placement findPlacement(Level level, LocalPlayer player, ItemStack held, Block block, BlockPos pos, BlockState wanted, Vec3 eye, double reach)
    {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        List<float[]> rotations = new ArrayList<>();
        rotations.add(new float[] {yaw, pitch});

        for (float p : PITCHES)
        {
            for (float y : YAWS)
            {
                rotations.add(new float[] {y, p});
            }
        }

        try
        {
            for (Direction direction : Direction.values())
            {
                BlockPos neighbor = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighbor);

                if (neighborState.isAir() || neighborState.canBeReplaced() ||
                    (player.isShiftKeyDown() == false && isInteractive(neighborState.getBlock())))
                {
                    continue;
                }

                Direction face = direction.getOpposite();
                double[] heights = face.getAxis() == Direction.Axis.Y ? new double[] {0.5} : SIDE_HIT_HEIGHTS;

                for (double height : heights)
                {
                    Vec3 hitVec = hitVector(neighbor, face, height);

                    if (hitVec.distanceTo(eye) > reach || isVisible(level, player, eye, hitVec, neighbor) == false)
                    {
                        continue;
                    }

                    BlockHitResult hit = new BlockHitResult(hitVec, face, neighbor, false);

                    for (float[] rotation : rotations)
                    {
                        player.setYRot(rotation[0]);
                        player.setXRot(rotation[1]);
                        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, held, hit);

                        if (context.canPlace() == false || context.getClickedPos().equals(pos) == false)
                        {
                            break;
                        }

                        BlockState predicted = block.getStateForPlacement(context);

                        if (predicted != null && matches(predicted, wanted))
                        {
                            return new Placement(hit, rotation[0], rotation[1]);
                        }
                    }
                }
            }
        }
        finally
        {
            player.setYRot(yaw);
            player.setXRot(pitch);
        }

        return null;
    }

    private static boolean matches(BlockState predicted, BlockState wanted)
    {
        if (predicted == wanted)
        {
            return true;
        }

        if (predicted.getBlock() != wanted.getBlock())
        {
            return false;
        }

        for (Property<?> property : wanted.getProperties())
        {
            if (IGNORED_PROPERTIES.contains(property.getName()) == false &&
                predicted.getValue(property).equals(wanted.getValue(property)) == false)
            {
                return false;
            }
        }

        return true;
    }

    private static boolean needsOpening(BlockState wanted)
    {
        if (wanted.hasProperty(BlockStateProperties.OPEN) == false || wanted.getValue(BlockStateProperties.OPEN) == false)
        {
            return false;
        }

        return wanted.is(BlockTags.WOODEN_TRAPDOORS, state -> true) ||
               wanted.is(BlockTags.WOODEN_DOORS, state -> true) ||
               wanted.is(BlockTags.FENCE_GATES, state -> true);
    }

    private static void open(Minecraft mc, LocalPlayer player, BlockPos pos)
    {
        BlockState state = mc.level.getBlockState(pos);

        if (state.hasProperty(BlockStateProperties.OPEN) == false || state.getValue(BlockStateProperties.OPEN))
        {
            return;
        }

        boolean sneaking = player.isShiftKeyDown();
        Input keys = player.input.keyPresses;

        if (sneaking)
        {
            Input released = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(), keys.jump(), false, keys.sprint());
            player.input.keyPresses = released;
            player.setShiftKeyDown(false);
            mc.getConnection().send(new ServerboundPlayerInputPacket(released));
        }

        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);

        if (sneaking)
        {
            player.input.keyPresses = keys;
            player.setShiftKeyDown(true);
            mc.getConnection().send(new ServerboundPlayerInputPacket(keys));
        }
    }

    private static Vec3 hitVector(BlockPos neighbor, Direction face, double height)
    {
        double x = neighbor.getX() + 0.5 + face.getStepX() * 0.5;
        double z = neighbor.getZ() + 0.5 + face.getStepZ() * 0.5;
        double y = face.getAxis() == Direction.Axis.Y ? neighbor.getY() + 0.5 + face.getStepY() * 0.5 : neighbor.getY() + height;
        return new Vec3(x, y, z);
    }

    private static boolean isVisible(Level level, LocalPlayer player, Vec3 eye, Vec3 hitVec, BlockPos neighbor)
    {
        Vec3 target = hitVec.add(Vec3.atCenterOf(neighbor).subtract(hitVec).scale(0.02));
        BlockHitResult result = level.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return result.getType() == HitResult.Type.MISS || result.getBlockPos().equals(neighbor);
    }

    private static void place(Minecraft mc, LocalPlayer player, Placement placement)
    {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        boolean rotate = yaw != placement.yaw() || pitch != placement.pitch();

        if (rotate)
        {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(placement.yaw(), placement.pitch(), player.onGround(), player.horizontalCollision));
            player.setYRot(placement.yaw());
            player.setXRot(placement.pitch());
        }

        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, placement.hit());

        if (rotate)
        {
            player.setYRot(yaw);
            player.setXRot(pitch);
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), player.horizontalCollision));
        }
    }
}
