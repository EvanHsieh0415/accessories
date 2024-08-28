package io.wispforest.accessories.networking.client;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.endec.RegistriesAttribute;
import io.wispforest.accessories.impl.AccessoriesContainerImpl;
import io.wispforest.accessories.networking.BaseAccessoriesPacket;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.*;

/**
 * Catch all packet for handling syncing of containers and accessories within the main container
 * and cosmetic variant with the ability for it to be sync separately
 */
public record SyncContainerData(int entityId, Map<String, NbtMapCarrier> updatedContainers, Map<String, ItemStack> dirtyStacks, Map<String, ItemStack> dirtyCosmeticStacks) implements BaseAccessoriesPacket {

    public static Endec<SyncContainerData> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", SyncContainerData::entityId),
            NbtMapCarrier.ENDEC.mapOf().fieldOf("updatedContainers", SyncContainerData::updatedContainers),
            CodecUtils.ofCodec(ItemStack.OPTIONAL_CODEC).mapOf().fieldOf("dirtyStacks", SyncContainerData::dirtyStacks),
            CodecUtils.ofCodec(ItemStack.OPTIONAL_CODEC).mapOf().fieldOf("dirtyCosmeticStacks", SyncContainerData::dirtyCosmeticStacks),
            SyncContainerData::new
    );

    public static SyncContainerData of(LivingEntity livingEntity, Collection<AccessoriesContainer> updatedContainers, Map<String, ItemStack> dirtyStacks, Map<String, ItemStack> dirtyCosmeticStacks){
        var updatedContainerTags = new HashMap<String, NbtMapCarrier>();

        for (AccessoriesContainer updatedContainer : updatedContainers) {
            var syncCarrier = NbtMapCarrier.of();

            ((AccessoriesContainerImpl) updatedContainer).write(syncCarrier, SerializationContext.attributes(RegistriesAttribute.of(livingEntity.registryAccess())), true);

            updatedContainerTags.put(updatedContainer.getSlotName(), syncCarrier);
        }

        return new SyncContainerData(livingEntity.getId(), updatedContainerTags, dirtyStacks, dirtyCosmeticStacks);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Player player) {
        var level = player.level();

        var entity = level.getEntity(entityId);

        if(entity == null) {
            LOGGER.error("Unable to Sync Container Data for a given Entity as it is null on the Client! [EntityId: {}]", entityId);

            return;
        }

        if(!(entity instanceof LivingEntity livingEntity)) return;

        var capability = AccessoriesCapability.get(livingEntity);

        if(capability == null) {
            LOGGER.error("Unable to Sync Container Data for a given Entity as its Capability is null on the Client! [EntityId: {}]", entityId);

            return;
        }

        var containers = capability.getContainers();

        var aContainerHasResized = false;

        //--

        Set<String> invalidSyncedContainers = new HashSet<>();

        for (var entry : this.updatedContainers.entrySet()) {
            if (!containers.containsKey(entry.getKey())) {
                invalidSyncedContainers.add(entry.getKey());

                continue;
            }

            var container = containers.get(entry.getKey());

            ((AccessoriesContainerImpl) container).read(entry.getValue(), SerializationContext.attributes(RegistriesAttribute.of(player.level().registryAccess())), true);

            if (container.getAccessories().wasNewlyConstructed()) aContainerHasResized = true;
        }

        if(!invalidSyncedContainers.isEmpty()) {
            LOGGER.warn("Unable to sync container data for the following containers: {}", invalidSyncedContainers);
        }

        //--

        Set<String> invalidDirtyStackContainers = new HashSet<>();

        for (var entry : dirtyStacks.entrySet()) {
            var parts = entry.getKey().split("/");

            var slot = parts[0];

            if(!containers.containsKey(slot)) {
                invalidDirtyStackContainers.add(slot);

                continue;
            }

            var container = containers.get(slot);

            try {
                container.getAccessories().setItem(Integer.parseInt(parts[1]), entry.getValue());
            } catch (NumberFormatException ignored){}
        }

        if(!invalidDirtyStackContainers.isEmpty()) {
            LOGGER.warn("Unable to sync dirty stack data for the following containers: {}", invalidSyncedContainers);
        }

        //--

        Set<String> invalidDirtyCosmeticContainers = new HashSet<>();

        for (var entry : dirtyCosmeticStacks.entrySet()) {
            var parts = entry.getKey().split("/");

            var slot = parts[0];

            if(!containers.containsKey(slot)) {
                invalidDirtyCosmeticContainers.add(slot);

                continue;
            }

            var container = containers.get(slot);

            try {
                container.getCosmeticAccessories().setItem(Integer.parseInt(parts[1]), entry.getValue());
            } catch (NumberFormatException ignored){}
        }

        if(!invalidDirtyCosmeticContainers.isEmpty()) {
            LOGGER.warn("Unable to sync dirty stack data for the following containers: {}", invalidSyncedContainers);
        }

        //--

        if(player.containerMenu instanceof AccessoriesMenu menu && aContainerHasResized) {
            menu.reopenMenu();
            //AccessoriesClient.attemptToOpenScreen();
        }
    }
}
