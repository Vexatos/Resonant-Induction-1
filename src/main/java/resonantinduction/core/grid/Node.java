package resonantinduction.core.grid;

import net.minecraft.nbt.NBTTagCompound;

public abstract class Node<G extends IGrid> implements INode<G>
{
	public G grid = null;

	@Override
	public final G getGrid()
	{
		if (grid == null)
			grid = newGrid();

		return grid;
	}

	protected abstract G newGrid();

	@Override
	public final void setGrid(G grid)
	{
		this.grid = grid;
	}

	@Override
	public void reconstruct()
	{
		recache();
		getGrid().reconstruct();
	}

	public void recache()
	{

	}

	public void load(NBTTagCompound nbt)
	{

	}

	public void save(NBTTagCompound nbt)
	{

	}
}