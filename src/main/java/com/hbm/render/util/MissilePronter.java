package com.hbm.render.util;

import org.lwjgl.opengl.GL11;

import com.hbm.handler.RocketStruct;
import com.hbm.handler.RocketStruct.RocketStage;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;

import net.minecraft.client.renderer.texture.TextureManager;

public class MissilePronter {

	public static void prontMissile(MissileMultipart missile, TextureManager tex) {
		
		GL11.glPushMatrix();
		
		if(missile.thruster != null && missile.thruster.type.name().equals(PartType.THRUSTER.name())) {
			
			tex.bindTexture(missile.thruster.texture);
			missile.thruster.model.renderAll();
			GL11.glTranslated(0, missile.thruster.height, 0);
		}
		
		if(missile.fuselage != null && missile.fuselage.type.name().equals(PartType.FUSELAGE.name())) {

			if(missile.fins != null && missile.fins.type.name().equals(PartType.FINS.name())) {
				
				tex.bindTexture(missile.fins.texture);
				missile.fins.model.renderAll();
			}
			
			tex.bindTexture(missile.fuselage.texture);
			missile.fuselage.model.renderAll();
			GL11.glTranslated(0, missile.fuselage.height, 0);
		}
		
		if(missile.warhead != null && missile.warhead.type.name().equals(PartType.WARHEAD.name())) {
			
			tex.bindTexture(missile.warhead.texture);
			missile.warhead.model.renderAll();
		}

		GL11.glPopMatrix();
	}

	// Attaches a set of stages together
	public static void prontRocket(RocketStruct rocket, TextureManager tex) {
		GL11.glPushMatrix();

		// first stage is always deployed, ready to be landed on
		boolean isDeployed = true;

		for(RocketStage stage : rocket.stages) {
			int stack = stage.getStack();
			int cluster = stage.getCluster();

			if(isDeployed && stage.thruster != null && stage.fins != null && stage.fins.height > stage.thruster.height) {
				GL11.glTranslated(0, stage.fins.height - stage.thruster.height, 0);
			}
			
			for(int c = 0; c < cluster; c++) {
				GL11.glPushMatrix();
				{

					if(c > 0) {
						float spin = (float)c / (float)(cluster - 1);
						GL11.glRotatef(360.0F * spin, 0, 1, 0);
						GL11.glTranslatef(2, 0, 0);
					}

					if(stage.thruster != null) {
						tex.bindTexture(stage.thruster.texture);
						stage.thruster.getModel(isDeployed).renderAll();
						GL11.glTranslated(0, stage.thruster.height, 0);
					}
		
					if(stage.fuselage != null) {
						if(stage.fins != null) {
							tex.bindTexture(stage.fins.texture);
							stage.fins.getModel(isDeployed).renderAll();
						}
					
						for(int s = 0; s < stack; s++) {
							tex.bindTexture(stage.fuselage.texture);
							stage.fuselage.getModel(isDeployed).renderAll();
							GL11.glTranslated(0, stage.fuselage.height, 0);
						}
					}

				}
				GL11.glPopMatrix();
			}

			
			if(stage.thruster != null) GL11.glTranslated(0, stage.thruster.height, 0);
			if(stage.fuselage != null) GL11.glTranslated(0, stage.fuselage.height * stack, 0);

			isDeployed = false;
		}

		if(rocket.capsule != null) {
			tex.bindTexture(rocket.capsule.texture);
			rocket.capsule.model.renderAll();
		}

		GL11.glPopMatrix();
	}
}
