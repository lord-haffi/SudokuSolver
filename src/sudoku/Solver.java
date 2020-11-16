/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sudoku;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This Sudoku-Solver solves sudokus with "human" solving-strategies with a propagation of actions and events.
 * There are generally 2 types of actions and 2 types of events in this Solver:
 * Actions:
 *    -fixField(i,j,num): Fix a field to a specified number.
 *    -reduceField(i,j,num): Reducing a field means the registration of the impossibility of a number for the field (i,j).
 * Events:
 *    -fieldFixed(i,j,num): The field (i,j) was fixed to a specified number.
 *    -fieldReduced(i,j,num): The impossibility of a number for a field (i,j) was registrated.
 * The propagation is quiet simple: Actions may result in some events. Events may result in some actions.
 * Due to the propagation, it is difficult to estimate the runtime of this program for a sudoku.
 * 
 * @author lord_haffi
 */
public class Solver {
    /**
     * Constants for the read-method
     */
    private static final int n0 = (int)'0', n9 = (int)'9', s = (int)' ', r = (int)'\r', n = (int)'\n';
    
    /**
     * 'field' contains the sudoku. The interpretation is 'field[rowIndex][columnIndex]'
     */
    private static byte[][] field = new byte[9][9];
    /**
     * This field contains flags for each entry in 'field' (i,j) if a number (1-9) is possible (or impossible).
     * The interpretation is 'possib[rowIndex][columnIndex][possNumber-1]', 'possNumber' = the number for which you want to know if it's possible
     * that this entry can be 'possNumber'.
     */
    private static boolean[][][] possib = new boolean[9][9][9];
    /**
     * A constant indicating, if the program should choose "random" numbers, if the sudoku is not finished.
     * I used this method to think about more rules/solving strategies I can implement, when the program runs into a contradiction.
     */
    private static final boolean chooseNum = false;
    
    /**
     * Runs the program and solves all specified sudokus.
     * All solutions will be simply added to the output without any extra characters.
     * The output will look like:
     * 123456789
     * 123456789
     * 123456789
     * 123456789
     * 123456789
     * 123456789
     * 123456789
     * 123456789
     * 123456789
     * 987654321
     * 987654321
     * 987654321
     * ....
     * If a sudoku has a contradiction in itself, an error with a little description will be printed followed by a detailed output of the sudoku.
     * 
     * @param args A list of file-paths to sudokus which will be solved (or not) by this program. If the list is empty, an error will be printed to stdout.
     */
    public static void main(String[] args){
        /*int start = 3, end=7;
        args = new String[end-start+1];
        for(int i=start; i<=end; i++)
            args[i-start] = "D:\\grids\\grid"+i+".txt";*/
        
        if(args.length != 0){
            for(String fileS : args){
                File file = new File(fileS);
                try {
                    //long time1 = System.currentTimeMillis();
                    read(new FileInputStream(file));
                    //long time2 = System.currentTimeMillis();
                    solve();
                    //long time3 = System.currentTimeMillis();
                    //System.out.println("Sudoku '"+file.getName()+"': reading time: "+(time2-time1)+"ms, solving time: "+(time3-time2)+"ms, validity: "+checkValidity());
                    //print(true);
                    printRudimentary();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (RuntimeException ex) {
                    System.err.println(ex.getMessage());
                    print(true);
                }
            }
        }else
            System.out.println("No file to a Sudoku in parameter-list.");
    }
    /**
     * Reads The sudoku from a InputStream.
     * The format is:
     * '1'-'9' = a number
     * ' ' = an empty field
     * '\n' or '\r\n' = next row of the sudoku
     * There must be 9 rows of 9 characters followed by a 'newLine'.
     * All content after the sudoku will be ignored.
     * 
     * @param input The InputStream from the sudoku.
     * @throws IOException If it couldn't be read from the InputStream.
     * @throws RuntimeException If the format of the content is invalid.
     */
    public static void read(InputStream input) throws IOException, RuntimeException{
        field = new byte[9][9];
        possib = new boolean[9][9][9];
        int next;
        int i = 0, j = 0; // field (i,j)
        while((next = input.read()) != -1){
            if(next > n0 && next <= n9){
                if(j == 9)
                    throw new RuntimeException("Illegal amount of columns: >"+j);
                field[i][j] = (byte)(next-n0);
                j++;
            }else if(next == s){
                if(j == 9)
                    throw new RuntimeException("Illegal amount of columns: >"+j);
                field[i][j] = 0;
                
                for(int poss=0; poss<9; poss++)
                    possib[i][j][poss] = true;
                    
                j++;
            }else if(next == n || next == r){
                if(next == r){
                    int next2 = input.read();
                    if(next2 != n)
                        throw new RuntimeException("Illegal string: \\r"+(char)next2);
                }
                if(j != 9)
                    throw new RuntimeException("Illegal amount of columns: "+j);
                j = 0;
                i++;
                if(i == 9) // ignore anything in the file after the sudoku
                    return;
            }else
                throw new RuntimeException("Illegal character: "+(char)next);
        }
        if(i != 8)
            throw new RuntimeException("Illegal amount of rows: "+(i+1));
        if(j != 9)
            throw new RuntimeException("Illegal amount of columns: "+j);
    }
    /**
     * Prints the sudoku to stdout in the format of the input.
     */
    public static void printRudimentary(){
        for(int i=0; i<9; i++){
            for(int j=0; j<9; j++){
                if(field[i][j] != 0)
                    System.out.print(field[i][j]);
                else
                    System.out.print(" ");
            }
            System.out.println();
        }
    }
    /**
     * Prints the sudoku to stdout with a nicer look.
     * @param withDetail true = additionally print the sudoku with the possibilities of each field. (will be printed "next to" the standard sudoku output, not after)
     */
    public static void print(boolean withDetail){
        for(int i=0; i<9; i++){
            if(i%3 == 0){
                System.out.print("+-----+-----+-----+");
                if(withDetail)
                    System.out.print("     +---------------------------------+---------------------------------+---------------------------------+");
                System.out.println();
            }
            String lineDet = "     ";
            for(int j=0; j<9; j++){
                if(j%3 == 0)
                    System.out.print("|");
                else
                    System.out.print(" ");
                if(field[i][j] != 0)
                    System.out.print(field[i][j]);
                else
                    System.out.print(" ");
                
                if(withDetail){
                    if(j%3 == 0)
                        lineDet+="|";
                    if(field[i][j] != 0)
                        lineDet+="     "+field[i][j]+"     ";
                    else{
                        String out = "{", app = "}";
                        boolean left = true;
                        for(int poss=0; poss<9; poss++)
                            if(possib[i][j][poss])
                                out+=(poss+1);
                            else{
                                if(left)
                                    out=" "+out;
                                else
                                    app+=" ";
                                left=!left;
                            }
                        lineDet+=out+app;
                    }
                }
            }
            System.out.println("|"+(withDetail ? lineDet+"|" : ""));
        }
        System.out.print("+-----+-----+-----+");
        if(withDetail)
            System.out.print("     +---------------------------------+---------------------------------+---------------------------------+");
        System.out.println();
    }
    /**
     * A method to check if the solution of this program is valid.
     * I used this method to check, if the programs does no bullshit :) The program doesn't use this method by default
     * but I kept the function for me to check the output of the program.
     * @return A String-message for the console. I know, it's an ugly way...
     */
    public static String checkValidity(){
        boolean mult = false; // Indicating if the program solved the sudoku explicitly.
        for(int i=0; i<9; i++){
            for(int j=0; j<9; j++){
                if(field[i][j] == 0)
                    mult = true;
                for(int i2=0; i2<9; i2++)
                    if(field[i][j] == field[i2][j] && i!=i2 && field[i][j] != 0)
                        return "Duplication at ("+i+","+j+") and ("+i2+","+j+")";
                for(int j2=0; j2<9; j2++)
                    if(field[i][j] == field[i][j2] && j!=j2 && field[i][j] != 0)
                        return "Duplication at ("+i+","+j+") and ("+i+","+j2+")";
                for(int i2=i/3*3; i2<(i/3*3+3); i2++)
                    for(int j2=j/3*3; j2<(j/3*3+3); j2++)
                        if(field[i][j] == field[i2][j2] && (i!=i2 || j!=j2) && field[i][j] != 0)
                            return "Duplication at ("+i+","+j+") and ("+i2+","+j2+")";
            }
        }
        return mult ? "Underdetermined but valid" : "Valid!";
    }
    /**
     * This is where the fun begins.
     * The program iterates over the hole sudoku and invokes a fixField-Action (and there corresponding fieldReduced-Events) whenever
     * an explicit default number (the numbers defined by the input) was found in 'fields'.
     * @throws RuntimeException if a contradiction will be detected.
     */
    public static void solve() throws RuntimeException{
        for(int i=0; i<field.length; i++){
            for(int j=0; j<field[i].length; j++){
                if(field[i][j] != 0 &&              //Handle the values which are set from beginning as "fixer"
                        !possib[i][j][field[i][j]-1]){ //flag to identify if this value was set from beginning.
                                                      //booleans will be initialized in Java with 'false'.
                                                      //That's why the initially fixed numbers (by the input) won't have
                                                      //the value 'true' in the corresponding field in 'possib' in contrast
                                                      //to numbers fixed by this program.
                                                      //This is an optimization for not repeating the propagation from the programmatically
                                                      //fixed numbers on.
                    fieldFixed(i,j,field[i][j]);
                    for(int poss=0; poss<9; poss++)
                        if(poss+1 != field[i][j])
                            fieldReduced(i,j,field[i][j]);
                }
            }
        }
        if(chooseNum){ //In this section the program chooses "random" numbers if the flag is 'true' and the sudoku is unsolved.
                       //But the flag is by default 'false'.
            boolean solved = false;
            while(!solved){
                //print(true);
                boolean cont = false; // if an unspecified field is found, this variable will be set to 'true'.
                for(int i=0; i<9 && !cont; i++){
                    for(int j=0; j<9 && !cont; j++){
                        if(field[i][j] == 0)
                            for(int poss=0; poss<9 && !cont; poss++)
                                if(possib[i][j][poss]){
                                    fixField(i,j,(byte)(poss+1)); //choose "randomly" the first possibility for this field and invoke a fixField-Action.
                                    cont = true;
                                }
                    }
                }
                //print(true);
                if(!cont)
                    solved = true;
            }
        }
    }
    
    /**
     * The fixField-Action.
     * This method will be used if a field will be "forced" to be set to a specified number.
     * Here we need to check if this does not result in any contradictions.
     * Additionally all other possibilities which are left for this field become impossible.
     * Therefore this method may invokes a fieldFixed-Event and a set of fieldReduced-Events.
     * @param i row index
     * @param j column index
     * @param number fix this field to 'number'.
     */
    private static void fixField(int i, int j, byte number){
        field[i][j] = number;
        //start test for contradiction
        for(int i2=0; i2<9; i2++)
            if(field[i2][j] == number && i2 != i)
                throw new RuntimeException("Contradiction detected: Number "+number+" in column "+j+" was found twice or more.");
        
        for(int j2=0; j2<9; j2++)
            if(field[i][j2] == number && j2 != j)
                throw new RuntimeException("Contradiction detected: Number "+number+" in row "+i+" was found twice or more.");
        
        for(int i2=i/3*3; i2<(i/3*3+3); i2++)
            for(int j2=j/3*3; j2<(j/3*3+3); j2++)
                if(field[i2][j2] == number && (i2 != i || j2 != j))
                    throw new RuntimeException("Contradiction detected: Number "+number+" in box "+(i/3+1)+","+(j/3+1)+" was found twice or more.");
        //end test for contradiction
        fieldFixed(i,j,number);
        for(int poss=0; poss<9; poss++)
            if(possib[i][j][poss] && poss != number-1){
                possib[i][j][poss] = false;
                fieldReduced(i,j,(byte)(poss+1));
            }
    }
    /**
     * The reduceField-Action.
     * When a number "becomes" impossible for the field (i,j) we can check if there is only 1 possibility left
     * and we can set this field to a number.
     * According to that, this method may invokes a fieldFixed-Event and a fieldReduced-Event.
     * @param i row index
     * @param j column index
     * @param imposs reduce this field by 'imposs'.
     */
    private static void reduceField(int i, int j, byte imposs){
        possib[i][j][imposs-1] = false;
        byte newVal = -1;
        for(int poss=0; poss<9; poss++)
            if(possib[i][j][poss] && newVal == -1)
                newVal = (byte)(poss+1);
            else if(possib[i][j][poss]){
                newVal = 0;
                break;
            }
        //if(newVal == -1) //this will never be a case
        //    throw new RuntimeException("field to -1? i="+i+", j="+j);
        if(newVal != 0){
            field[i][j] = newVal;
            fieldFixed(i,j,newVal);
        }
        fieldReduced(i,j,imposs);
    }
    /**
     * The fieldFixed-Event.
     * When a field is fixed to a specified number, this number is impossible for all other fields in this box, row and column.
     * According to that, this method may invokes a set of reduceField-Actions.
     * @param i row index
     * @param j column index
     * @param number this field was fixed to the specified number.
     */
    private static void fieldFixed(int i, int j, byte number){
        for(int i2=0; i2<9; i2++)
            if(field[i2][j] == 0 && possib[i2][j][number-1])
                reduceField(i2,j,number);
        
        for(int j2=0; j2<9; j2++)
            if(field[i][j2] == 0 && possib[i][j2][number-1])
                reduceField(i,j2,number);
        
        for(int i2=i/3*3; i2<(i/3*3+3); i2++)
            for(int j2=j/3*3; j2<(j/3*3+3); j2++)
                if(field[i2][j2] == 0 && possib[i2][j2][number-1])
                    reduceField(i2,j2,number);
    }
    /**
     * The fieldReduced-Event.
     * First we need to check if (as an result of the reduction before) in this row, column and box is only
     * 1 possibility left for 'imposs'. This may result in some fixField-Actions.
     * Second there are some little tricks described below in the relevant code section.
     * @param i row index
     * @param j column index
     * @param imposs this field was reduced by 'imposs'.
     */
    private static void fieldReduced(int i, int j, byte imposs){
        // -1 = no possibility found
        // -2 = number as fixed value found
        // -3 = more than 1 possibility found
        int iPoss = -1;
        for(int i2=0; i2<9; i2++)
            if(field[i2][j] == imposs){
                iPoss = -2;
                break;
            }else if(field[i2][j] == 0 && possib[i2][j][imposs-1]){
                if(iPoss == -1)
                    iPoss = i2;
                else{
                    iPoss = -3;
                    break;
                }
            }
        if(iPoss == -1)
            throw new RuntimeException("Contradiction detected: No possibility found for number "+imposs+" in column "+j+".");
        if(iPoss >= 0)
            fixField(iPoss,j,imposs);
        
        int jPoss = -1;
        for(int j2=0; j2<9; j2++)
            if(field[i][j2] == imposs){
                jPoss = -2;
                break;
            }else if(field[i][j2] == 0 && possib[i][j2][imposs-1]){
                if(jPoss == -1)
                    jPoss = j2;
                else{
                    jPoss = -3;
                    break;
                }
            }
        if(jPoss == -1)
            throw new RuntimeException("Contradiction detected: No possibility found for number "+imposs+" in row "+i+".");
        if(jPoss >= 0)
            fixField(i,jPoss,imposs);
        
        //Here is the first little trick. Additionally to the test if there is only 1 possibility left in this box,
        //we can check for cases like that:
        //             j                               field (i,j) reduced by '1'
        //       +---------------------------------+
        //       |     7          8          9     |
        //  i    |    {23}       {23}       {23}   |
        //       |  {23456}    {123456}   {123456} |  <- Here we know that number '1' is now definetly in row i+1.
        //       +---------------------------------+     `=> We can reduce the possibilities in this row in the other boxes
        iPoss = jPoss = -1;
        for(int i2=i/3*3; i2<(i/3*3+3); i2++)
            for(int j2=j/3*3; j2<(j/3*3+3); j2++)
                if(field[i2][j2] == imposs){
                    iPoss = jPoss = -2;
                    i2 = j2 = 9;
                }else if(field[i2][j2] == 0 && possib[i2][j2][imposs-1]){
                    if(iPoss == -1)
                        iPoss = i2;
                    else if(iPoss != i2) // more than one possibility found which are not in the same row
                        iPoss = -3;
                    
                    if(jPoss == -1)
                        jPoss = j2;
                    else if(jPoss != j2) // more than one possibility found which are not in the same column
                        jPoss = -3;
                }
        if(iPoss == -1)
            throw new RuntimeException("Contradiction detected: No possibility found for number "+imposs+" in box "+(i/3+1)+","+(j/3+1)+".");
        if(iPoss >= 0 && jPoss >= 0)
            fixField(iPoss,jPoss,imposs);
        else if(iPoss >= 0){
            //reduce possibilities for row iPoss
            for(int j2=0; j2<9; j2++){
                if(j2/3 != j/3 && field[iPoss][j2] == 0 && possib[iPoss][j2][imposs-1])
                    reduceField(iPoss,j2,imposs);
            }
        }else if(jPoss >= 0){
            //reduce possibilities for column jPoss
            for(int i2=0; i2<9; i2++){
                if(i2/3 != i/3 && field[i2][jPoss] == 0 && possib[i2][jPoss][imposs-1])
                    reduceField(i2,jPoss,imposs);
            }
        }
        
        //Some more tricks:
        //case 1:                 j                   field (i,j) reduced by '4'
        //       +---------------------------------+
        //       |     7          8          9     |
        //   i   |   {123}      {123}      {123}   |  <- check for cases like this. In this case all other {123}-possibilities in this row and this box will be impossible.
        //       |  {123456}   {123456}   {123456} |
        //       +---------------------------------+
        //case 2:                            j        field (i,j) reduced by '4'
        //       +---------------------------------+
        //       |     7          8          9     |
        //   i   |    {13}        2         {13}   |  <- or something similar like this...
        //       |  {123456}   {123456}   {123456} |
        //       +---------------------------------+
        
        //First, check this situation on this row
        byte p1 = -1, p2 = -1, p3 = -1, count0 = 0;
        for(int j2=j/3*3; j2<(j/3*3+3); j2++){
            if(field[i][j2] == 0){
                count0++;
                for(int poss=0; poss<9; poss++)
                    if(possib[i][j2][poss])
                        if(p1 == -1)
                            p1 = (byte)(poss+1);
                        else if(p2 == -1)
                            p2 = (byte)(poss+1);
                        else if(p3 == -1)
                            p3 = (byte)(poss+1);
                        else if(p1 != poss+1 && p2 != poss+1 && p3 != poss+1){
                            p1 = p2 = p3 = -3;
                            j2 = poss = 9;
                        }
            }
        }
        if(p3 == -1 && count0 == 2 || p3 > 0 && count0 == 3){ //case 2 || case 1
            //reduce possibilities in this box
            for(int i2=i/3*3; i2<(i/3*3+3); i2++)
                for(int j2=j/3*3; j2<(j/3*3+3); j2++)
                    if(i2 != i && field[i2][j2] == 0){
                        if(possib[i2][j2][p1-1])
                            reduceField(i2,j2,p1);
                        if(possib[i2][j2][p2-1])
                            reduceField(i2,j2,p2);
                        if(p3 != -1 && possib[i2][j2][p3-1])
                            reduceField(i2,j2,p3);
                    }
            //reduce possibilities in this row
            for(int j2=0; j2<9; j2++)
                if(j2/3 != j/3 && field[i][j2] == 0){
                    if(possib[i][j2][p1-1])
                        reduceField(i,j2,p1);
                    if(possib[i][j2][p2-1])
                        reduceField(i,j2,p2);
                    if(p3 != -1 && possib[i][j2][p3-1])
                        reduceField(i,j2,p3);
                }
        }
        //Second, check for the same problem in this column
        p1 = -1; p2 = -1; p3 = -1; count0 = 0;
        for(int i2=i/3*3; i2<(i/3*3+3); i2++){
            if(field[i2][j] == 0){
                count0++;
                for(int poss=0; poss<9; poss++)
                    if(possib[i2][j][poss])
                        if(p1 == -1)
                            p1 = (byte)(poss+1);
                        else if(p2 == -1)
                            p2 = (byte)(poss+1);
                        else if(p3 == -1)
                            p3 = (byte)(poss+1);
                        else if(p1 != poss+1 && p2 != poss+1 && p3 != poss+1){
                            p1 = p2 = p3 = -3;
                            i2 = poss = 9;
                        }
            }
        }
        if(p3 == -1 && count0 == 2 || p3 > 0 && count0 == 3){ //case 2 || case 1
            //reduce possibilities in this box
            for(int i2=i/3*3; i2<(i/3*3+3); i2++)
                for(int j2=j/3*3; j2<(j/3*3+3); j2++)
                    if(j2 != j && field[i2][j2] == 0){
                        if(possib[i2][j2][p1-1])
                            reduceField(i2,j2,p1);
                        if(possib[i2][j2][p2-1])
                            reduceField(i2,j2,p2);
                        if(p3 != -1 && possib[i2][j2][p3-1])
                            reduceField(i2,j2,p3);
                    }
            //reduce possibilities in this column
            for(int i2=0; i2<9; i2++)
                if(i2/3 != i/3 && field[i2][j] == 0){
                    if(possib[i2][j][p1-1])
                        reduceField(i2,j,p1);
                    if(possib[i2][j][p2-1])
                        reduceField(i2,j,p2);
                    if(p3 != -1 && possib[i2][j][p3-1])
                        reduceField(i2,j,p3);
                }
        }
    }
}
