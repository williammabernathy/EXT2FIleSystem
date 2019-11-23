package ext2filesystem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Inode 
{
    //reference:
    //https://www.nongnu.org/ext2-doc/ext2.html#inode-table
    
    private int mode;               //0
    private int uid;                //2
    private int	sizeLower;          //4
    private int sizeUpper;          //108       dir_acl
    private int atime;              //8         access time
    private int	ctime;              //12        creation time
    private int mtime;              //16        modification time
    private int	dtime;              //20        deletion time
    private int gid;                //24        
    private int links;              //26        links count
    private int[] blocks;           //28        pointer
    //private int flags;
    //private int osd1;
    //private int generation;

    private ByteBuffer buff;
    
    public Inode(byte[] byteArray)
    {
        buff = ByteBuffer.wrap(byteArray);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        
        //init block pointer array
        blocks = new int[15];
        
        //get inode table data
        mode = buff.getShort(0);
        uid = buff.getShort(2);
        sizeLower = buff.getInt(4);
        sizeUpper = buff.getInt(108);
        atime = buff.getInt(8);
        ctime = buff.getInt(12);
        mtime = buff.getInt(16);
        dtime = buff.getInt(20);
        gid = buff.getShort(24);
        links = buff.getShort(26);

        for(int i = 0; i < blocks.length; i++)
        {
            blocks[i] = buff.getInt(40+ (i * 4)); //increments of 4 bytes
        }
    }
    
    static int getBlockLoc(SuperBlock superblock, GroupDescriptor groupDescriptor) 
    {

        int rootInodeOffset = 2;
        
        //get our inode information from the superblock
        int inodeAmount = superblock.getINodeCount();
        int iNodesPerGroup = superblock.getINodeGroups();
        int iNodeSize = superblock.getINodeSize();

        int pointerDiv;
        int[] gDescPointer = groupDescriptor.getGroupDescriptorSize();
        int inodeTablePointer;
        double pointer;
        double containingBlock;

        if (rootInodeOffset < inodeAmount) 
        {
            rootInodeOffset -= 1;
            pointerDiv = rootInodeOffset / iNodesPerGroup;
            pointer = rootInodeOffset % iNodesPerGroup;
            inodeTablePointer = gDescPointer[pointerDiv];
            containingBlock = ((pointer * iNodeSize / 1024) + inodeTablePointer) * 1024;

            //convert to int to return
            return (int) containingBlock;
        }
        
        return 0;
    }
}
