package com.hbm.items.machine;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.hbm.config.MachineConfig;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemRTGPelletDepleted.DepletedRTGMaterial;
import com.hbm.tileentity.IRadioisotopeFuel;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class ItemRTGPellet extends Item {
	
	private short heat = 0;
	private boolean doesDecay = false;
	private ItemStack decayItem = null;
	private long lifespan = 0;
	
	public static final List<ItemRTGPellet> pelletList = new ArrayList();
	
	public ItemRTGPellet(int heatIn) {
		heat = (short) heatIn;
		setMaxStackSize(1);
		pelletList.add(this);
	}
	
	private static final String[] facts = new String[] {
			"One gram of Pu-238 costs $8,000.",
			"One gram of Pu-238 produces just under half a Watt of decay heat.",
			"The typical plutonium RTG contains close to eight kilograms of Pu-238.",
			"Pu-238's half life is 87.7 years.",
			"A plutonium RTG was used to power the New Horizons probe that visited Pluto in 2015.",
			"Po-210 can also be used for RTGs as it generates 140 W/g of decay heat due to its 138 day half life.",
			"Pa-231 is an isotope of protactinium that easily fissions, but it isn't quite fissile.",
			"Muons generated by matter-antimatter reactions can trigger nuclear fusion reactions at room temperature.",
			"Roughly 20% of U-235 nuclei will fail to fission when hit by a neutron. They become U-236 nuclei instead.",
			"Thorium reactors are really uranium reactors that convert thorium into U-233.",
			"Natural uranium consists of 99.284% U-238, 0.711% U-235, and 0.0055% U-234.",
			"Most nuclear reactors use uranium that has been enriched to 3-5% U-235.",
			"Uranium-based nuclear weapons require uranium enriched to at least 85-90% U-235.",
			"Depleted uranium is uranium that has had most of its U-235 removed. It is effectively pure U-238.",
			"In the 1920s, uranium was considered a useless byproduct of the production of radium.",
			"The Manhattan Project referred to refined natural uranium as tuballoy, enriched uranium as oralloy, and depleted uranium as depletalloy."
	};
	
	public ItemRTGPellet setDecays(DepletedRTGMaterial mat, long life) {
		doesDecay = true;
		decayItem = new ItemStack(ModItems.pellet_rtg_depleted, 1, mat.ordinal());
		lifespan = life;
		return this;
	}
	
	public long getMaxLifespan() {
		return lifespan;
	}

	public short getHeat() {
		return heat;
	}

	@CheckForNull
	public ItemStack getDecayItem() {
		return decayItem == null ? null : decayItem.copy();
	}

	public boolean getDoesDecay() {
		return this.doesDecay;
	}
	
	public static ItemStack handleDecay(ItemStack stack, ItemRTGPellet instance) {
		if (instance.getDoesDecay() && MachineConfig.doRTGsDecay) {
			if (instance.getLifespan(stack) <= 0)
				return instance.getDecayItem();
			else
				instance.decay(stack);
		}
		
		return stack;
	}
	
	public void decay(ItemStack stack) {
		if (stack != null && stack.getItem() instanceof ItemRTGPellet) {
			if (!((ItemRTGPellet) stack.getItem()).getDoesDecay())
				return;
			if (stack.hasTagCompound())
				stack.stackTagCompound.setLong("PELLET_DEPLETION", getLifespan(stack) - 1);
			else {
				stack.stackTagCompound = new NBTTagCompound();
				stack.stackTagCompound.setLong("PELLET_DEPLETION", getMaxLifespan());
			}
		}
	}
	
	public long getLifespan(ItemStack stack)
	{
		if (stack != null && stack.getItem() instanceof ItemRTGPellet)
		{
			if (stack.hasTagCompound())
				return stack.stackTagCompound.getLong("PELLET_DEPLETION");
			else
			{
				stack.stackTagCompound = new NBTTagCompound();
				stack.stackTagCompound.setLong("PELLET_DEPLETION", getMaxLifespan());
				return getMaxLifespan();
			}
		}
		return 0;
	}
	
	public static short getScaledPower(ItemRTGPellet fuel, ItemStack stack) {
		return (short) Math.ceil(fuel.getHeat() * ((double)fuel.getLifespan(stack) / (double)fuel.getMaxLifespan()));
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		
		if(!world.isRemote && this == ModItems.pellet_rtg) {
			player.addChatComponentMessage(new ChatComponentText(facts[world.rand.nextInt(facts.length)]).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)));
			world.playSoundAtEntity(player, "random.orb", 1.0F, 1.0F);
		}
		
		return stack;
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return getDoesDecay() && getLifespan(stack) != getMaxLifespan();
	}
	
	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		final ItemRTGPellet instance = (ItemRTGPellet) stack.getItem();
		return 1D - (double)instance.getLifespan(stack) / (double)instance.getMaxLifespan();
	}
	
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		super.addInformation(stack, player, list, bool);
		list.add(I18nUtil.resolveKey(this.getUnlocalizedName().concat(".desc")));
		final ItemRTGPellet instance = (ItemRTGPellet) stack.getItem();
		list.add(I18nUtil.resolveKey("desc.item.rtgHeat", instance.getDoesDecay() && MachineConfig.scaleRTGPower ? getScaledPower(instance, stack) : instance.getHeat()));
		if (instance.getDoesDecay()) {
			list.add(I18nUtil.resolveKey("desc.item.rtgDecay", I18nUtil.resolveKey(instance.getDecayItem().getUnlocalizedName() + ".name"), instance.getDecayItem().stackSize));
			list.add(BobMathUtil.toPercentage(instance.getLifespan(stack), instance.getMaxLifespan()));
			if (bool) {
				list.add("EXTENDED INFO:");
				list.add(String.format("%s / %s ticks", instance.getLifespan(stack), instance.getMaxLifespan()));
				final String[] timeLeft = BobMathUtil.ticksToDate(instance.getLifespan(stack));
				final String[] maxLife = BobMathUtil.ticksToDate(instance.getMaxLifespan());
				list.add(String.format("Time remaining: %s y, %s d, %s h", (Object[]) timeLeft));
				list.add(String.format("Maximum life: %s y, %s d, %s h", (Object[]) maxLife));
			}
		}
	}

	public String getData() {
		return String.format("%s (%s HE/t) %s", I18nUtil.resolveKey(getUnlocalizedName().concat(".name")), getHeat(), (getDoesDecay() ? " (decays)" : ""));
	}
}
