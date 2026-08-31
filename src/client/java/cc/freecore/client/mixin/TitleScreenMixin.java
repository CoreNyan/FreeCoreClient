package cc.freecore.client.mixin;

import cc.freecore.client.ConfigManager;
import cc.freecore.client.BackgroundManager;
import cc.freecore.client.FreeCoreConfig;
import cc.freecore.client.GuiActions;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void freecore$injectButtons(CallbackInfo ci) {
        // Remove vanilla Realms/promotional entries that are irrelevant for the dedicated client.
        for (Element element : List.copyOf(children())) {
            if (element instanceof ButtonWidget button && button.getMessage().getString().toLowerCase().contains("realm")) {
                remove(element);
            }
        }
        for (FreeCoreConfig.ButtonConfig button : ConfigManager.get().buttons) {
            if (button == null || button.label == null) continue;
            addDrawableChild(ButtonWidget.builder(Text.literal(button.label), ignored -> GuiActions.perform(this, button))
                    .dimensions(width / 2 - 100, height / 4 + 72 + 24 * children().size(), 200, 20).build());
        }
    }

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
    private void freecore$customBackground(GuiGraphics graphics, float delta, CallbackInfo ci) {
        if (BackgroundManager.render(graphics, width, height)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void freecore$renderBrand(DrawContext graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        graphics.drawTextWithShadow(textRenderer, "FreeCore", 8, 8, 0xFFFFFFFF);
    }
}
