package ext2filesystem;

import java.util.Scanner;

public class EXT2FileSystem 
{
    //used to list current working directory
    private static String dir = "";
    
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
    
    public static void main(String[] args) 
    {
        boolean running = true;                                 //is instance running
        Scanner input = new Scanner(System.in);                 //input scanner
        String userInput = "";                                  //string to store user input
        String[] splitInput;                                    //array to handle split user input
        
        /*
        ADD THE CONNECTION/DRIVE CONFIG HERE
        IE. This is where we will do the conversion of
        the drive to a byte array. 
        */
        
        //test dir for visualizing
        dir = "C:/users/willthethrill/";
        
        while(running)
        {
            System.out.print("DIR>" + dir + ">>>");
            userInput = input.next();
            
            splitInput = userInput.split(" ");
            
            checkInput(splitInput);
        }
    }
    
}