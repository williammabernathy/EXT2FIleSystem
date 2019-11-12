package ext2filesystem;

import java.util.Scanner;
import java.io.*;
import java.nio.*;

public class EXT2FileSystem 
{
    //used to list current working directory
    private static String dir = "";
    private static RandomAccessFile raf;
    private static ByteBuffer buff;
    
    //check the split user input
    //ie array will contain:
    //"cd", "c:/" at array index
    // 0  ,  1
    public static void checkInput(String[] splitInput)
    {
        String command = splitInput[0];
        
        //check the command entered by the user
        switch(command)
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
            //invalid command
            default:
                System.out.println("Command \""+command+"\" is not recognized.");
                break;
        }
    }
    
    public static byte[] readData() throws IOException
    {
        int offset = 1024;
        byte[] data = new byte[offset];
        raf.seek(offset);
        raf.readFully(data);
        
        return data;
    }
    
    public static void main(String[] args) throws IOException
    {
        boolean running = true;                                 //is instance running
        Scanner input = new Scanner(System.in);                 //input scanner
        String userInput;                                  //string to store user input
        String[] splitInput;                                    //array to handle split user input
        
        /*
        ADD THE CONNECTION/DRIVE CONFIG HERE
        IE. This is where we will do the conversion of
        the drive to a byte array. 
        */
        
        //test dir for visualizing
        dir = "users/willthethrill/";
        
        try
        {
            raf = new RandomAccessFile("virtdisk", "r");
        }
        catch(IOException e)
        {
            System.out.println("Something went wong.");
        }
        
        
        byte[] dataConv = readData();
        
        buff = ByteBuffer.wrap(dataConv);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        
        byte[] char_bytes = new byte[16];

        for(int i = 0; i < 16; i++) 
        {
            char_bytes[i] = buff.get(128 + i);
        }

        //converting the bytes that contain the name to a String
        String volumeName = new String(char_bytes);
        
        while(running)
        {
            System.out.print(volumeName + "/" + dir + " >>> ");
            userInput = input.next();
            
            splitInput = userInput.split(" ");
            
            checkInput(splitInput);
        }
    }
    
}