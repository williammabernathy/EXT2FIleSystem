package ext2filesystem;

import java.io.IOException;
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
    //private int links;              //26        links count
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
        
        initInode();
    }
    
    public void initInode()
    {
        //get inode table data
        mode = buff.getInt(0);
        uid = buff.getInt(2);
        sizeLower = buff.getInt(4);
        sizeUpper = buff.getInt(108);
        atime = buff.getInt(8);
        ctime = buff.getInt(12);
        mtime = buff.getInt(16);
        dtime = buff.getInt(20);
        gid = buff.getInt(24);
        //links = buff.getInt(26);

        for(int i = 0; i < 15; i++)
        {
            //40, 44, 48, 52, etc..
            blocks[i] = buff.getInt(40 + (i * 4));
            //System.out.println("inode blocks init: "+buff.getInt(40 + (i * 4)));
        }
    }
    
    public int[] getBlocks()
    {
        return this.blocks;
    }
    
    public int getMode()
    {
        return this.mode;
    }
    
    public int getUID()
    {
        return this.uid;
    }
    public int getSizeUpper()
    {
        return this.sizeUpper;
    }
    public int getSizeLower()
    {
        return this.sizeLower;
    }
    public int getATime()
    {
        return this.atime;
    }
    public int getCTime()
    {
        return this.ctime;
    }
    
    public int getMTime()
    {
        return this.mtime;
    }
    
    public int getDTime()
    {
        return this.dtime;
    }
    
    public int getGID()
    {
        return this.gid;
    }
    
    public static int getBlockLoc(int iNodeOffset, SuperBlock superBlock, GroupDescriptor groupDescriptor) throws IOException
    {
        //get our inode information from the superblock
        
        //number of inodes in entire system
        int iNodeAmount = superBlock.getINodeCount();
        //System.out.println("number of inodes "+superBlock.getINodeCount());
        
        //number of inodes per group block
        int iNodesPerGroup = superBlock.getINodePerGroups();
        //System.out.println("inodes per group "+superBlock.getINodeGroups());
        
        //size of each inode
        int iNodeSize = superBlock.getINodeSize();
        //System.out.println("inode size "+superBlock.getINodeSize());

        //block group with inode
        int pointerDiv;
        //group descriptor pointers
        int[] gDescPointer = groupDescriptor.getGroupDescriptorSize();
        //inode table pointer
        int inodeTablePointer;
        //index of inode
        double pointer;
        //number of the containing block
        double containingBlock;

        if (iNodeOffset >= 2) 
        {
            if (iNodeOffset < iNodeAmount) 
            {
                iNodeOffset -= 1;
                pointerDiv = iNodeOffset / iNodesPerGroup;
                pointer = iNodeOffset % iNodesPerGroup;
                inodeTablePointer = gDescPointer[pointerDiv];
                containingBlock = ((pointer * iNodeSize / 1024) + inodeTablePointer) * 1024;

                //convert to int to return
                return (int) containingBlock;
            }
        }
        
        return 0;
    }
    
    public static Inode getReferencedINode(String[] pathArg, int iNodeSize, Inode inode, SuperBlock superBlock, GroupDescriptor groupDescriptor) throws IOException
    {
        Inode referencedInode = inode;
        
        for (String byteData : pathArg) 
        {
            int inodeOffset = findInodeOffset(byteData, referencedInode);
            //System.out.println("inode offset: "+inodeOffset);
            
            //look for the path
            if (inodeOffset > 0) 
            {
                //retrieve inode from specified path
                //then read the data to build a new inode
                //System.out.println("block location "+getBlockLoc(inodeOffset, superBlock, groupDescriptor));
                byte[] iNodeData = EXT2FileSystem.readData((Inode.getBlockLoc(inodeOffset, superBlock, groupDescriptor)), iNodeSize);
                referencedInode = new Inode(iNodeData);
                referencedInode.initInode();
            } 
            else 
            {
                System.out.println("Specified file or directory does not exist.");
                return null;
            }
        }
        
        return referencedInode;
    }
    
    private static int findInodeOffset(String path, Inode inode) throws IOException 
    {
        int[] pointers = inode.getBlocks();
        int data = 0;

        //can find the poitners at specific locations
        //unaffected by location as offset is constant
        for (int i = 0; i < 12; i++) 
        {
            //System.out.println("find offset first loop iteration: "+i);
            //check that there are pointers
            //meaning data can be referenced at pointer
            if(pointers[i] != 0) 
            {
                //get the data referenced by pointers since they exist
                byte[] imageData = EXT2FileSystem.readData((pointers[i] * 1024), 1024);
                ByteBuffer buff = ByteBuffer.wrap(imageData);
                buff.order(ByteOrder.LITTLE_ENDIAN);
                
                int recordLength;

                //limit() = buff size
                for(int j = 0; j < buff.limit(); j += recordLength) 
                {
                    //recLength is used to update record length (the loop's step)
                    recordLength = buff.getInt(j + 4);
                    
                    //using bytearray, get the characters that reference a name
                    //found via pointer + offset locations
                    byte[] nameBytes = new byte[buff.get(j + 6)];

                    //get byte names
                    for(int k = 0; k < nameBytes.length; k++) 
                    {
                        nameBytes[k] = buff.get(k + j + 8);
                    }
                    
                    //clean/check white space
                    if(path.equals(new String(nameBytes).trim())) 
                    {
                        //System.out.println("end of offset: "+buff.getInt(j));
                        return buff.getInt(j);
                    }
                }
            }
        }
        
        //return 0 if nothing is found
        return data;
    }
}
