/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.manual;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.client.utils.IERenderTypes;
import blusunrize.immersiveengineering.common.util.fakeworld.TemplateWorld;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.ManualUtils;
import blusunrize.lib.manual.SpecialManualElements;
import blusunrize.lib.manual.gui.GuiButtonManualNavigation;
import blusunrize.lib.manual.gui.ManualScreen;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.*;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import java.util.*;
import java.util.function.Predicate;

import static blusunrize.immersiveengineering.api.client.TextUtils.applyFormat;

public class ManualElementMultiblock extends SpecialManualElements
{
	private final IMultiblock multiblock;

	private boolean canTick = true;
	private boolean showCompleted = false;

	private float scale = 50f;
	private float transX = 0;
	private float transY = 0;
	private TransformationMatrix additionalTransform;
	private List<ITextComponent> componentTooltip;
	private final MultiblockRenderInfo renderInfo;
	private final TemplateWorld structureWorld;
	private final int yOffTotal;

	private long lastStep = -1;
	private long lastPrintedErrorTimeMs = -1;

	public ManualElementMultiblock(ManualInstance manual, IMultiblock multiblock)
	{
		super(manual);
		this.multiblock = multiblock;
		List<BlockInfo> structure = multiblock.getStructure(Minecraft.getInstance().world);
		renderInfo = new MultiblockRenderInfo(structure);
		float diagLength = (float)Math.sqrt(renderInfo.structureHeight*renderInfo.structureHeight+
				renderInfo.structureWidth*renderInfo.structureWidth+
				renderInfo.structureLength*renderInfo.structureLength);
		structureWorld = new TemplateWorld(structure, renderInfo);
		transX = 60+renderInfo.structureWidth/2F;
		transY = 35+diagLength/2;
		additionalTransform = new TransformationMatrix(
				null,
				new Quaternion(25, 0, 0, true),
				null,
				new Quaternion(0, -45, 0, true)
		);
		scale = multiblock.getManualScale();
		yOffTotal = (int)(transY+scale*diagLength/2);
	}

	private static final ITextComponent greenTick = applyFormat(
			new StringTextComponent("\u2713"), TextFormatting.GREEN, TextFormatting.BOLD
	).appendString(" ");

	@Override
	public void onOpened(ManualScreen gui, int x, int y, List<Button> pageButtons)
	{
		int yOff = 0;
		if(multiblock.getStructure(null)!=null)
		{
			boolean canRenderFormed = multiblock.canRenderFormedStructure();

			yOff = (int)(transY+scale*Math.sqrt(renderInfo.structureHeight*renderInfo.structureHeight+renderInfo.structureWidth*renderInfo.structureWidth+renderInfo.structureLength*renderInfo.structureLength)/2);
			pageButtons.add(new GuiButtonManualNavigation(gui, x+4, (int)transY-(canRenderFormed?11: 5), 10, 10, 4, btn -> {
				GuiButtonManualNavigation btnNav = (GuiButtonManualNavigation)btn;
				canTick = !canTick;
				lastStep = -1;
				btnNav.type = btnNav.type==4?5: 4;
			}));
			if(this.renderInfo.structureHeight > 1)
			{
				pageButtons.add(new GuiButtonManualNavigation(gui, x+4, (int)transY-(canRenderFormed?14: 8)-16, 10, 16, 3,
						btn -> renderInfo.setShowLayer(Math.min(renderInfo.showLayer+1, renderInfo.structureHeight-1))
				));
				pageButtons.add(new GuiButtonManualNavigation(gui, x+4, (int)transY+(canRenderFormed?14: 8), 10, 16, 2,
						btn -> renderInfo.setShowLayer(Math.max(renderInfo.showLayer-1, -1))));
			}
			if(canRenderFormed)
				pageButtons.add(new GuiButtonManualNavigation(gui, x+4, (int)transY+1, 10, 10, 6,
						btn -> showCompleted = !showCompleted));
		}

		checkMaterials();
		super.onOpened(gui, x, yOff, pageButtons);
	}

	private void checkMaterials()
	{
		ItemStack[] totalMaterials = this.multiblock.getTotalMaterials();
		if(totalMaterials!=null)
		{
			componentTooltip = new ArrayList<>();
			componentTooltip.add(new TranslationTextComponent("desc.immersiveengineering.info.reqMaterial"));
			int maxOff = 1;
			boolean hasAnyItems = false;
			boolean[] hasItems = new boolean[totalMaterials.length];
			for(int ss = 0; ss < totalMaterials.length; ss++)
				if(totalMaterials[ss]!=null)
				{
					ItemStack req = totalMaterials[ss];
					int reqSize = req.getCount();
					for(int slot = 0; slot < ManualUtils.mc().player.inventory.getSizeInventory(); slot++)
					{
						ItemStack inSlot = ManualUtils.mc().player.inventory.getStackInSlot(slot);
						if(!inSlot.isEmpty()&&ItemStack.areItemsEqual(inSlot, req))
							if((reqSize -= inSlot.getCount()) <= 0)
								break;
					}
					if(reqSize <= 0)
					{
						hasItems[ss] = true;
						if(!hasAnyItems)
							hasAnyItems = true;
					}
					maxOff = Math.max(maxOff, (""+req.getCount()).length());
				}
			for(int ss = 0; ss < totalMaterials.length; ss++)
				if(totalMaterials[ss]!=null)
				{
					ItemStack req = totalMaterials[ss];
					int indent = maxOff-(""+req.getCount()).length();
					StringBuilder sIndent = new StringBuilder();
					if(indent > 0)
						for(int ii = 0; ii < indent; ii++)
							sIndent.append("0");
					IFormattableTextComponent s;
					if(hasItems[ss])
						s = greenTick.deepCopy();
					else
						s = new StringTextComponent(hasAnyItems?"   ": "");
					s.appendSibling(applyFormat(
							new StringTextComponent(sIndent.toString()+req.getCount()+"x "), TextFormatting.GRAY
					));
					if(!req.isEmpty())
						s.appendSibling(applyFormat(req.getDisplayName().deepCopy(), req.getRarity().color));
					else
						s.appendString("???");
					componentTooltip.add(s);
				}
		}
	}

	@Override
	public void render(MatrixStack transform, ManualScreen gui, int x, int y, int mouseX, int mouseY)
	{
		if(multiblock.getStructure(null)!=null)
		{
			IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
			MatrixStack.Entry lastEntryBeforeTry = transform.getLast();
			try
			{
				long currentTime = System.currentTimeMillis();
				if(lastStep < 0)
					lastStep = currentTime;
				else if(canTick&&currentTime-lastStep > 500)
				{
					renderInfo.step();
					lastStep = currentTime;
				}

				int structureLength = renderInfo.structureLength;
				int structureWidth = renderInfo.structureWidth;
				int structureHeight = renderInfo.structureHeight;

				transform.push();

				final BlockRendererDispatcher blockRender = Minecraft.getInstance().getBlockRendererDispatcher();

				transform.translate(transX, transY, Math.max(structureHeight, Math.max(structureWidth, structureLength)));
				transform.scale(scale, -scale, 1);
				additionalTransform.push(transform);
				transform.rotate(new Quaternion(0, 90, 0, true));

				transform.translate(structureLength/-2f, structureHeight/-2f, structureWidth/-2f);

				int idx = 0;
				if(showCompleted&&multiblock.canRenderFormedStructure())
				{
					transform.push();
					multiblock.renderFormedStructure(transform, IERenderTypes.disableLighting(buffer));
					transform.pop();
				}
				else
					for(int h = 0; h < structureHeight; h++)
						for(int l = 0; l < structureLength; l++)
							for(int w = 0; w < structureWidth; w++)
							{
								BlockPos pos = new BlockPos(l, h, w);
								BlockState state = structureWorld.getBlockState(pos);
								if(!state.isAir(structureWorld, pos))
								{
									transform.push();
									transform.translate(l, h, w);
									boolean b = multiblock.overwriteBlockRender(state, idx++);
									if(!b)
									{
										int overlay;
										if(pos.equals(multiblock.getTriggerOffset()))
											overlay = OverlayTexture.getPackedUV(0, true);
										else
											overlay = OverlayTexture.NO_OVERLAY;
										IModelData modelData = EmptyModelData.INSTANCE;
										TileEntity te = structureWorld.getTileEntity(pos);
										if(te!=null)
											modelData = te.getModelData();

										blockRender.renderBlock(state, transform,
												IERenderTypes.disableLighting(buffer),
												0xf000f0, overlay, modelData);
									}
									transform.pop();
								}
							}
				transform.pop();
				transform.pop();
			} catch(Exception e)
			{
				final long now = System.currentTimeMillis();
				if(now > lastPrintedErrorTimeMs+1000)
				{
					e.printStackTrace();
					lastPrintedErrorTimeMs = now;
				}
				while(lastEntryBeforeTry!=transform.getLast())
					transform.pop();
			}
			buffer.finish();

			if(componentTooltip!=null)
			{
				manual.fontRenderer().drawString(transform, "?", 116, yOffTotal/2-4, manual.getTextColour());
				if(mouseX >= 116&&mouseX < 122&&mouseY >= yOffTotal/2-4&&mouseY < yOffTotal/2+4)
					gui.renderToolTip(transform, LanguageMap.getInstance().func_244260_a(
							Collections.unmodifiableList(componentTooltip)
					), mouseX, mouseY, manual.fontRenderer());
			}
		}
	}

	@Override
	public void mouseDragged(int x, int y, double clickX, double clickY, double mouseX, double mouseY, double lastX, double lastY, int mouseButton)
	{
		if((clickX >= 40&&clickX < 144&&mouseX >= 20&&mouseX < 164)&&(clickY >= 30&&clickY < 130&&mouseY >= 30&&mouseY < 180))
		{
			double dx = mouseX-lastX;
			double dy = mouseY-lastY;
			additionalTransform = forRotation(dx*80D/104, dy*0.8).compose(additionalTransform);
		}
	}

	private TransformationMatrix forRotation(double rX, double rY)
	{
		Vector3f axis = new Vector3f((float)rY, (float)rX, 0);
		float angle = (float)Math.sqrt(axis.dot(axis));
		if(!axis.normalize())
			return TransformationMatrix.identity();
		return new TransformationMatrix(
				null,
				new Quaternion(axis, angle, true),
				null,
				null
		);
	}

	@Override
	public boolean listForSearch(String searchTag)
	{
		return false;
	}

	@Override
	public int getPixelsTaken()
	{
		return yOffTotal;
	}

	public IMultiblock getMultiblock()
	{
		return this.multiblock;
	}

	//Stolen back from boni's StructureInfo
	static class MultiblockRenderInfo implements Predicate<BlockPos>
	{
		public Map<BlockPos, BlockInfo> data = new HashMap<>();
		private final int structureHeight;
		private final int structureLength;
		private final int structureWidth;
		private final int maxBlockIndex;

		private int showLayer = -1;
		private int blockIndex;

		MultiblockRenderInfo(List<BlockInfo> structure)
		{
			int structureHeight = 0;
			int structureWidth = 0;
			int structureLength = 0;
			for(BlockInfo block : structure)
			{
				structureHeight = Math.max(structureHeight, block.pos.getY()+1);
				structureWidth = Math.max(structureWidth, block.pos.getZ()+1);
				structureLength = Math.max(structureLength, block.pos.getX()+1);
				data.put(block.pos, block);
			}
			this.maxBlockIndex = this.blockIndex = structureHeight*structureLength*structureWidth;
			this.structureHeight = structureHeight;
			this.structureLength = structureLength;
			this.structureWidth = structureWidth;
		}

		void setShowLayer(int layer)
		{
			showLayer = layer;
			if(layer < 0)
				reset();
			else
				blockIndex = (layer+1)*(structureLength*structureWidth)-1;
		}

		public void reset()
		{
			blockIndex = maxBlockIndex;
		}

		void step()
		{
			final int start = blockIndex;
			do
			{
				if(++blockIndex >= maxBlockIndex)
					blockIndex = 0;
			}
			while(isEmpty(blockIndex)&&blockIndex!=start);
		}

		private boolean isEmpty(int index)
		{
			int y = index/(structureLength*structureWidth);
			int r = index%(structureLength*structureWidth);
			int x = r/structureWidth;
			int z = r%structureWidth;

			return !data.containsKey(new BlockPos(x, y, z));
		}

		int getLimiter()
		{
			return blockIndex;
		}

		@Override
		public boolean test(BlockPos blockPos)
		{
			int index = blockPos.getZ()+structureWidth*(blockPos.getX()+structureLength*blockPos.getY());
			return index <= getLimiter();
		}
	}
}
