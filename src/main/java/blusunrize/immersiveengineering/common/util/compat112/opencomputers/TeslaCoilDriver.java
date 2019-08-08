package blusunrize.immersiveengineering.common.util.compat112.opencomputers;

import blusunrize.immersiveengineering.common.Config.IEConfig;
import blusunrize.immersiveengineering.common.blocks.metal.TeslaCoilTileEntity;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TeslaCoilDriver extends DriverSidedTileEntity
{

	@Override
	public ManagedEnvironment createEnvironment(World w, BlockPos bp, Direction facing)
	{
		TileEntity te = w.getTileEntity(bp);
		if(te instanceof TeslaCoilTileEntity&&!((TeslaCoilTileEntity)te).isDummy())
		{
			return new EnergyMeterEnvironment(w, bp);
		}
		return null;
	}

	@Override
	public Class<?> getTileEntityClass()
	{
		return TeslaCoilTileEntity.class;
	}


	public class EnergyMeterEnvironment extends ManagedEnvironmentIE<TeslaCoilTileEntity>
	{
		public EnergyMeterEnvironment(World w, BlockPos bp)
		{
			super(w, bp, TeslaCoilTileEntity.class);
		}


		@Override
		public String preferredName()
		{
			return "ie_tesla_coil";
		}

		@Override
		public int priority()
		{
			return 1000;
		}

		@Override
		public void onConnect(Node node)
		{
		}

		@Override
		public void onDisconnect(Node node)
		{
		}

		@Callback(doc = "function():boolean -- checks whether the coil is active")
		public Object[] isActive(Context context, Arguments args)
		{
			TeslaCoilTileEntity te = getTileEntity();
			int energyDrain = IEConfig.Machines.teslacoil_consumption;
			if(te.lowPower)
				energyDrain /= 2;
			return new Object[]{te.canRun(energyDrain)};
		}

		@Callback(doc = "function():boolean -- checks whether the coil is active")
		public Object[] setRSMode(Context context, Arguments args)
		{
			getTileEntity().redstoneControlInverted = args.checkBoolean(0);
			return null;
		}

		@Callback(doc = "function():boolean -- checks whether the coil is active")
		public Object[] setPowerMode(Context context, Arguments args)
		{
			TeslaCoilTileEntity te = getTileEntity();
			int energyDrain = IEConfig.Machines.teslacoil_consumption;
			if(te.lowPower)
				energyDrain /= 2;
			if(te.canRun(energyDrain))
				throw new IllegalArgumentException("Can't switch power mode on an active coil");
			te.lowPower = !args.checkBoolean(0);
			return null;
		}

	}
}