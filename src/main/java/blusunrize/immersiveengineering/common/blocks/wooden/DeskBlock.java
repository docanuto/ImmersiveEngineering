/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.wooden;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.generic.GenericTileBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;

public class DeskBlock<T extends TileEntity> extends GenericTileBlock<T>
{
	public static final Property<Direction> FACING = IEProperties.FACING_HORIZONTAL;
	public static final Property<Boolean> DUMMY = IEProperties.MULTIBLOCKSLAVE;

	public DeskBlock(String name, RegistryObject<TileEntityType<T>> tileType)
	{
		super(name, tileType, Block.Properties.create(Material.WOOD).sound(SoundType.WOOD).hardnessAndResistance(2, 5).notSolid());
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder)
	{
		super.fillStateContainer(builder);
		builder.add(DUMMY, FACING);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot)
	{
		Direction newFacing = rot.rotate(state.get(FACING));
		return state.with(FACING, newFacing);
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn)
	{
		if(mirrorIn==Mirror.NONE)
			return state;
		Direction oldFacing = state.get(FACING);
		Direction newFacing = mirrorIn.mirror(oldFacing);
		boolean oldDummy = state.get(DUMMY);
		boolean newDummy = !oldDummy;
		return state.with(FACING, newFacing).with(DUMMY, newDummy);
	}

	@Override
	public boolean canIEBlockBePlaced(BlockState newState, BlockItemUseContext context)
	{
		BlockPos start = context.getPos();
		World w = context.getWorld();
		return areAllReplaceable(start, start.up(2), context);
	}

	@Override
	public BlockState updatePostPlacement(BlockState stateIn, Direction updateSide, BlockState updatedState,
										  IWorld worldIn, BlockPos currentPos, BlockPos updatedPos)
	{
		Direction facing = stateIn.get(FACING);
		boolean dummy = stateIn.get(DUMMY);
		{
			// Check if current facing is correct
			BlockPos otherHalf = currentPos.offset(facing.rotateY(), dummy?-1: 1);
			BlockState otherState = worldIn.getBlockState(otherHalf);
			if(otherState.getBlock()==this&&otherState.get(FACING)==facing&&otherState.get(DUMMY)==!dummy)
				return stateIn;
		}
		// Find correct facing, or remove
		for(Direction candidate : FACING.getAllowedValues())
			if(candidate!=facing)
			{
				BlockPos otherHalf = currentPos.offset(candidate.rotateY(), dummy?-1: 1);
				BlockState otherState = worldIn.getBlockState(otherHalf);
				if(otherState.getBlock()==this&&otherState.get(FACING)==candidate&&otherState.get(DUMMY)==!dummy)
					return stateIn.with(FACING, candidate);
			}
		return Blocks.AIR.getDefaultState();
	}

	public static Direction getDeskDummyOffset(World world, BlockPos pos, Direction facing, BlockItemUseContext ctx)
	{
		Direction dummyDir;
		if(facing.getAxis()==Axis.X)
			dummyDir = ctx.getHitVec().z < .5?Direction.NORTH: Direction.SOUTH;
		else
			dummyDir = ctx.getHitVec().x < .5?Direction.WEST: Direction.EAST;
		BlockPos dummyPos = pos.offset(dummyDir);
		if(!world.getBlockState(dummyPos).isReplaceable(BlockItemUseContext.func_221536_a(ctx, dummyPos, dummyDir)))
			dummyDir = dummyDir.getOpposite();
		return dummyDir;
	}
}
