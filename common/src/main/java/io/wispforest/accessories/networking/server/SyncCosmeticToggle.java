package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.api.events.AllowEntityModificationCallback;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record SyncCosmeticToggle(@Nullable Integer entityId, String slotName, int slotIndex)  {

    public static final StructEndec<SyncCosmeticToggle> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.nullableOf().fieldOf("entityId", SyncCosmeticToggle::entityId),
            Endec.STRING.fieldOf("slotName", SyncCosmeticToggle::slotName),
            Endec.VAR_INT.fieldOf("slotIndex", SyncCosmeticToggle::slotIndex),
            SyncCosmeticToggle::new
    );

    public static SyncCosmeticToggle of(@Nullable LivingEntity livingEntity, SlotType slotType, int slotIndex){
        return new SyncCosmeticToggle(livingEntity != null ? livingEntity.getId() : null, slotType.name(), slotIndex);
    }

    public static void handlePacket(SyncCosmeticToggle packet, Player player) {
        if(player.level().isClientSide()) return;

        LivingEntity targetEntity = player;

        if(packet.entityId() != null) {
            if(!(player.level().getEntity(packet.entityId()) instanceof LivingEntity livingEntity)) {
                return;
            }

            targetEntity = livingEntity;

            var result = AllowEntityModificationCallback.EVENT.invoker().allowModifications(targetEntity, player, null);

            if(!result.orElse(false)) return;
        }

        var capability = targetEntity.accessoriesCapability();

        if(capability == null) return;

        var slotType = SlotTypeLoader.getSlotType(player.level(), packet.slotName());

        if(slotType == null) return;

        var container = capability.getContainer(slotType);

        var renderOptions = container.renderOptions();

        renderOptions.set(packet.slotIndex(), !container.shouldRender(packet.slotIndex()));

        container.markChanged(false);
    }
}
