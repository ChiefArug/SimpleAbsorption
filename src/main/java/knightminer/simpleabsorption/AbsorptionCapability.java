package knightminer.simpleabsorption;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Capability handling absorption NBT storage */
public class AbsorptionCapability {

	/** Capability ID */
	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SimpleAbsorption.MOD_ID, "absorption_handler");
	/** Capability type */
	public final static Capability<AbsorptionHandler> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
	/** Logic to run for the absorption handler */
	private static final NonNullConsumer<AbsorptionHandler> HANDLER_CONSUMER =  AbsorptionHandler::playerTick;

	/** Registers the event handlers and the capability */
	public static void init(IEventBus modBus) {
		modBus.addListener(AbsorptionCapability::registerCapability);
		NeoForge.EVENT_BUS.addGenericListener(Entity.class, AbsorptionCapability::attachCapability);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerTickEvent.class, AbsorptionCapability::playerTick);
	}

	/** Event listener to register the capability */
	private static void registerCapability(RegisterCapabilitiesEvent event) {
		event.register(AbsorptionHandler.class);
	}

	/** Event listener to attach the capability */
	private static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof Player) {
			event.addCapability(ID, new AbsorptionHandler((Player) event.getObject()));
		}
	}

	/** Runs on player update to update absorption shield, internal event */
	private static void playerTick(PlayerTickEvent event) {
		// use phase.start so we run before the food timer resets
		if (event.side != LogicalSide.SERVER || event.phase != Phase.START) {
			return;
		}
		event.player.getCapability(CAPABILITY).ifPresent(HANDLER_CONSUMER);
	}
}
