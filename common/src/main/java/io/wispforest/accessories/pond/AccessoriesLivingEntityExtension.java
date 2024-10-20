package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.item.ItemStack;

public interface AccessoriesLivingEntityExtension {
    void onEquipItem(SlotReference slotReference, ItemStack oldItem, ItemStack newItem);
}
