package knightminer.simpleabsorption;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.UUID;
import java.util.function.Supplier;

import static knightminer.simpleabsorption.SimpleAbsorption.MOD_ID;
import static knightminer.simpleabsorption.SimpleAbsorption.MOD_RL;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.*;

/** Logic adding absorption from all relevant sources */
public class AbsorptionSources {

	/** UUID for potions */
	private static final UUID POTION_UUID = UUID.fromString("e7c88f6c-4d46-11eb-ae93-0242ac130002");

	/** Map of slot to UUID to ensure consistent removals */
	private static final ResourceLocation ARMOR_ADD_UUID = MOD_RL.withPath("armor_add");
	private static final ResourceLocation ARMOR_MULTIPLY_TOTAL_UUID = MOD_RL.withPath("armor_multiply_total");
	private static final ResourceLocation ARMOR_MULTIPLY_BASE_UUID = MOD_RL.withPath("armor_multiply_base");
	private static final ResourceLocation EFFICIENCY_ADD_UUID = MOD_RL.withPath("regen_add");
	private static final ResourceLocation EFFICIENCY_MULTIPLY_TOTAL_UUID = MOD_RL.withPath("efficiency_multiply_total");
	private static final ResourceLocation EFFICIENCY_MULTIPLY_BASE_UUID = MOD_RL.withPath("efficiency_multiply_base");

	/** Cached object for removing the potion attribute */
	private static final Supplier<Multimap<Attribute, AttributeModifier>> POTION_REMOVAL = Suppliers.memoize(() -> ImmutableMultimap.of(SimpleAbsorption.ABSORPTION_MAX.get(), new AttributeModifier(POTION_UUID, "simple_absorption_potion", 0,Operation.ADDITION)));


	/* Absorption sources */

	/**
	 * Replaces an attribute on the item
	 * @param event        Event
	 * @param original     Original attribute to replace
	 * @param replacement  New atttribute
	 * @param baseUUID     UUID for the multiply base modifier
	 * @param totalUUID    UUID for the multiply total modifier
	 * @return  Additive value for attribute
	 */
	private static float replaceAttribute(ItemAttributeModifierEvent event, Attribute original, Attribute replacement, String name) {
		float additiveBoost = 0;
		float multiplyBase = 0;
		float multiplyTotal = 1;
		for (AttributeModifier modifier : event.removeModifier(Holder.direct(original), )) {
			switch (modifier.operation()) {
				case ADD_VALUE -> additiveBoost += modifier.amount();
				case ADD_MULTIPLIED_BASE -> multiplyBase += modifier.amount();
				case ADD_MULTIPLIED_TOTAL ->
						// operation is (1 + x1) * (1 + x2) * ..., so add the 1 before multiplying for the total
						multiplyTotal *= (1 + modifier.amount());
			}
		}
		// add in armor unique modifiers
		if (multiplyBase != 0) {
			event.addModifier(replacement, new AttributeModifier(MOD_RL.withPath( "multiply_base"), multiplyBase, ADD_MULTIPLIED_BASE));
		}
		// add in armor unique modifiers
		if (multiplyTotal != 1) {
			event.addModifier(replacement, new AttributeModifier( name + "_multiply_total", multiplyTotal - 1, ADD_MULTIPLIED_TOTAL));
		}

		return additiveBoost;
	}

	/** Adds the attribute for relevant armors */
	@SubscribeEvent
	static void itemAttributeModifiers(ItemAttributeModifierEvent event) {
		// must be in the right slot
		float max = 0;
		float efficiency = 0;
		ItemStack stack = event.getItemStack();
		EquipmentSlot slot = event.slot();
		if (slot == Mob.getEquipmentSlotForItem(stack)) {
			// boost from enchant
			max += stack.getEnchantmentLevel(SimpleAbsorption.ABSORPTION.get());

			// boost from gold
			int goldBoost = Config.GOLD_ABSORPTION.get();
			double chainBoost = Config.CHAIN_EFFICIENCY.get();
			if (goldBoost > 0 || chainBoost > 0) {
				Item item = stack.getItem();
				if (item instanceof ArmorItem) {
					ArmorMaterial material = ((ArmorItem)item).getMaterial();
					if (material == ArmorMaterials.GOLD) {
						max += goldBoost;
					} else if (material == ArmorMaterials.CHAIN) {
						efficiency += chainBoost;
					}
				}
			}
		}

		// replace armor means attributes on all 6 slots are replaced with absorption
		if (Config.REPLACE_ARMOR.get()) {
			// armor -> absorption max
			max += replaceAttribute(event, Attributes.ARMOR, SimpleAbsorption.ABSORPTION_MAX.get(), "simple_absorption_max",
															ARMOR_MULTIPLY_BASE_UUID, ARMOR_MULTIPLY_TOTAL_UUID.get(slot));
			// toughness -> absorption efficiency
			efficiency += replaceAttribute(event, Attributes.ARMOR_TOUGHNESS, SimpleAbsorption.ABSORPTION_EFFICIENCY.get(), "simple_absorption_efficiency",
																		 EFFICIENCY_MULTIPLY_BASE_UUID.get(slot), EFFICIENCY_MULTIPLY_TOTAL_UUID.get(slot));
		}

		// add the attributes if we have any changes
		if (max != 0) event.addModifier(SimpleAbsorption.ABSORPTION_MAX.get(), new AttributeModifier(ARMOR_ADD_UUID.get(slot), "simple_absorption_armor", max, Operation.ADDITION));
		if (efficiency != 0) event.addModifier(SimpleAbsorption.ABSORPTION_EFFICIENCY.get(), new AttributeModifier(EFFICIENCY_ADD_UUID.get(slot), "simple_absorption_efficiency", efficiency, Operation.ADDITION));
	}

	/** Adds the attribute when absorption is added */
	@SubscribeEvent
	static void onAdd(MobEffectEvent.Added event) {
		// if we added absorption, add the modifier based on the level
		MobEffectInstance added = event.getEffectInstance();
		if (Config.INCLUDE_POTION.get() && added.getEffect() == MobEffects.ABSORPTION) {
			event.getEntity().getAttributes().addTransientAttributeModifiers(ImmutableMultimap.of(SimpleAbsorption.ABSORPTION_MAX.get(),
																																													new AttributeModifier(POTION_UUID, "simple_absorption_potion", (added.getAmplifier() + 1) * 4, Operation.ADDITION)));
		}
	}

	/** Removes the attribute when absorption is removed */
	@SubscribeEvent
	static void onRemove(MobEffectEvent.Remove event) {
		// if we removed absorption, remove the modifier
		// remove regardless of config in case it changed since the attribute was added
		if (event.getEffect() == MobEffects.ABSORPTION) {
			event.getEntity().getAttributes().removeAttributeModifiers(POTION_REMOVAL.get());
		}
	}

	/** Removes the attribute when absorption timer runs out */
	@SubscribeEvent
	static void onExpire(MobEffectEvent.Expired event) {
		// see above comment
		MobEffectInstance instance = event.getEffectInstance();
		if (instance != null && instance.getEffect() == MobEffects.ABSORPTION) {
			event.getEntity().getAttributes().removeAttributeModifiers(POTION_REMOVAL.get());
		}
	}
}
