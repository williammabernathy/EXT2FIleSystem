package ext2filesystem;

import java.util.Scanner;
import java.io.*;
import java.nio.*;

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
        
        blockTotal = superBlock.getBlockCount();
        blocksPerGroupDescrip = superBlock.getBlocksOfGroups();
        iNodeSize = superBlock.getINodeSize();
        blockGroupAmount = superBlock.calcBlockGroupTotal(blockTotal, blocksPerGroupDescrip);
    
        //get all group descriptor data from offset
        //create group descriptor
        byte[] groupDescriptorData = readData(2048, 1024);
        groupDescriptor = new GroupDescriptor(groupDescriptorData, superBlock.getBlocksOfGroups());
        
        //get all inode information from offset
        //create initialized inode object
        int rootINodeLoc = inode.getBlockLoc(superBlock, groupDescriptor);
        byte[] rootINodeData = readData(rootINodeLoc, superBlock.getINodeSize());
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
    public static void checkInput(String[] splitInput)
    {
        String command = splitInput[0];
        
        //check the command entered by the user
        switch(command.toLowerCase())
        {
            //change directory
            case "cd":
                break;
            //list contents of current directory
            case "ls":
                break;
            //show contents of file (or files? do we want to show single file or multipe?
            //actual cat command can show many)
            case "cat":
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
            System.out.print(volumeRootName + " >>> ");
            userInput = input.next();
            
            splitInput = userInput.split(" ");
            
            checkInput(splitInput);
        }
    }
    
}