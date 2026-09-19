package io.github.lwozr.buildhelper.event;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.lwozr.buildhelper.Reference;
import io.github.lwozr.buildhelper.config.Configs;
import io.github.lwozr.buildhelper.printer.Printer;
import io.github.lwozr.buildhelper.render.HudRenderer;
import io.github.lwozr.buildhelper.render.MaterialTooltip;
import io.github.lwozr.buildhelper.render.TargetRenderer;

public class RenderHandler implements IRenderer
{
    private static boolean litematicaRenderingEnabled()
    {
        return fi.dy.masa.litematica.config.Configs.Visuals.ENABLE_RENDERING.getBooleanValue();
    }

    @Override
    public void onRenderWorldLast(RenderTarget fb, Matrix4fc posMatrix, CameraRenderState cameraState, Frustum culling, RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler)
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
            GuiUtils.getCurrentScreen() == null && ctx.mc().options.hideGui == false)
        {
            profiler.push(Reference.MOD_ID + "_hud");
            HudRenderer.render(ctx);
            profiler.pop();
        }

        if (ctx.mc().player != null && Printer.isEnabled() && GuiUtils.getCurrentScreen() == null)
        {
            String label = StringUtils.translate(Reference.MOD_ID + ".hud.printer_on");
            int x = GuiUtils.getScaledWindowWidth() / 2 - ctx.mc().font.width(label) / 2;
            int y = GuiUtils.getScaledWindowHeight() - 72;
            ctx.drawString(ctx.mc().font, label, x, y, 0xFFFFB040, true);
        }
    }

    @Override
    public void onRenderTooltipComponentInsertLast(Item.TooltipContext context, ItemStack stack, Consumer<Component> list)
    {
        if (Configs.Generic.CONTAINER_TOOLTIP.getBooleanValue() && stack.isEmpty() == false)
        {
            MaterialTooltip.append(stack, list);
        }
    }

    @Override
    public Supplier<String> getProfilerSectionSupplier()
    {
        return () -> Reference.MOD_ID + "_render_handler";
    }
}
