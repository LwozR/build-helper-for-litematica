package io.github.lwozr.buildhelper.event;

import java.util.function.Supplier;
import org.joml.Vector4f;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.GuiUtils;
import io.github.lwozr.buildhelper.Reference;
import io.github.lwozr.buildhelper.config.Configs;
import io.github.lwozr.buildhelper.render.HudRenderer;
import io.github.lwozr.buildhelper.render.TargetRenderer;

public class RenderHandler implements IRenderer
{
    private static boolean litematicaRenderingEnabled()
    {
        return fi.dy.masa.litematica.config.Configs.Visuals.ENABLE_RENDERING.getBooleanValue();
    }

    @Override
    public void onRenderWorldLast(RenderTarget fb, CameraRenderState cameraState, Frustum culling, RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && litematicaRenderingEnabled() && Configs.Generic.HIGHLIGHT_HELD_BLOCK.getBooleanValue())
        {
            profiler.push(Reference.MOD_ID + "_targets");
            TargetRenderer.render();
            profiler.pop();
        }
    }

    @Override
    public void onExtractGuiOverlayPost(GuiContext ctx, float partialTicks, ProfilerFiller profiler)
    {
        if (ctx.mc().player != null && litematicaRenderingEnabled() && Configs.Generic.HUD_ENABLED.getBooleanValue() &&
            GuiUtils.getCurrentScreen() == null && ctx.mc().gui.hud.isHidden() == false)
        {
            profiler.push(Reference.MOD_ID + "_hud");
            HudRenderer.render(ctx);
            profiler.pop();
        }
    }

    @Override
    public Supplier<String> getProfilerSectionSupplier()
    {
        return () -> Reference.MOD_ID + "_render_handler";
    }
}
