package io.github.lwozr.buildhelper.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import io.github.lwozr.buildhelper.render.ContainerHighlighter;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen
{
    @Inject(method = "extractSlots", at = @At("TAIL"))
    private void buildhelper_highlightSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci)
    {
        ContainerHighlighter.render(graphics, (AbstractContainerScreen<?>) (Object) this);
    }
}
