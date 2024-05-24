package com.hbm.dim;

import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CelestialBodyTrait.CBT_SUNEXPLODED;
import com.hbm.inventory.fluid.Fluids;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.IRenderHandler;

public abstract class WorldProviderCelestial extends WorldProvider {
	
	@Override
	public abstract void registerWorldChunkManager();

	@Override
	public void updateWeather() {
		if(CelestialBody.hasTrait(worldObj, CBT_Atmosphere.class)) {
			super.updateWeather();
			return;
		}

		this.worldObj.getWorldInfo().setRainTime(0);
		this.worldObj.getWorldInfo().setRaining(false);
		this.worldObj.getWorldInfo().setThunderTime(0);
		this.worldObj.getWorldInfo().setThundering(false);
		this.worldObj.rainingStrength = 0.0F;
		this.worldObj.thunderingStrength = 0.0F;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Vec3 getFogColor(float x, float y) {
		if(CelestialBody.hasTrait(worldObj, CBT_SUNEXPLODED.class)) return Vec3.createVectorHelper(0, 0, 0);

		CBT_Atmosphere atmosphere = CelestialBody.getTrait(worldObj, CBT_Atmosphere.class);

		// The cold hard vacuum of space
		if(atmosphere == null) return Vec3.createVectorHelper(0, 0, 0);
		
		float sun = this.getSunBrightnessFactor(1.0F);
		float totalPressure = (float)atmosphere.getPressure();
		Vec3 color = Vec3.createVectorHelper(0, 0, 0);

		for(CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
			Vec3 fluidColor;

			if(entry.fluid == Fluids.EVEAIR) {
				fluidColor = Vec3.createVectorHelper(53F / 255F * sun, 32F / 255F * sun, 74F / 255F * sun);
			} else if(entry.fluid == Fluids.CARBONDIOXIDE) {
				fluidColor = Vec3.createVectorHelper(212F / 255F * sun, 112F / 255F * sun, 78F / 255F * sun);
			} else if(entry.fluid == Fluids.AIR || entry.fluid == Fluids.OXYGEN || entry.fluid == Fluids.NITROGEN) {
				// Default to regular ol' overworld
				fluidColor = super.getFogColor(x, y);
			} else {
				fluidColor = getColorFromHex(entry.fluid.getColor());
				fluidColor.xCoord *= sun * 1.4F;
				fluidColor.yCoord *= sun * 1.4F;
				fluidColor.zCoord *= sun * 1.4F;
			}

			float percentage = (float)entry.pressure / totalPressure;
			color = Vec3.createVectorHelper(
				color.xCoord + fluidColor.xCoord * percentage,
				color.yCoord + fluidColor.yCoord * percentage,
				color.zCoord + fluidColor.zCoord * percentage
			);
		}


		// Fog intensity remains high to simulate a thin looking atmosphere on low pressure planets
		float pressureFactor = MathHelper.clamp_float(totalPressure * 10.0F, 0.0F, 1.0F);
		color.xCoord *= pressureFactor;
		color.yCoord *= pressureFactor;
		color.zCoord *= pressureFactor;

		return color;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Vec3 getSkyColor(Entity camera, float partialTicks) {
		if(CelestialBody.hasTrait(worldObj, CBT_SUNEXPLODED.class)) return Vec3.createVectorHelper(0, 0, 0);

		CBT_Atmosphere atmosphere = CelestialBody.getTrait(worldObj, CBT_Atmosphere.class);
		
		// The cold hard vacuum of space
		if(atmosphere == null) return Vec3.createVectorHelper(0, 0, 0);

		float sun = this.getSunBrightnessFactor(1.0F);
		float totalPressure = (float)atmosphere.getPressure();
		Vec3 color = Vec3.createVectorHelper(0, 0, 0);

		for(CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
			Vec3 fluidColor;

			if(entry.fluid == Fluids.EVEAIR) {
				fluidColor = Vec3.createVectorHelper(53F / 255F * sun, 32F / 255F * sun, 74F / 255F * sun);
			} else if(entry.fluid == Fluids.CARBONDIOXIDE) {
				fluidColor = Vec3.createVectorHelper(212F / 255F * sun, 112F / 255F * sun, 78F / 255F * sun);
			} else if(entry.fluid == Fluids.AIR || entry.fluid == Fluids.OXYGEN || entry.fluid == Fluids.NITROGEN) {
				// Default to regular ol' overworld
				fluidColor = super.getSkyColor(camera, partialTicks);
			} else {
				fluidColor = getColorFromHex(entry.fluid.getColor());
				fluidColor.xCoord *= sun;
				fluidColor.yCoord *= sun;
				fluidColor.zCoord *= sun;
			}

			float percentage = (float)entry.pressure / totalPressure;
			color = Vec3.createVectorHelper(
				color.xCoord + fluidColor.xCoord * percentage,
				color.yCoord + fluidColor.yCoord * percentage,
				color.zCoord + fluidColor.zCoord * percentage
			);
		}

		// Lower pressure sky renders thinner
		float pressureFactor = MathHelper.clamp_float(totalPressure, 0.0F, 1.0F);
		color.xCoord *= pressureFactor;
		color.yCoord *= pressureFactor;
		color.zCoord *= pressureFactor;

		return color;
	}

	private Vec3 getColorFromHex(int hexColor) {
		float red = ((hexColor >> 16) & 0xFF) / 255.0F;
		float green = ((hexColor >> 8) & 0xFF) / 255.0F;
		float blue = (hexColor & 0xFF) / 255.0F;
		return Vec3.createVectorHelper(red, green, blue);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float[] calcSunriseSunsetColors(float celestialAngle, float partialTicks) {
		CBT_Atmosphere atmosphere = CelestialBody.getTrait(worldObj, CBT_Atmosphere.class);
		if(CelestialBody.hasTrait(worldObj, CBT_SUNEXPLODED.class)) return null;
		if(atmosphere == null || atmosphere.getPressure() < 0.05F) return null;

		float[] colors = super.calcSunriseSunsetColors(celestialAngle, partialTicks);
		if(colors == null) return null;

		// Mars IRL has inverted blue sunsets, which look cool as
		// So carbon dioxide rich atmospheres will do the same
		// for now, it's just a swizzle between red and blue
		if(atmosphere.hasFluid(Fluids.CARBONDIOXIDE)) {
			float tmp = colors[0];
			colors[0] = colors[2];
			colors[2] = tmp;
		} else if (atmosphere.hasFluid(Fluids.EVEAIR)) {
			float f2 = 0.4F;
			float f3 = MathHelper.cos((celestialAngle) * (float)Math.PI * 2.0F) - 0.0F;
			float f4 = -0.0F;
	
			if (f3 >= f4 - f2 && f3 <= f4 + f2) {
				float f5 = (f3 - f4) / f2 * 0.5F + 0.5F;
				float f6 = 1.0F - (1.0F - MathHelper.sin(f5 * (float)Math.PI)) * 0.99F;
				f6 *= f6;
				colors[0] = f5 * 0.01F;
				colors[1] = f5 * f5 * 0.9F + 0.3F;
				colors[2] = f5 * f5;
				colors[3] = f6;
			}
		}

		return colors;
	}

	@Override
	public boolean canDoLightning(Chunk chunk) {
		CBT_Atmosphere atmosphere = CelestialBody.getTrait(worldObj, CBT_Atmosphere.class);

		if(atmosphere != null && atmosphere.getPressure() > 0.2)
			return super.canDoLightning(chunk);

		return false;
	}

	@Override
	public boolean canDoRainSnowIce(Chunk chunk) {
		CBT_Atmosphere atmosphere = CelestialBody.getTrait(worldObj, CBT_Atmosphere.class);

		if(atmosphere != null && atmosphere.getPressure() > 0.2)
			return super.canDoRainSnowIce(chunk);

		return false;
	}

	// Stars do not show up during the day in a vacuum, common misconception:
	// The reason stars aren't visible during the day on Earth isn't because of the sky,
	// the sky is ALWAYS there. The reason they aren't visible is because the Sun is too bright!
	@Override
	@SideOnly(Side.CLIENT)
	public float getStarBrightness(float par1) {
		// Stars become visible during the day beyond the orbit of Duna
		// And are fully visible during the day beyond the orbit of Jool
		float distanceStart = 20_000_000;
		float distanceEnd = 80_000_000;

		float semiMajorAxisKm = CelestialBody.getSemiMajorAxis(worldObj);
		float distanceFactor = MathHelper.clamp_float((semiMajorAxisKm - distanceStart) / (distanceEnd - distanceStart), 0F, 1F);

		// Vacuum still increases star brightness tho
		float starBrightness = super.getStarBrightness(par1);
		if (!CelestialBody.hasTrait(worldObj, CBT_Atmosphere.class))
			starBrightness *= 2F;

		return MathHelper.clamp_float(starBrightness, distanceFactor, 1F);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float getSunBrightness(float par1) {
		if(CelestialBody.hasTrait(worldObj, CBT_SUNEXPLODED.class)) {
			return 0;
		}

		CBT_Atmosphere atmosphere = CelestialBody.getTrait(worldObj, CBT_Atmosphere.class);
		float sunBrightness = super.getSunBrightness(par1);

		if(atmosphere == null) return sunBrightness;

		if(atmosphere.getPressure() < 2F) {
			return sunBrightness *= 0.3F;
		}

		return sunBrightness;
	}

	@Override
	public boolean canRespawnHere() {
		return false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getCloudHeight() {
		CBT_Atmosphere atmosphere = CelestialBody.getTrait(worldObj, CBT_Atmosphere.class);

		if(atmosphere == null || atmosphere.getPressure() < 0.5F) return -100;
		
		return super.getCloudHeight();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IRenderHandler getSkyRenderer() {
		return new SkyProviderCelestial();
	}

	protected double getDayLength() {
		CelestialBody body = CelestialBody.getBody(worldObj);
		return body.getRotationalPeriod() / (1 - (1 / body.getPlanet().getOrbitalPeriod()));
	}
	
	@Override
	public float calculateCelestialAngle(long worldTime, float timeOffset) {
		double j = ((worldTime - Math.abs(worldObj.getSeed())) % this.getDayLength());
		double f1 = (j + timeOffset) / this.getDayLength() - 0.25F;

		if(f1 < 0.0F) {
			++f1;
		}

		if(f1 > 1.0F) {
			--f1;
		}

		double f2 = f1;
		f1 = 0.5F - Math.cos(f1 * Math.PI) / 2.0F;
		return (float)(f2 + (f1 - f2) / 3.0D);
	}

}