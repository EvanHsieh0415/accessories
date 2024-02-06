package io.wispforest.accessories.fabric;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.impl.AccessoriesInternals;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.function.UnaryOperator;

public class AccessoriesAccessImpl {

    public static Optional<AccessoriesCapability> getCapability(LivingEntity livingEntity){
        return Optional.ofNullable(AccessoriesFabric.CAPABILITY.find(livingEntity, null));
    }

    public static AccessoriesHolder getHolder(LivingEntity livingEntity){
        return livingEntity.getAttachedOrCreate(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE);
    }

    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolder> modifier){
        var holder = getHolder(livingEntity);

        holder = modifier.apply(holder);

        livingEntity.setAttached(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public static AccessoriesNetworkHandler getNetworkHandler(){
        return AccessoriesFabricNetworkHandler.INSTANCE;
    }

    public static AccessoriesInternals getInternal(){
        return AccessoriesFabricInternals.INSTANCE;
    }

}
