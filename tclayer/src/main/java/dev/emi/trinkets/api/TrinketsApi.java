package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Function3;
import com.mojang.logging.LogUtils;
import dev.emi.trinkets.TrinketSlotTarget;
import dev.emi.trinkets.compat.*;
import dev.emi.trinkets.data.EntitySlotLoader;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.EntityBasedPredicate;
import io.wispforest.accessories.api.slot.SlotBasedPredicate;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.slf4j.Logger;

import java.util.*;

public class TrinketsApi implements EntityComponentInitializer {
    public static final ComponentKey<TrinketComponent> TRINKET_COMPONENT = ComponentRegistryV3.INSTANCE
            .getOrCreate(ResourceLocation.fromNamespaceAndPath(TrinketConstants.MOD_ID, "trinkets"), TrinketComponent.class);
    private static final Map<ResourceLocation, Function3<ItemStack, SlotReference, LivingEntity, TriState>> PREDICATES = new HashMap<>();

    private static final Map<Item, Trinket> TRINKETS = new HashMap<>();
    private static final Trinket DEFAULT_TRINKET;

    public static void registerTrinket(Item item, Trinket trinket) {
        AccessoriesAPI.registerAccessory(item, new WrappedTrinket(trinket));
        TRINKETS.put(item, trinket);
    }

    public static Trinket getTrinket(Item item) {
        var trinket = TRINKETS.get(item);

        if(trinket == null) {
            var accessory = AccessoriesAPI.getAccessory(item);

            if(accessory != null) trinket = new WrappedAccessory(accessory);
        }

        //TODO: Maybe check for valid slots if any and return different wrapped accessory as a way of indicating this item is for sure a valid trinket for things like Compound Accessories
        if(trinket == null) return DEFAULT_TRINKET;

        return trinket;
    }

    public static Trinket getDefaultTrinket() {
        return DEFAULT_TRINKET;
    }

    public static Optional<TrinketComponent> getTrinketComponent(LivingEntity livingEntity) {
        if(livingEntity == null) return Optional.empty();

        var capability = AccessoriesCapability.get(livingEntity);

        return Optional.of(capability != null ? new LivingEntityTrinketComponent(livingEntity) : new EmptyComponent(livingEntity));
    }

    public static void onTrinketBroken(ItemStack stack, SlotReference ref, LivingEntity entity) {
        var slotName = ((WrappedTrinketInventory) ref.inventory()).container.getSlotName();

        var reference = io.wispforest.accessories.api.slot.SlotReference.of(entity, slotName, ref.index());

        AccessoriesAPI.breakStack(reference);
    }

    @Deprecated
    public static Map<String, SlotGroup> getPlayerSlots() {
        return getEntitySlots(EntityType.PLAYER);
    }

    public static Map<String, SlotGroup> getPlayerSlots(Level world) {
        return getEntitySlots(world, EntityType.PLAYER);
    }

    public static Map<String, SlotGroup> getPlayerSlots(Player player) {
        return getEntitySlots(player);
    }

    @Deprecated
    public static Map<String, SlotGroup> getEntitySlots(EntityType<?> type) {
        return EntitySlotLoader.SERVER.getEntitySlots(type);
    }

    public static Map<String, SlotGroup> getEntitySlots(Level world, EntityType<?> type) {
        return ((world.isClientSide) ? EntitySlotLoader.CLIENT : EntitySlotLoader.SERVER).getEntitySlots(type);
    }

    public static Map<String, SlotGroup> getEntitySlots(Entity entity) {
        if (entity != null) return getEntitySlots(entity.level(), entity.getType());

        return ImmutableMap.of();
    }

    public static void registerTrinketPredicate(ResourceLocation id, Function3<ItemStack, SlotReference, LivingEntity, TriState> predicate) {
        PREDICATES.put(id, predicate);

        AccessoriesAPI.registerPredicate(id, new SafeSlotBasedPredicate(id, predicate));
    }

    public static Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> getTrinketPredicate(ResourceLocation id) {
        return Optional.ofNullable(PREDICATES.get(id));
    }

    public static boolean evaluatePredicateSet(Set<ResourceLocation> set, ItemStack stack, SlotReference ref, LivingEntity entity) {
        var convertedSet = new HashSet<ResourceLocation>();

        for (var location : set) {
            var converetdLocation = switch (location.toString()){
                case "trinkets:all" -> Accessories.of("all");
                case "trinkets:none" -> Accessories.of("none");
                case "trinkets:tag" -> Accessories.of("tag");
                case "trinkets:relevant" -> Accessories.of("relevant");
                default -> location;
            };

            convertedSet.add(converetdLocation);
        }

        var slotName = ((WrappedTrinketInventory) ref.inventory()).container.getSlotName();

        var slotType = SlotTypeLoader.getSlotType(entity.level(), slotName);

        if(slotType == null) {
            throw new IllegalStateException("Unable to get a SlotType using the WrappedTrinketInventory from the SlotTypeLoader! [Name: " + slotName +"]");
        }

        return AccessoriesAPI.getPredicateResults(convertedSet, entity.level(), slotType, ref.index(), stack);
    }

    public static Enchantment.EnchantmentDefinition withTrinketSlots(Enchantment.EnchantmentDefinition definition, Set<String> slots) {
        var def = new Enchantment.EnchantmentDefinition(definition.supportedItems(), definition.primaryItems(), definition.weight(), definition.maxLevel(),
                definition.minCost(), definition.maxCost(), definition.anvilCost(), definition.slots());

        ((TrinketSlotTarget) (Object) def).trinkets$slots(slots);
        return def;
    }

    static {
        TrinketsApi.registerTrinketPredicate(ResourceLocation.fromNamespaceAndPath("trinkets", "all"), (stack, ref, entity) -> TriState.TRUE);
        TrinketsApi.registerTrinketPredicate(ResourceLocation.fromNamespaceAndPath("trinkets", "none"), (stack, ref, entity) -> TriState.FALSE);
        TagKey<Item> trinketsAll = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("trinkets", "all"));

        TrinketsApi.registerTrinketPredicate(ResourceLocation.fromNamespaceAndPath("trinkets", "tag"), (stack, ref, entity) -> {
            SlotType slot = ref.inventory().getSlotType();
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("trinkets", slot.getId()));

            if (stack.is(tag) || stack.is(trinketsAll)) {
                return TriState.TRUE;
            }
            return TriState.DEFAULT;
        });
        TrinketsApi.registerTrinketPredicate(ResourceLocation.fromNamespaceAndPath("trinkets", "relevant"), (stack, ref, entity) -> {
            var reference = io.wispforest.accessories.api.slot.SlotReference.of(entity, ref.inventory().getSlotType().getName(), ref.index());
            var builder = new AccessoryAttributeBuilder(reference);

            var accessory = AccessoriesAPI.getOrDefaultAccessory(stack);

            if(accessory != null) accessory.getDynamicModifiers(stack, reference, builder);

            if (!builder.getAttributeModifiers(false).isEmpty()) return TriState.TRUE;
            return TriState.DEFAULT;
        });
        DEFAULT_TRINKET = new WrappedAccessory(AccessoriesAPI.defaultAccessory());
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        //System.out.println("WEEEEEEEEEEEEEEEE\nWEEEEEEEEEEEEEEEE\nWEEEEEEEEEEEEEEEE\nWEEEEEEEEEEEEEEEE\nWEEEEEEEEEEEEEEEE\n");
        registry.registerFor(LivingEntity.class, TrinketsApi.TRINKET_COMPONENT, livingEntity -> getTrinketComponent(livingEntity).get());
        registry.registerForPlayers(TrinketsApi.TRINKET_COMPONENT, player -> getTrinketComponent(player).get(), RespawnCopyStrategy.ALWAYS_COPY);
    }

    private final static class SafeSlotBasedPredicate implements EntityBasedPredicate {
        private static final Logger LOGGER = LogUtils.getLogger();
        private boolean hasErrored = false;

        private final ResourceLocation location;
        private final Function3<ItemStack, SlotReference, LivingEntity, TriState> trinketPredicate;

        public SafeSlotBasedPredicate(ResourceLocation location, Function3<ItemStack, SlotReference, LivingEntity, TriState> trinketPredicate) {
            this.location = location;
            this.trinketPredicate = trinketPredicate;
        }

        @Override
        public TriState isValid(Level level, @Nullable LivingEntity entity, io.wispforest.accessories.api.slot.SlotType slotType, int slot, ItemStack stack) {
            if(hasErrored) return TriState.DEFAULT;

            try {
                return this.trinketPredicate.apply(stack, new SlotReference(new CursedTrinketInventory(slotType, level.isClientSide()), slot), entity);
            } catch (Exception e) {
                this.hasErrored = true;
                LOGGER.warn("Unable to handle Trinket Slot Predicate converted to Accessories Slot Predicate due to fundamental incompatibility, issues may be present with it! [Slot: {}, Predicate ID: {}]", slotType.name(), this.location, e);
            }

            return TriState.DEFAULT;
        }
    }

    private static final class CursedTrinketInventory extends TrinketInventory {
        public CursedTrinketInventory(io.wispforest.accessories.api.slot.SlotType slotType, boolean isClientSide) {
            super(WrappedSlotType.of(slotType, isClientSide), null, inv -> {});
        }
    }
}
