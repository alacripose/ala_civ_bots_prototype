package dev.ensouled.entity;

import dev.ensouled.EnsouledMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class EnsouledEntities {
	public static final EntityType<AgentBodyEntity> AGENT = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(EnsouledMod.MOD_ID, "agent"),
			EntityType.Builder.create(AgentBodyEntity::new, SpawnGroup.CREATURE)
					.setDimensions(0.6f, 1.8f)
					.maxTrackingRange(96)
					.build("ensouled:agent"));

	private EnsouledEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(AGENT, PathAwareEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0));
		EnsouledMod.LOG.info("[ensouled] registered entity {}", Registries.ENTITY_TYPE.getId(AGENT));
	}
}
