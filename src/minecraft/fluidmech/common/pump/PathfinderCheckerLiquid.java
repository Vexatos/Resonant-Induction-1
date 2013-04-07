package fluidmech.common.pump;

import fluidmech.common.FluidMech;
import hydraulic.helpers.FluidHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.LiquidStack;
import universalelectricity.core.path.IPathCallBack;
import universalelectricity.core.path.Pathfinder;
import universalelectricity.core.vector.Vector3;

public class PathfinderCheckerLiquid extends Pathfinder
{
	public List<Vector3> targetList = new ArrayList<Vector3>();

	public PathfinderCheckerLiquid(final World world, final Vector3 callLoc, final LiquidStack resource, final Vector3... ignoreList)
	{
		super(new IPathCallBack()
		{
			@Override
			public Set<Vector3> getConnectedNodes(Pathfinder finder, Vector3 currentNode)
			{
				System.out.println("AN:" + currentNode.toString());
				Set<Vector3> neighbors = new HashSet<Vector3>();

				for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS)
				{
					Vector3 pos = currentNode.clone().modifyPositionFromSide(direction);
					System.out.println("AN:" + direction.ordinal() + ":" + pos.toString() + "  " + pos.getBlockID(world) + ":" + pos.getBlockMetadata(world));
					LiquidStack liquid = FluidHelper.getLiquidFromBlockId(pos.getBlockID(world));
					if (liquid != null && (liquid.equals(resource) || resource == null))
					{
						System.out.println("ADD:" + pos.toString());
						neighbors.add(pos);
					}
				}

				return neighbors;
			}

			@Override
			public boolean onSearch(Pathfinder finder, Vector3 node)
			{
				LiquidStack liquid = FluidHelper.getLiquidFromBlockId(node.getBlockID(world));
				if (liquid != null && (liquid.equals(resource) || resource == null) && node.getBlockMetadata(world) == 0)
				{
					TileEntity entity = callLoc.getTileEntity(world);
					if (entity instanceof TileEntityDrain)
					{
						((TileEntityDrain) entity).addVectorToQue(node);
					}
				}
				if (finder.closedSet.size() >= 2000)
				{
					return true;
				}

				return false;
			}
		});
	}
}
