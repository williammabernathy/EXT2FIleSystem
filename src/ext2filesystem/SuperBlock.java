package ext2filesystem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SuperBlock 
{
    //reference:
    //https://www.nongnu.org/ext2-doc/ext2.html#superblock
    
    private static int iNodeCount;
    private static int blockCount;
    private static int blocksPerGroups;
    private static int iNodePerGroups;
    private static int iNodeSize;
    private static String volumeRootName;
    
    private ByteBuffer buff;
    
    public SuperBlock()
    {
        
    }
    
    public SuperBlock(byte[] byteArray)
    {
        buff = ByteBuffer.wrap(byteArray);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        
        //array to store the bytes of characters
        byte[] charBytes = new byte[16];

        //get the characters that represent the volume root name
        for(int i = 0; i < 16; i++) 
        {
            charBytes[i] = buff.get(128 + i);
        }
        
        volumeRootName = new String(charBytes);
                
        //retrieve information from superblock using various offsets
        //as referenced from documentation link
        iNodeCount = buff.getInt(0);
        blockCount = buff.getInt(4);
        blocksPerGroups = buff.getInt(32);
        iNodePerGroups = buff.getInt(40);
        iNodeSize = buff.getInt(88);
    }
    
    public String getVolumeRootName()
    {
        return this.volumeRootName;
    }
    
    public int getINodeCount()
    {
        return this.iNodeCount;
    }
    
    public int getBlockCount()
    {
        return this.blockCount;
    }
    
    public int getBlocksOfGroups()
    {
        return this.blocksPerGroups;
    }
    
    public int getINodePerGroups()
    {
        return this.iNodePerGroups;
    }
    
    public int getINodeSize()
    {
        return this.iNodeSize;
    }
    
    public int calcBlockGroupTotal(int blockAmount, int perGroup)
    {
        int count = (blockAmount / perGroup);
        
        //add one if odd division for count
        //ie 2/3
        //***Ceiling???***
        if((blockAmount % perGroup) != 0)
            count++;
        
        return count;
    }
}
