package cc.freecore.client.mixin;

import cc.freecore.client.FreeCoreClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers the short resource/save message screen without changing its lifecycle. */
@Mixin(GenericMessageScreen.class)
public abstract class GenericMessageScreenMixin26 {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void freecore$messageBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                             float delta, CallbackInfo ci) {
        FreeCoreClientRuntime.renderBackground(graphics, graphics.guiWidth(), graphics.guiHeight());
        ci.cancel();
    }
}
