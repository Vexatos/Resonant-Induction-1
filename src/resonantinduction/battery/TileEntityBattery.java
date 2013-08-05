/**
 * 
 */
package resonantinduction.battery;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import resonantinduction.PacketHandler;
import resonantinduction.api.IBattery;
import resonantinduction.api.ITesla;
import resonantinduction.base.IPacketReceiver;
import resonantinduction.base.ListUtil;
import resonantinduction.base.TileEntityBase;
import resonantinduction.tesla.TeslaGrid;

import com.google.common.io.ByteArrayDataInput;

/**
 * A modular battery with no GUI.
 * 
 * @author AidanBrady
 */
public class TileEntityBattery extends TileEntityBase implements IPacketReceiver, IInventory, ITesla
{
	public SynchronizedBatteryData structure = SynchronizedBatteryData.getBase(this);
	
	public SynchronizedBatteryData prevStructure;
	
	public float clientEnergy;
	public int clientCells;
	public float clientMaxEnergy;

	@Override
	public void updateEntity()
	{
		ticks++;
		
		if(ticks == 1)
		{
			TeslaGrid.instance().register(this);
		}
		
		if(!worldObj.isRemote)
		{
			if(playersUsing.size() > 0)
			{
				PacketHandler.sendTileEntityPacketToClients(this, getNetworkedData(new ArrayList()).toArray());
			}
			
			if(ticks == 5 && !structure.isMultiblock)
			{
				update();
			}
			
			if(structure.visibleInventory[0] != null)
			{
				if(structure.inventory.size() < structure.getMaxCells())
				{
					structure.inventory.add(structure.visibleInventory[0]);
					structure.visibleInventory[0] = null;
					structure.sortInventory();
				}
			}
			
			if(prevStructure != structure)
			{
				for(EntityPlayer player : playersUsing)
				{
					player.closeScreen();
				}
				
				PacketHandler.sendTileEntityPacketToClients(this, getNetworkedData(new ArrayList()).toArray());
			}
			
			prevStructure = structure;
			
			structure.wroteInventory = false;
			structure.didTick = false;
		}
	}
	
	@Override
	public void invalidate()
	{
		TeslaGrid.instance().unregister(this);
		super.invalidate();
	}
	
	@Override
    public void readFromNBT(NBTTagCompound nbtTags)
    {
        super.readFromNBT(nbtTags);
        
        //Main inventory
        if(nbtTags.hasKey("Items"))
        {
	        NBTTagList tagList = nbtTags.getTagList("Items");
	        structure.inventory = new ArrayList<ItemStack>();
	
	        for(int tagCount = 0; tagCount < tagList.tagCount(); tagCount++)
	        {
	            NBTTagCompound tagCompound = (NBTTagCompound)tagList.tagAt(tagCount);
	            int slotID = tagCompound.getInteger("Slot");
	            structure.inventory.add(slotID, ItemStack.loadItemStackFromNBT(tagCompound));
	        }
        }
        
        //Visible inventory
        if(nbtTags.hasKey("VisibleItems"))
        {
	        NBTTagList tagList = nbtTags.getTagList("VisibleItems");
	        structure.visibleInventory = new ItemStack[3];
	
	        for(int tagCount = 0; tagCount < tagList.tagCount(); tagCount++)
	        {
	            NBTTagCompound tagCompound = (NBTTagCompound)tagList.tagAt(tagCount);
	            byte slotID = tagCompound.getByte("Slot");
	
	            if(slotID >= 0 && slotID < structure.visibleInventory.length)
	            {
	            	if(slotID == 0)
	            	{
	            		setInventorySlotContents(slotID, ItemStack.loadItemStackFromNBT(tagCompound));
	            	}
	            	else {
	            		setInventorySlotContents(slotID+1, ItemStack.loadItemStackFromNBT(tagCompound));
	            	}
	            }
	        }
        }
    }

	@Override
    public void writeToNBT(NBTTagCompound nbtTags)
    {
        super.writeToNBT(nbtTags);
        
        if(!structure.wroteInventory)
        {
	        //Inventory
        	if(structure.inventory != null)
        	{
		        NBTTagList tagList = new NBTTagList();
		
		        for(int slotCount = 0; slotCount < structure.inventory.size(); slotCount++)
		        {
		            if(structure.inventory.get(slotCount) != null)
		            {
		                NBTTagCompound tagCompound = new NBTTagCompound();
		                tagCompound.setInteger("Slot", slotCount);
		                structure.inventory.get(slotCount).writeToNBT(tagCompound);
		                tagList.appendTag(tagCompound);
		            }
		        }
		
		        nbtTags.setTag("Items", tagList);
        	}
	        
	        //Visible inventory
        	if(structure.visibleInventory != null)
        	{
		        NBTTagList tagList = new NBTTagList();
		
		        for(int slotCount = 0; slotCount < structure.visibleInventory.length; slotCount++)
		        {
		        	if(slotCount > 0)
		        	{
		        		slotCount++;
		        	}
		        	
		            if(getStackInSlot(slotCount) != null)
		            {
		                NBTTagCompound tagCompound = new NBTTagCompound();
		                tagCompound.setByte("Slot", (byte)slotCount);
		                getStackInSlot(slotCount).writeToNBT(tagCompound);
		                tagList.appendTag(tagCompound);
		            }
		        }
		
		        nbtTags.setTag("VisibleItems", tagList);
        	}
	        
	        structure.wroteInventory = true;
        }
    }
	
	public void update()
	{
		if(!worldObj.isRemote && (structure == null || !structure.didTick))
		{
			new BatteryUpdateProtocol(this).updateBatteries();
			
			if(structure != null)
			{
				structure.didTick = true;
			}
		}
	}
	
	/**
	 * @return added energy
	 */
	public float addEnergy(float amount, boolean doAdd)
	{
		float added = 0;
		
		for(ItemStack itemStack : structure.inventory)
		{
			if(itemStack.getItem() instanceof IBattery)
			{
				IBattery battery = (IBattery)itemStack.getItem();
				
				float needed = amount-added;
				float itemAdd = Math.min(battery.getMaxEnergyStored(itemStack)-battery.getEnergyStored(itemStack), needed);
				
				if(doAdd)
				{
					battery.setEnergyStored(itemStack, battery.getEnergyStored(itemStack) + itemAdd);
				}
				
				added += itemAdd;
				
				if(amount == added)
				{
					break;
				}
			}
		}
		
		return added;
	}
	
	/**
	 * @return removed energy
	 */
	public float removeEnergy(float amount, boolean doRemove)
	{
		List<ItemStack> inverse = ListUtil.inverse(structure.inventory);
		
		float removed = 0;
		
		for(ItemStack itemStack : inverse)
		{
			if(itemStack.getItem() instanceof IBattery)
			{
				IBattery battery = (IBattery)itemStack.getItem();
				
				float needed = amount-removed;
				float itemRemove = Math.min(battery.getEnergyStored(itemStack), needed);
				
				if(doRemove)
				{
					battery.setEnergyStored(itemStack, battery.getEnergyStored(itemStack) - itemRemove);
				}
				
				removed += itemRemove;
				
				if(amount == removed)
				{
					break;
				}
			}
		}
		
		return removed;
	}

	public float getMaxEnergyStored()
	{
		if(!worldObj.isRemote)
		{
			float max = 0;
	
			for (ItemStack itemStack : structure.inventory)
			{
				if (itemStack != null)
				{
					if (itemStack.getItem() instanceof IBattery)
					{
						max += ((IBattery) itemStack.getItem()).getMaxEnergyStored(itemStack);
					}
				}
			}
	
			return max;
		}
		else {
			return clientMaxEnergy;
		}
	}
	
	public float getEnergyStored()
	{
		if(!worldObj.isRemote)
		{
			float energy = 0;
			
			for (ItemStack itemStack : structure.inventory)
			{
				if (itemStack != null)
				{
					if (itemStack.getItem() instanceof IBattery)
					{
						energy += ((IBattery) itemStack.getItem()).getEnergyStored(itemStack);
					}
				}
			}
			
			return energy;
		}
		else {
			return clientEnergy;
		}
	}

	@Override
	public void handle(ByteArrayDataInput input) 
	{
		try {
			structure.isMultiblock = input.readBoolean();
			
			clientEnergy = input.readFloat();
			clientCells = input.readInt();
			clientMaxEnergy = input.readFloat();
			
			structure.height = input.readInt();
			structure.length = input.readInt();
			structure.width = input.readInt();
		} catch(Exception e) {}
	}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		data.add(structure.isMultiblock);
		
		data.add(getEnergyStored());
		data.add(structure.inventory.size());
		data.add(getMaxEnergyStored());
		
		data.add(structure.height);
		data.add(structure.length);
		data.add(structure.width);
		
		return data;
	}

	@Override
	public int getSizeInventory() 
	{
		return 4;
	}

	@Override
	public ItemStack getStackInSlot(int i)
	{
		if(i == 0)
		{
			return structure.visibleInventory[0];
		}
		else if(i == 1)
		{
			if(!worldObj.isRemote)
			{
				return ListUtil.getTop(structure.inventory);
			}
			else {
				return structure.tempStack;
			}
		}
		else {
			return structure.visibleInventory[i-1];
		}
	}

	@Override
	public ItemStack decrStackSize(int slotID, int amount) 
	{
        if(getStackInSlot(slotID) != null)
        {
            ItemStack tempStack;

            if(getStackInSlot(slotID).stackSize <= amount)
            {
                tempStack = getStackInSlot(slotID);
                setInventorySlotContents(slotID, null);
                return tempStack;
            }
            else {
                tempStack = getStackInSlot(slotID).splitStack(amount);

                if(getStackInSlot(slotID).stackSize == 0)
                {
                	setInventorySlotContents(slotID, null);
                }

                return tempStack;
            }
        }
        else {
            return null;
        }
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i) 
	{
		return getStackInSlot(i);
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) 
	{
		if(i == 0)
		{
			structure.visibleInventory[0] = itemstack;
		}
		else if(i == 1)
		{
			if(itemstack == null)
			{
				if(!worldObj.isRemote)
				{
					structure.inventory.remove(ListUtil.getTop(structure.inventory));
				}
				else {
					structure.tempStack = null;
				}
			}
			else {
				if(worldObj.isRemote)
				{
					structure.tempStack = itemstack;
				}
			}
		}
		else {
			structure.visibleInventory[i-1] = itemstack;
		}
	}

	@Override
	public String getInvName()
	{
		return "Battery";
	}

	@Override
	public boolean isInvNameLocalized() 
	{
		return false;
	}

	@Override
	public int getInventoryStackLimit() 
	{
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer) 
	{
		return true;
	}

	@Override
	public void openChest() {}

	@Override
	public void closeChest() {}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) 
	{
		return false;
	}

	@Override
	public float transfer(float transferEnergy, boolean doTransfer) 
	{
		return addEnergy(transferEnergy, doTransfer);
	}

	@Override
	public boolean canReceive(TileEntity transferTile)
	{
		return true;
	}
}
