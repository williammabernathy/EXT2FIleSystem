package ext2filesystem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class GroupDescriptor 
{
    private ByteBuffer buff;
    
    private int blocksOfGroups;
    private int[] groupDescriptorPointer;
    
    public GroupDescriptor()
    {
        
    }
    
    public GroupDescriptor(byte[] byteArray, int blocksOfGroups)
    {
        buff = ByteBuffer.wrap(byteArray);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        
        //reference
        //https://www.nongnu.org/ext2-doc/ext2.html#block-group-descriptor-table
        this.blocksOfGroups = blocksOfGroups;
        
        groupDescriptorPointer = new int[blocksOfGroups];
        
        //size of inode tables
        //always same regarldess of used allocated space
        int groupDescriptorSize = 32;
        
        //where the inode reference is
        int inodePointer = 8;
        
        //get groupDescriptor pointers
        for (int i = 0; i < 15; i++) 
        {
            groupDescriptorPointer[i] = buff.getInt((groupDescriptorSize * i) + inodePointer);
        }
    }
    
    //group descriptor size
    public void setBlocksOfGroups(int blocksOfGroups)
    {
        this.blocksOfGroups = blocksOfGroups;
    }
    
    public int getBlocksOfGroups()
    {
        return this.blocksOfGroups;
    }
    
    //group descriptor pointers
    public int[] getGroupDescriptorSize()
    {
        return groupDescriptorPointer;
    }
}
