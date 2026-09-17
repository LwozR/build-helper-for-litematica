package io.github.lwozr.buildhelper.render;

import java.util.List;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.core.BlockPos;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import io.github.lwozr.buildhelper.config.Configs;

public class TargetRenderer
{
    public static void render()
    {
        List<BlockPos> targets = BuildScanner.getInstance().getHeldTargets();

        if (targets.isEmpty())
        {
            return;
        }

        boolean throughBlocks = Configs.Generic.HIGHLIGHT_THROUGH_BLOCKS.getBooleanValue();
        Color4f color = Configs.Colors.HIGHLIGHT_COLOR.getColor();
        Color4f lineColor = new Color4f(color.r, color.g, color.b, 1.0f);

        RenderContext ctx = new RenderContext(() -> "buildhelper_litematica:targets/lines",
                throughBlocks ? MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL : MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);
        BufferBuilder buffer = ctx.getBuilder();

        for (BlockPos pos : targets)
        {
            RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(pos, lineColor, 0.003, 2.0f, buffer);
        }

        draw(ctx, buffer, true);
        ctx.reset();

        buffer = ctx.start(() -> "buildhelper_litematica:targets/sides",
                throughBlocks ? MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL : MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH);

        for (BlockPos pos : targets)
        {
            RenderUtils.renderAreaSidesBatched(pos, pos, color, 0.003, buffer);
        }

        draw(ctx, buffer, false);

        try
        {
            ctx.close();
        }
        catch (Exception ignored)
        {
        }
    }

    private static void draw(RenderContext ctx, BufferBuilder buffer, boolean lines)
    {
        try
        {
            MeshData meshData = buffer.build();

            if (meshData != null)
            {
                ctx.draw(meshData, false, lines);
                meshData.close();
            }
        }
        catch (Exception ignored)
        {
        }
    }
}
