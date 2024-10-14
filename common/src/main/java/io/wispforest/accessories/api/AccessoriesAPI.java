package io.wispforest.accessories.api;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryItemAttributeModifiers;
import io.wispforest.accessories.api.components.AccessoryStackSizeComponent;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.events.AdjustAttributeModifierCallback;
import io.wispforest.accessories.api.events.CanEquipCallback;
import io.wispforest.accessories.api.events.CanUnequipCallback;
import io.wispforest.accessories.api.slot.*;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.client.AccessoryBreak;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

/**
 * Class containing the bulk of API calls for either registering {@link Accessory} instances, new {@link SlotBasedPredicate}s and more.
 */
public class AccessoriesAPI {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * @deprecated Internal API, will become private in the future.
     */
    @ApiStatus.Internal
    public static final Accessory DEFAULT = new Accessory() {
        @Override
        public int maxStackSize(ItemStack stack) {
            var data = stack.getOrDefault(AccessoriesDataComponents.STACK_SIZE, AccessoryStackSizeComponent.DEFAULT);

            if(data.useStackSize()) return stack.getMaxStackSize();

            return Math.min(Math.max(data.sizeOverride(), 1), stack.getMaxStackSize());
        }
    };

    @ApiStatus.Internal
    private static final AccessoryNest DEFAULT_NEST = new AccessoryNest() {
        @Override
        public int maxStackSize(ItemStack stack) {
            var data = stack.getOrDefault(AccessoriesDataComponents.STACK_SIZE, AccessoryStackSizeComponent.DEFAULT);

            if(data.useStackSize()) return stack.getMaxStackSize();

            return Math.min(Math.max(data.sizeOverride(), 1), stack.getMaxStackSize());
        }
    };

    private static final Map<ResourceLocation, SlotBasedPredicate> PREDICATE_REGISTRY = new HashMap<>();

    private static final Map<Item, Accessory> REGISTER = new HashMap<>();

    @ApiStatus.Internal
    public static Map<Item, Accessory> getAllAccessories() {
        return Collections.unmodifiableMap(REGISTER);
    }

    //--

    /**
     * Registers an accessory implementation for a given item.
     */
    public static void registerAccessory(Item item, Accessory accessory) {
        REGISTER.put(item, accessory);
    }

    /**
     * @return the accessory bound to this stack or {@code null} if there is none
     */
    @Nullable
    public static Accessory getAccessory(ItemStack stack){
        return getAccessory(stack.getItem());
    }

    /**
     * @return the accessory bound to this item or {@code null} if there is none
     */
    @Nullable
    public static Accessory getAccessory(Item item) {
        return REGISTER.get(item);
    }

    /**
     * @return the accessory bound to this stack or {@link #defaultAccessory()} if there is none
     */
    public static Accessory getOrDefaultAccessory(ItemStack stack){
        var accessory = REGISTER.get(stack.getItem());

        if(accessory == null) {
            accessory = stack.has(AccessoriesDataComponents.NESTED_ACCESSORIES) ? DEFAULT_NEST : DEFAULT;
        }

        return accessory;
    }

    /**
     * @return the accessory bound to this item or {@link #defaultAccessory()} if there is none
     */
    public static Accessory getOrDefaultAccessory(Item item){
        return REGISTER.getOrDefault(item, DEFAULT);
    }

    /**
     * @return the default accessory implementation
     */
    public static Accessory defaultAccessory(){
        return DEFAULT;
    }

    public static boolean isDefaultAccessory(Accessory accessory) {
        return accessory == DEFAULT || accessory == DEFAULT_NEST;
    }

    /**
     * @return If a given {@link ItemStack} is found either to have an {@link Accessory} besides the
     * default or if the given stack has valid slots which it can be equipped
     */
    public static boolean isValidAccessory(ItemStack stack, Level level){
        return isValidAccessory(stack, level, null);
    }

    public static boolean isValidAccessory(ItemStack stack, Level level, @Nullable LivingEntity entity){
        return !isDefaultAccessory(getOrDefaultAccessory(stack))
                || !getStackSlotTypes(level, entity, stack).isEmpty();
    }

    //--

    public static AccessoryAttributeBuilder getAttributeModifiers(ItemStack stack, SlotReference slotReference){
        return getAttributeModifiers(stack, slotReference, false);
    }

    public static AccessoryAttributeBuilder getAttributeModifiers(ItemStack stack, SlotReference slotReference, boolean useTooltipCheck){
        return getAttributeModifiers(stack, slotReference.entity(), slotReference.slotName(), slotReference.slot(), useTooltipCheck);
    }

    @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
    @Deprecated(forRemoval = true)
    public static AccessoryAttributeBuilder getAttributeModifiers(ItemStack stack, String slotName, int slot){
        return getAttributeModifiers(stack, null, slotName, slot);
    }

    public static AccessoryAttributeBuilder getAttributeModifiers(ItemStack stack, @Nullable LivingEntity entity, String slotName, int slot){
        return getAttributeModifiers(stack, entity, slotName, slot, false);
    }

    /**
     * Attempts to get any at all AttributeModifier's found on the stack either though NBT or the Accessory bound
     * to the {@link ItemStack}'s item
     */
    public static AccessoryAttributeBuilder getAttributeModifiers(ItemStack stack, @Nullable LivingEntity entity, String slotName, int slot, boolean hideTooltipIfDisabled){
        var slotReference = SlotReference.of(entity, slotName, slot);

        var builder = new AccessoryAttributeBuilder(slotReference);

        AccessoryNestUtils.recursiveStackConsumption(stack, slotReference, (innerStack, innerRef) -> {
            var component = innerStack.getOrDefault(AccessoriesDataComponents.ATTRIBUTES, AccessoryItemAttributeModifiers.EMPTY);

            var innerBuilder = (!hideTooltipIfDisabled || component.showInTooltip())
                    ? component.gatherAttributes(innerRef)
                    : new AccessoryAttributeBuilder(slotName, slot);

            builder.addFrom(innerBuilder);
        });

        if(entity != null) {
            //TODO: Decide if the presence of modifiers prevents the accessory modifiers from existing
            var accessory = AccessoriesAPI.getAccessory(stack);

            if(accessory != null) accessory.getDynamicModifiers(stack, slotReference, builder);

            AdjustAttributeModifierCallback.EVENT.invoker().adjustAttributes(stack, slotReference, builder);
        }

        return builder;
    }

    public static void addAttribute(ItemStack stack, String slotName, Holder<Attribute> attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation, boolean isStackable) {
        stack.update(
                AccessoriesDataComponents.ATTRIBUTES,
                new AccessoryItemAttributeModifiers(List.of(), true),
                modifiers -> modifiers.withModifierAdded(attribute, new AttributeModifier(location, amount, operation), slotName, isStackable)
        );
    }

    //--

    /**
     * @return {@link UUID} based on the provided {@link SlotType#name} and entry index
     */
    public static ResourceLocation createSlotLocation(SlotType slotType, int index) {
        return createSlotLocation(slotType.name(), index);
    }

    /**
     * @return {@link UUID} based on the provided slot name and entry index
     */
    public static ResourceLocation createSlotLocation(String slotName, int index) {
        return Accessories.of(slotName.replace(":", "_") + "/" + index);
    }

    //--

    /**
     * Used to check if the given {@link ItemStack} is valid for the given LivingEntity and SlotReference
     * based on {@link SlotBasedPredicate}s bound to the Slot and the {@link Accessory} bound to the stack if present
     */
    public static boolean canInsertIntoSlot(ItemStack stack, SlotReference reference){
        var slotType = reference.type();

        if(slotType == null) {
            throw new IllegalStateException("Unable to get the needed SlotType from the SlotReference passed within `canInsertIntoSlot`! [Name: " + reference.slotName() + "]");
        }

        return getPredicateResults(slotType.validators(), reference.entity().level(), reference.entity(), slotType, 0, stack) && canEquip(stack, reference);
    }

    /**
     * Method used to check weather or not the given stack can be equipped within the slot referenced
     *
     * @param stack
     * @param reference
     * @return if the stack can be equipped or not
     */
    public static boolean canEquip(ItemStack stack, SlotReference reference){
        var result = CanEquipCallback.EVENT.invoker().canEquip(stack, reference);

        if(!result.equals(TriState.DEFAULT)) return result.orElse(true);

        return getOrDefaultAccessory(stack).canEquip(stack, reference);
    }

    /**
     * Method used to check weather or not the given stack can be unequipped within the slot referenced
     *
     * @param stack
     * @param reference
     * @return if the stack can be unequipped or not
     */
    public static boolean canUnequip(ItemStack stack, SlotReference reference){
        var result = CanUnequipCallback.EVENT.invoker().canUnequip(stack, reference);

        if(!result.equals(TriState.DEFAULT)) return result.orElse(true);

        return getOrDefaultAccessory(stack).canUnequip(stack, reference);
    }

    /**
     * @return All valid {@link SlotType}s for the given {@link ItemStack} based on the {@link LivingEntity}
     * available {@link SlotType}s
     */
    public static Collection<SlotType> getValidSlotTypes(LivingEntity entity, ItemStack stack){
        var slots = EntitySlotLoader.getEntitySlots(entity);

        var validSlots = new ArrayList<SlotType>();

        var capability = AccessoriesCapability.get(entity);

        if(capability != null) {
            var containers = capability.getContainers();

            for (SlotType value : slots.values()) {
                if (!containers.containsKey(value.name())) continue;

                var container = containers.get(value.name());

                var size = containers.get(value.name()).getSize();

                if(size == 0) size = 1;

                for (int i = 0; i < size; i++) {
                    var reference = SlotReference.of(entity, container.getSlotName(), i);

                    if (canInsertIntoSlot(stack, reference)) {
                        validSlots.add(value);

                        break;
                    }
                }
            }
        }

        return validSlots;
    }

    public static Collection<SlotType> getStackSlotTypes(Level level, ItemStack stack){
        return getStackSlotTypes(level, null, stack);
    }

    public static Collection<SlotType> getStackSlotTypes(LivingEntity entity, ItemStack stack) {
        return getStackSlotTypes(entity.level(), entity, stack);
    }

    public static Collection<SlotType> getStackSlotTypes(Level level, @Nullable LivingEntity entity, ItemStack stack) {
        var validSlots = new ArrayList<SlotType>();

        for (SlotType value : SlotTypeLoader.getSlotTypes(level).values()) {
            if(getPredicateResults(value.validators(), level, entity, value, 0, stack)) validSlots.add(value);
        }

        return validSlots;
    }

    public static Collection<SlotType> getUsedSlotsFor(Player player) {
        return getUsedSlotsFor(player, player.getInventory());
    }

    public static Collection<SlotType> getUsedSlotsFor(LivingEntity entity, Container container) {
        var capability = entity.accessoriesCapability();

        if(capability == null) return Set.of();

        var slots = new HashSet<SlotType>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);

            if (stack.isEmpty()) continue;

            slots.addAll(AccessoriesAPI.getValidSlotTypes(entity, stack));
        }

        for (var ref : capability.getAllEquipped()) {
            slots.addAll(AccessoriesAPI.getValidSlotTypes(entity, ref.stack()));
        }

        slots.addAll(SlotTypeLoader.getUsedSlotsByRegistryItem(entity));
        
        return slots;
    }

    /**
     * Helper method to trigger effects of a given accessory being broken on any tracking clients for the given entity
     */
    public static void breakStack(SlotReference reference){
        AccessoriesNetworking.sendToTrackingAndSelf(reference.entity(), AccessoryBreak.of(reference));
    }

    //--

    /**
     * @return {@link SlotBasedPredicate} bound to the given {@link ResourceLocation} or an Empty {@link Optional} if absent
     */
    @Nullable
    public static SlotBasedPredicate getPredicate(ResourceLocation location) {
        return PREDICATE_REGISTRY.get(location);
    }

    public static void registerPredicate(ResourceLocation location, SlotBasedPredicate predicate) {
        if(PREDICATE_REGISTRY.containsKey(location)) {
            LOGGER.warn("[AccessoriesAPI]: A SlotBasedPredicate attempted to be registered but a duplicate entry existed already! [Id: {}]", location);

            return;
        }

        PREDICATE_REGISTRY.put(location, predicate);
    }

    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, Level level, SlotType slotType, int index, ItemStack stack){
        return getPredicateResults(predicateIds, level, null, slotType, index, stack);
    }

    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, Level level, @Nullable LivingEntity entity, SlotType slotType, int index, ItemStack stack){
        var result = TriState.DEFAULT;

        for (var predicateId : predicateIds) {
            var predicate = getPredicate(predicateId);

            if(predicate == null) continue;

            if(predicate instanceof EntityBasedPredicate entityBasedPredicate) {
                result = entityBasedPredicate.isValid(level, entity, slotType, index, stack);
            } else {
                result = predicate.isValid(level, slotType, index, stack);
            }

            if(result != TriState.DEFAULT) break;
        }

        return result.orElse(false);
    }

    /**
     * @deprecated Use {@link AccessoriesTags#ANY_TAG} instead!
     */
    @Deprecated(forRemoval = true)
    public static final TagKey<Item> ALL_ACCESSORIES = AccessoriesTags.ALL_TAG;

    /**
     * @deprecated Use {@link AccessoriesTags#ANY_TAG} instead!
     */
    @Deprecated(forRemoval = true)
    public static final TagKey<Item> ANY_ACCESSORIES = AccessoriesTags.ANY_TAG;

    public static TagKey<Item> getSlotTag(SlotType slotType) {
        var location = UniqueSlotHandling.isUniqueSlot(slotType.name()) ? ResourceLocation.parse(slotType.name()) : Accessories.of(slotType.name());

        return TagKey.create(Registries.ITEM, location);
    }

    static {
        registerPredicate(AccessoriesBaseData.ALL_PREDICATE_ID, (level, slotType, i, stack) -> TriState.TRUE);
        registerPredicate(AccessoriesBaseData.NONE_PREDICATE_ID, (level, slotType, i, stack) -> TriState.FALSE);
        registerPredicate(AccessoriesBaseData.TAG_PREDICATE_ID, (level, slotType, i, stack) -> {
            return (stack.is(getSlotTag(slotType)) || stack.is(AccessoriesTags.ANY_TAG)) ? TriState.TRUE : TriState.DEFAULT;
        });
        registerPredicate(AccessoriesBaseData.RELEVANT_PREDICATE_ID, (level, slotType, i, stack) -> {
            var bl = !getAttributeModifiers(stack, null, slotType.name(), i).getAttributeModifiers(false).isEmpty();

            return bl ? TriState.TRUE : TriState.DEFAULT;
        });
        registerPredicate(AccessoriesBaseData.COMPONENT_PREDICATE_ID, (level, slotType, index, stack) -> {
            if(stack.has(AccessoriesDataComponents.SLOT_VALIDATION)) {
                var slotValidationData = stack.get(AccessoriesDataComponents.SLOT_VALIDATION);
                var name = slotType.name();

                //--

                var invalidSlots = slotValidationData.invalidSlotOverrides();

                for (var invalidSlot : invalidSlots) {
                    if (name.equals(invalidSlot)) return TriState.FALSE;
                }

                //--

                var validSlots = slotValidationData.validSlotOverrides();

                for (var validSlot : validSlots) {
                    if (validSlot.equals("any")) return TriState.TRUE;

                    if (name.equals(validSlot)) return TriState.TRUE;
                }
            }

            return TriState.DEFAULT;
        });
    }
}