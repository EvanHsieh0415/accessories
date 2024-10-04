package io.wispforest.tclayer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AccessoriesExperimentalScreen.class)
public abstract class AccessoriesExperimentalScreenMixin {

    @WrapOperation(
            method = "renderSlotTexture(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"))
    private static void adjustSlotRender(GuiGraphics instance, int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite, Operation<Void> original, @Local(argsOnly = true) Slot slot, @Local(ordinal = 0) Pair<ResourceLocation, ResourceLocation> pair) {
        if(slot instanceof AccessoriesBasedSlot && sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            var location = ResourceLocation.fromNamespaceAndPath(pair.getSecond().getNamespace(), "textures/" + pair.getSecond().getPath() + ".png");

            instance.blit(location, x, y, blitOffset, 0, 0, width, height, width, height);
        } else {
            original.call(instance, x, y, blitOffset, width, height, sprite);
        }
    }
}
