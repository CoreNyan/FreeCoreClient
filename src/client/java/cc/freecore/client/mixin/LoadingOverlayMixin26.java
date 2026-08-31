package cc.freecore.client.mixin;

import cc.freecore.client.FreeCoreClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Monochrome Mojang Studios loading screen, configurable through JSON. */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin26 {
    /**
     * Draw over the vanilla loading frame after it has updated its fade/progress
     * state. Cancelling this method prevents LoadingOverlay from ever reaching
     * its normal fade-out completion path.
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void freecore$loading(GuiGraphicsExtractor graphics, int width, int height, float delta, CallbackInfo ci) {
        var cfg = FreeCoreClientRuntime.getConfig();
        if (!cfg.loadingScreenEnabled) return;

        // Overlay callbacks receive framebuffer dimensions that can differ from
        // the logical GUI coordinate space (especially with GUI scale > 1).
        // Always use the extractor's logical size, otherwise the vanilla red
        // BRAND_BACKGROUND remains exposed on the right/bottom edges.
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();

        // Put the custom loading layer on top of the vanilla frame. The opaque
        // color also hides the vanilla progress bar while keeping its state alive.
        graphics.nextStratum();
        graphics.fill(0, 0, guiWidth, guiHeight, cfg.loadingScreenColor | 0xff000000);

        // Mojang's texture is a 120x120 sheet: the upper and lower halves are
        // rendered side by side. Using the native layout avoids stretching the
        // logo (the old 256x64 blit used the wrong source dimensions).
        int scale = Math.max(1, cfg.loadingLogoScale);
        int base = (int) (Math.min(guiWidth * 0.75d, guiHeight) * 0.25d * scale);
        if (base <= 0) return;
        int halfHeight = base / 2;
        int halfWidth = base * 2;
        int centerX = guiWidth / 2;
        int centerY = guiHeight / 2;
        int color = 0xffffffff;
        graphics.blit(RenderPipelines.MOJANG_LOGO, LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION,
                centerX - halfWidth, centerY - halfHeight,
                -0.0625f, 0.0f, halfWidth, base, 120, 60, 120, 120, color);
        graphics.blit(RenderPipelines.MOJANG_LOGO, LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION,
                centerX, centerY - halfHeight,
                0.0625f, 60.0f, halfWidth, base, 120, 60, 120, 120, color);
    }
}
