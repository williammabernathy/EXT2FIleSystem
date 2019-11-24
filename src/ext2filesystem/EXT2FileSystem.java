package ext2filesystem;

import java.util.Scanner;
import java.io.*;
import java.nio.*;
import java.util.Arrays;
import java.util.Stack;

public class EXT2FileSystem 
{
    //used to list current working directory
    //and to reference superblock data via offsets
    private static RandomAccessFile raf;
    private static ByteBuffer buff;
    
    //reference:
    //https://www.nongnu.org/ext2-doc/ext2.html#superblock
    
    private static Inode inode;
    private static SuperBlock superBlock;
    private static GroupDescriptor groupDescriptor;
    
    private static int blockTotal;
    private static int blocksPerGroupDescrip;
    private static int iNodeSize;
    private static int blockGroupAmount;
    private static String currentDirectory;
    private static Stack<String> pathStack = new Stack<>();
    
    public static void mountImage() throws IOException
    {
        //try to find image and "mount" as read only
        try
        {
            raf = new RandomAccessFile("virtdisk", "r");
        }
        catch(IOException e)
        {
            System.out.println("Something went wong.");
        }
        
        //read all data at superblock offset position
        //then creat SuperBlock object
        byte[] superBlockData = readData(1024, 1024);
        superBlock = new SuperBlock(superBlockData);
        
        //get specific superblock values for creating/finding
        //group descriptors and inodes
        blockTotal = superBlock.getBlockCount();
        blocksPerGroupDescrip = superBlock.getBlocksOfGroups();
        iNodeSize = superBlock.getINodeSize();
        blockGroupAmount = superBlock.calcBlockGroupTotal(blockTotal, blocksPerGroupDescrip);
    
        //get all group descriptor data from offset
        //create group descriptor
        byte[] groupDescriptorData = readData(2048, 1024);
        groupDescriptor = new GroupDescriptor(groupDescriptorData, superBlock.getBlocksOfGroups());
        
        //init to root directory
        pathStack.push("\\");
        currentDirectory = pathStack.peek();
        
        //get all root inode information from offset
        //create initialized inode object
        int rootINodeLoc = inode.getBlockLoc(2, superBlock, groupDescriptor);
        //System.out.println("root inode location: "+rootINodeLoc);
        byte[] rootINodeData = readData(rootINodeLoc, iNodeSize);
        //System.out.println("root inode byte array size: "+rootINodeData.length);
        /*
        for(int i = 0; i < rootINodeData.length; i++)
        {
            System.out.println("*******rootInodeData: "+rootINodeData[i]);
        }
        */
        inode = new Inode(rootINodeData); 
    }
    
    public static byte[] readData(int offset, int breakPoint) throws IOException
    {
        byte[] data = new byte[breakPoint];
        
        //find the referenced offset position, then read all data there
        //store as bytes and return
        raf.seek(offset);
        raf.readFully(data);
    
        return data;
    }
    
    //check the split user input
    //ie array will contain:
    //"cd", "c:/" at array index
    // 0  ,  1
    public static void checkInput(String userInput) throws IOException
    {
        /*
        *
        *
        *   NOTE: This image (virtdisk) uses \ rather than / as seen here: https://i.imgur.com/OAT5joc.png
        *
        *
        */
        String[] splitInput = userInput.split(" ");
        
        String command = splitInput[0];
        
        /*
        for(int i = 0; i < splitInput.length; i++)
        {
            System.out.println("*******input args: "+splitInput[i]);
        }
        */
        
        //is there a path specified
        String path = splitInput.length > 1 ? splitInput[1] : ".";
        //System.out.println("path: " +path);
        
        //split the path based on \
        String[] pathArg = path.split("\\\\");
        
        //clean up white space in argument
        pathArg = Arrays.stream(pathArg).filter(s -> s.length() > 0).toArray(String[]::new);
        
        /*
        for(int i = 0; i < splitInput.length; i++)
        {
            System.out.println("-------PATH args: "+pathArg[i]);
        }
        */
        
        Inode referencedINode = Inode.getReferencedINode(pathArg, iNodeSize, inode, superBlock, groupDescriptor);
        
        if (referencedINode == null) 
        {
            return;
        }
        
        //check the command entered by the user
        switch(command.toLowerCase())
        {
            //change directory
            case "cd":
                //update inode to specified location
                inode = referencedINode;
                
                //check if the path argument is a file
                if(((int) referencedINode.getMode() & 0x8000) == 0x8000)
                {
                    System.out.println("Invalid argument type. The destination is not a directory.");
                }
                else
                {
                    updatePath(pathArg);
                }
                break;
            //list contents of current directory
            case "ls":
                //check if the path argument is a file
                if(((int) referencedINode.getMode() & 0x8000) == 0x8000)
                {
                    System.out.println(pathArg[pathArg.length - 1]);
                }
                else
                {
                    LSCommand(referencedINode);
                }
                break;
            //show contents of file (or files? do we want to show single file or multipe?
            //actual cat command can show many)
            case "cat":
                //check if the path argument is a file
                if(!(((int) referencedINode.getMode() & 0x8000) == 0x8000))
                {
                    System.out.println(path+": Is a directory.");
                }
                else
                {
                    //can just reuse ls command function
                    LSCommand(referencedINode);
                }
                break;
            //exit and close completely
            case "exit":
                System.exit(0);
                break;
            //show all available commands
            case "help":
                System.out.println("Available Commands:");
                System.out.printf("%-30s %-30s\n", "Change Directory:", "cd <directory>");
                System.out.printf("%-30s %-30s\n", "List Directory Contents:", "ls");
                System.out.printf("%-30s %-30s\n", "View file contents:", "cat <filename>");
                System.out.printf("%-30s %-30s\n", "Close System:", "exit");
                break;
            //invalid command
            default:
                System.out.println("Command \""+command+"\" is not recognized.");
                System.out.println("Try the 'help' command to learn more.");
                break;
        }
    }
    
    //update the path when the cd command is used
    //use a stack to for the push or pop commands
    //in order to maneuver directories in either direction
    public static void updatePath(String[] pathArg)
    {
        for(String toPath : pathArg)
        {
            if(toPath.equals("..") && !pathStack.peek().equals("\\"))
            {
               pathStack.pop();
            }
            else if(!toPath.equals(".") && !toPath.equals(".."))
            {
                if(pathStack.peek().equals("\\"))
                {
                    pathStack.push(toPath);
                }
                else
                {
                    pathStack.push("/"+toPath);
                }
            }
        }
        
        Stack<String> pathStackTemp = new Stack<>();
        pathStackTemp.addAll(pathStack);
        StringBuilder sb = new StringBuilder();

        while(!pathStackTemp.empty())
        {
            sb.insert(0, pathStackTemp.pop());
        }
        
        currentDirectory = sb.toString();
    }
    
    public static void LSCommand(Inode referencedINode) throws IOException
    {
        //The block pointers
        int[] blockPointers = referencedINode.getBlocks();
        
        /*
        for(int l = 0; l < blockPointers.length; l++)
        {
            System.out.println("pointer data at position "+l+": "+blockPointers[l]);
        }
        */

        for (int i = 0; i < 12; i++) 
        {
            if (blockPointers[i] != 0) 
            {
                readLSCommandData(referencedINode, blockPointers[i]);
            }
        }
        
        //check indirect data
        //this retreives the nested inodes and inodes referencing other inodes
        //then prints the content they reference
        if(blockPointers[12] != 0)
            readFirstIndirect(referencedINode, blockPointers[12]);
        if(blockPointers[13] != 0)
            readSecondIndirect(referencedINode, blockPointers[13]);
        if(blockPointers[14] != 0)
            readThirdIndirect(referencedINode, blockPointers[14]);
    }
    
    //read the data from current inode
    public static void readLSCommandData(Inode referencedINode, int blockPointers) throws IOException
    {
        byte[] blockData = readData(blockPointers * 1024, 1024);

        if (((int) referencedINode.getMode() & 0x8000) == 0x8000) 
        {
            //the bytes converting to a String and trim() removes whitespace
            String str = new String(blockData).trim();
            System.out.print(str);
        } 
        else 
        {
            ByteBuffer buffer = ByteBuffer.wrap(blockData);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int directoryLength;

            for (int j = 0; j < buffer.limit(); j += directoryLength) {
                //because the index is 4 bytes long
                directoryLength = buffer.getInt(j + 4);

                //8 bits in size, located after directoryLength
                byte nameBytes = buffer.get(j + 6);
                byte[] charBytes = new byte[nameBytes];

                for (int k = 0; k < charBytes.length; k++) {
                    //get each char from the array
                    charBytes[k] = buffer.get(j + k + 8);
                }

                int containingBlock = Inode.getBlockLoc(2, superBlock, groupDescriptor);

                byte[] otherData = readData(containingBlock, iNodeSize);

                Inode iNodeData = new Inode(otherData);

                //Get the correct size of the file
                long fileSize = ((long) iNodeData.getSizeUpper() << 32) | ((long) iNodeData.getSizeLower() & 0xFFFFFFFFL);

                //print inode info
                System.out.format("UID: %-12s\tGID: %-7s\tFile Size: %-30s\t%-30s%n",
                        iNodeData.getUID(), iNodeData.getGID(), fileSize, new String(charBytes).trim());
            }
        }
    }
    
    //read data from first indirect inode (first nested)
    public static void readFirstIndirect(Inode referencedINode, int blockPointers) throws IOException
    {
        byte[] blockPointerData = readData(blockPointers * 1024, 1024);
        ByteBuffer buff2 = ByteBuffer.wrap(blockPointerData);
        buff2.order(ByteOrder.LITTLE_ENDIAN);

        for(int i = 0; i < buff2.limit(); i += 4) 
        {
            if(buff2.getInt(i) != 0)
                readLSCommandData(referencedINode, buff2.getInt(i));
        }
    }
    
    //read data from second indirect inodes (a nested > nested scenario)
    public static void readSecondIndirect(Inode referencedINode, int blockPointers) throws IOException
    {
        byte[] blockPointerData = readData(blockPointers * 1024, 1024);
        ByteBuffer buff2 = ByteBuffer.wrap(blockPointerData);
        buff2.order(ByteOrder.LITTLE_ENDIAN);

        for(int i = 0; i < buff2.limit(); i += 4) 
        {
            if(buff2.getInt(i) != 0)
                readFirstIndirect(referencedINode, buff2.getInt(i));
        }
    }
    
    //read data from third indirect inodes (nested > nested > nested> scenario)
    public static void readThirdIndirect(Inode referencedINode, int blockPointers) throws IOException
    {
        byte[] blockPointerData = readData(blockPointers * 1024, 1024);
        ByteBuffer buff2 = ByteBuffer.wrap(blockPointerData);
        buff2.order(ByteOrder.LITTLE_ENDIAN);

        for(int i = 0; i < buff2.limit(); i += 4) 
        {
            if(buff2.getInt(i) != 0)
                readSecondIndirect(referencedINode, buff2.getInt(i));
        }
    }
    
    public static void main(String[] args) throws IOException
    {
        boolean running = true;                                 //is instance running
        Scanner input = new Scanner(System.in);                 //input scanner
        String userInput;                                       //string to store user input
        String[] splitInput;                                    //array to handle splitting user input

        mountImage();
        
        //converting the bytes that contain the name to a String
        String volumeRootName = superBlock.getVolumeRootName();
        
        //UI running
        while(running)
        {
            System.out.print(volumeRootName + currentDirectory + " >>> ");
            userInput = input.nextLine();
            
            checkInput(userInput);
        }
    }
    
}