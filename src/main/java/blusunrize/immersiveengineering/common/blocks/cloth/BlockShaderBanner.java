/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.cloth;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.shader.CapabilityShader;
import blusunrize.immersiveengineering.client.models.IOBJModelCallback;
import blusunrize.immersiveengineering.common.blocks.BlockIETileProvider;
import blusunrize.immersiveengineering.common.blocks.ItemBlockIEBase;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public class BlockShaderBanner extends BlockIETileProvider
{
	public BlockShaderBanner()
	{
		super("shader_banner", Block.Properties.create(Material.CLOTH), ItemBlockIEBase.class,
				IEProperties.FACING_ALL, IOBJModelCallback.PROPERTY, CapabilityShader.BLOCKSTATE_PROPERTY);
	}

	@Nullable
	@Override
	public TileEntity createBasicTE(IBlockState state)
	{
		return new TileEntityShaderBanner();
	}
}