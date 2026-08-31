package cc.freecore.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ProgressScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the vanilla dirt background used by "Saving world..." progress UI. */
@Mixin(ProgressScreen.class)
public abstract class ProgressScreenMixin26 {
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void freecore$progressAnimation(GuiGraphicsExtractor graphics, int width, int height,
                                             float delta, CallbackInfo ci) {
        double t = System.nanoTime() / 1_000_000_000.0;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int cx = w / 2;
        int cy = h / 2 + 10;
        int barW = Math.min(220, Math.max(120, w / 3));
        int phase = (int) ((Math.sin(t * 1.8) + 1.0) * 0.5 * (barW - 34));
        graphics.fill(cx - barW / 2, cy, cx + barW / 2, cy + 2, 0x55ffffff);
        graphics.fill(cx - barW / 2 + phase, cy, cx - barW / 2 + phase + 34, cy + 2, 0xddffffff);
    }
}
