package knightminer.simpleabsorption;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Rarity;
import net.minecraftforge.registries.ForgeRegistries;
import net.neoforged.neoforge.common.MinecraftForge;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.eventbus.api.IEventBus;
import net.neoforged.neoforge.fml.common.Mod;
import net.neoforged.neoforge.fml.config.ModConfig;
import net.neoforged.neoforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("WeakerAccess")
@Mod(SimpleAbsorption.MOD_ID)
public class SimpleAbsorption {
	// IDs
	protected static final String MOD_ID = "simple_absorption";
	protected static final ResourceLocation MOD_RL = ResourceLocation.fromNamespaceAndPath(MOD_ID, MOD_ID);
	private static final String ENCHANT_ID = "absorption";
	private static final String ATTRIBUTE_MAX_ID = "absorption_max";
	private static final String ATTRIBUTE_EFFICIENCY_ID = "absorption_efficiency";

	private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(BuiltInRegistries.ENCHANTMENT, MOD_ID);
	private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(BuiltInRegistries.ATTRIBUTES, MOD_ID);

	public static final DeferredHolder<Enchantment, AbsorptionEnchantment> ABSORPTION = ENCHANTMENTS.register(ENCHANT_ID, () -> new AbsorptionEnchantment(Rarity.RARE, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET));
	public static final DeferredHolder<Attribute, RangedAttribute> ABSORPTION_MAX = ATTRIBUTES.register(ATTRIBUTE_MAX_ID, () -> {
		RangedAttribute attr = new RangedAttribute(MOD_ID + "." + ATTRIBUTE_MAX_ID, 0, 0, 100);
		attr.setSyncable(true);
		return attr;});
	public static final DeferredHolder<Attribute, RangedAttribute> ABSORPTION_EFFICIENCY = ATTRIBUTES.register(ATTRIBUTE_EFFICIENCY_ID, () -> {
		RangedAttribute atr = new RangedAttribute(MOD_ID + "." + ATTRIBUTE_EFFICIENCY_ID, 0, 0, 20);
		atr.setSyncable(true);
		return atr;});

	public SimpleAbsorption(FMLJavaModLoadingContext ctx) {
		IEventBus modBus = ctx.getModEventBus();
		ctx.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
		modBus.addListener(SimpleAbsorption::setupAttributes);
		AbsorptionCapability.init(modBus);
		MinecraftForge.EVENT_BUS.register(AbsorptionSources.class);

		ENCHANTMENTS.register(modBus);
		ATTRIBUTES.register(modBus);
	}

	/** Adds attributes to the player */
	private static void setupAttributes(EntityAttributeModificationEvent event) {
		if (event.getTypes().contains(EntityType.PLAYER)) {
			event.add(EntityType.PLAYER, ABSORPTION_MAX);
			event.add(EntityType.PLAYER, ABSORPTION_EFFICIENCY);
		}
	}
}
