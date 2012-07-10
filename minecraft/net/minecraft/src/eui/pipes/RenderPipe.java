package net.minecraft.src.eui.pipes;

import org.lwjgl.opengl.GL11;
import net.minecraft.src.*;

public class RenderPipe extends TileEntitySpecialRenderer
{
	int type = 0;
	private ModelPipe model;
	
	public RenderPipe()
	{
		model = new ModelPipe();
	}

	public void renderAModelAt(TileEntityPipe tileEntity, double d, double d1, double d2, float f)
	{
        //Texture file
		
		type = tileEntity.getType();
		switch(type)
		{
		case 0: bindTextureByName("/eui/SteamPipe.png");break;
		case 1: bindTextureByName("/eui/WaterPipe.png");break;
		//case 2: bindTextureByName("/eui/lavaPipe.png");break;
		//case 3: bindTextureByName("/eui/oilPipe.png");break;
		//case 4: bindTextureByName("/eui/fuelPipe.png");break;
		//case 5: bindTextureByName("/eui/airPipe.png");break;
		default:bindTextureByName("/eui/DefaultPipe.png"); break;
		}
        
		GL11.glPushMatrix();
		GL11.glTranslatef((float) d + 0.5F, (float) d1 + 1.5F, (float) d2 + 0.5F);
		GL11.glScalef(1.0F, -1F, -1F);

		if(tileEntity.connectedBlocks[0] != null) model.renderBottom();
		if(tileEntity.connectedBlocks[1] != null) model.renderTop();
		if(tileEntity.connectedBlocks[2] != null) model.renderFront();
		if(tileEntity.connectedBlocks[3] != null) model.renderBack();
		if(tileEntity.connectedBlocks[4] != null) model.renderRight();
		if(tileEntity.connectedBlocks[5] != null) model.renderLeft();
		
		model.renderMiddle();
		GL11.glPopMatrix();
	
	}

	@Override
	public void renderTileEntityAt(TileEntity tileEntity, double var2, double var4, double var6, float var8) {
		this.renderAModelAt((TileEntityPipe)tileEntity, var2, var4, var6, var8);
	}

}