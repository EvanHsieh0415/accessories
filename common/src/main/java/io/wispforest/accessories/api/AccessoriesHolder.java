package io.wispforest.accessories.api;

import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.impl.PlayerEquipControl;
import io.wispforest.accessories.impl.caching.AccessoriesHolderLookupCache;
import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

/**
 * Internal Holder object that has all the container data attached to the given player
 */
public interface AccessoriesHolder {

    @Nullable
    static AccessoriesHolder get(@NotNull LivingEntity livingEntity){
        return ((AccessoriesAPIAccess) livingEntity).accessoriesHolder();
    }

    static Optional<AccessoriesHolder> getOptionally(@NotNull LivingEntity livingEntity){
        return Optional.ofNullable(get(livingEntity));
    }

    AccessoriesHolderLookupCache getLookupCache();

    //--

    PlayerEquipControl equipControl();

    AccessoriesHolder equipControl(PlayerEquipControl value);

    //--

    /**
     * @return If unused accessory slots should be present within the {@link AccessoriesScreen}
     */
    boolean showUnusedSlots();

    AccessoriesHolder showUnusedSlots(boolean value);



    /**
     * @return If the cosmetic slots should be shown within the {@link AccessoriesScreen}
     */
    boolean cosmeticsShown();

    AccessoriesHolder cosmeticsShown(boolean value);

    boolean showAdvancedOptions();

    AccessoriesHolder showAdvancedOptions(boolean value);

    int columnAmount();

    AccessoriesHolder columnAmount(int value);

    int widgetType();

    AccessoriesHolder widgetType(int value);

    boolean showGroupFilter();

    AccessoriesHolder showGroupFilter(boolean value);

    boolean isGroupFiltersOpen();

    AccessoriesHolder isGroupFiltersOpen(boolean value);

    Set<String> filteredGroups();

    AccessoriesHolder filteredGroups(Set<String> value);

    boolean mainWidgetPosition();

    AccessoriesHolder mainWidgetPosition(boolean value);

    boolean sideWidgetPosition();

    AccessoriesHolder sideWidgetPosition(boolean value);

    boolean showCraftingGrid();

    AccessoriesHolder showCraftingGrid(boolean value);

    //--

    @Deprecated(forRemoval = true)
    default boolean showUniqueSlots() {
        return false;
    }

    @Deprecated(forRemoval = true)
    default AccessoriesHolder showUniqueSlots(boolean value) {
        return this;
    }

    @Deprecated(forRemoval = true)
    default boolean linesShown() {
        return false;
    }

    @Deprecated(forRemoval = true)
    default AccessoriesHolder linesShown(boolean value) {
        return this;
    }
}