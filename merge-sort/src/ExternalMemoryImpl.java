
import java.io.*;
import java.util.Arrays;

public class ExternalMemoryImpl implements IExternalMemory {

    /**
     * Inner Class defines Line object
     */
    private class Line implements Comparable<Line> {

        /**
         * Column Length as defined
         */
        private static final int COLUMN_LENGTH = 20;

        /**
         * wholeLine String
         */
        private String wholeLine;

        /**
         * comparable column String
         */
        private String columnStr;

        /**
         * creating Line object
         * @param wholeLine String all line
         * @param numOfColumn int, number of column to sort by
         */
        private Line(String wholeLine, int numOfColumn){
            if(wholeLine == null){
                columnStr = null;
                return;
            }
            this.wholeLine = wholeLine;
            int start = (numOfColumn - 1) * (COLUMN_LENGTH + 1);
            columnStr = wholeLine.substring(start, start + COLUMN_LENGTH);
        }

        /**
         * @return String, line
         */
        private String getLine(){
            return wholeLine;
        }

        /**
         * line has been written, object terminated
         */
        private void killLine(){
            columnStr = null;
            wholeLine = null;
        }

        /**
         * implements comparator
         * @param other Line object
         * @return int
         */
        @Override
        public int compareTo(Line other) {
            if(columnStr == null && other.columnStr == null){
                return 0;
            }
            if(columnStr == null){
                return 1;
            }
            if(other.columnStr == null){
                return -1;
            }
            return columnStr.compareTo(other.columnStr);
        }
    }


    /**
     * number of temp files in tempPath
     */
    private int numTemp;
    private int depthTemp = 0;

    /**
     * column to sort by
     */
    private int column;

    /**
     * column and String to filter by
     */
    private String filterString;
    private int filterColumn;

    /**
     * number of blocks available in the RAM
     */
    private static final int M = 12500;
    private static final int numOfBlocksInRam = M;

    /**
     * Block Size
     */
    private static final int Y = 500;
    private static final int blockSize = Y;
    private static final long RAM = 19500000;


    // =======  Run time const  ========= //

//    /**
//     * L - num of input lines
//     */
//    private int numberOfInputLines = 0;

    /**
     * X number of bytes in line
     */
    private int numOfBytesInLine;

    // ======= dependable ====== /

    // floor(Y/X)
    private int numOfLinesInBlock;
    // LX
//    private int byteSizeOfData;
//     ceil(LX/Y)
//    private int blockNumOfData;
    // M*floor(Y/X)
    private int numOfLinesInRam;
    // ceil(XL/YM) number of tiles we need to merge in the beginning of the second process of the sorting
//    private int numOfTiles;


    /**
     * temp files name
     */
    private static final String fileName1 = "depth";
    private static final String fileName2 = "temp";
    private static final String fileName3 = ".txt";


    public static void main(String[] args){
        long start = System.nanoTime();
        String fileIn = "newfile.txt";
        String fileOut = "output.txt";
        String tmpPath = "C:\\Users\\USER\\Documents\\my_java\\Ex3DB\\src\\temp\\";
        ExternalMemoryImpl mySort = new ExternalMemoryImpl();
        mySort.sort(fileIn, fileOut, 2, tmpPath);
        long end = System.nanoTime();
        System.out.println((end-start)/Math.pow(10,9));
    }

    /**
     *
     * @param in String, given File name
     * @param out String, file name to write to
     * @param colNumSelect column to filter by
     * @param substrSelect String to filter by
     * @param tmpPath String, path to write temp files to
     */
    @Override
    public void select(String in, String out, int colNumSelect,
                       String substrSelect, String tmpPath){
        try {
            BufferedReader myReader = new BufferedReader(new FileReader(in));
            BufferedWriter myWriter = new BufferedWriter(new FileWriter(out));
            String line;
            while((line = myReader.readLine()) != null){
                if(getColumn(line, colNumSelect).contains(substrSelect)){
                    myWriter.write(line + '\n');
                }
            }
            myReader.close();
            myWriter.close();
        }
        catch (IOException exception){
            System.out.println("EXCEPTION");
        }
    }

    /**
     * first filtering data during first time temp files creating, then sorting it
     * @param in String, given File name
     * @param out String, file name to write to
     * @param colNumSort column to sort by
     * @param tmpPath String, path to write temp files to
     * @param colNumSelect column to filter by
     * @param substrSelect String to filter by
     */
    @Override
    public void sortAndSelectEfficiently(String in, String out, int colNumSort,
                                         String tmpPath, int colNumSelect, String substrSelect) {
        filterString = substrSelect;
        filterColumn = colNumSelect;
        fullSort(in, out, colNumSort, tmpPath, true);
    }

    /**
     * main function, merge sort given file
     * @param in String, given File name
     * @param out String, file name to write to
     * @param colNum column to sort by
     * @param tmpPath String, path to write temp files to
     */
    @Override
    public void sort(String in, String out, int colNum, String tmpPath){
        fullSort(in, out, colNum, tmpPath, false);
    }

    private void fullSort(String input, String output, int column, String tempPath, boolean flag){
        try {
            setConst(input, column);
            setTempFiles(input, tempPath, column, flag);
            while(!mergeSort(tempPath, output)){
                depthTemp++;
            }
        }
        catch (IOException exception){
            System.out.println("EXCEPTION");
        }
    }

    /**
     * returns column String given number of column
     * @param line full line to get string from
     * @param column int, num of column
     * @return String, full column String
     */
    private String getColumn(String line, int column){
        int start = (column - 1) * (Line.COLUMN_LENGTH + 1);
        return line.substring(start, start + Line.COLUMN_LENGTH);
    }

    /**
     * set program constants
     * @param fileName String, name of file
     * @param column int, column to sort by
     */
    private void setConst(String fileName, int column) throws IOException{
        String line;
        BufferedReader tempBuff = new BufferedReader(new FileReader(fileName));
        line = tempBuff.readLine();
        numOfBytesInLine = line.length() * 2;
        tempBuff.close();
        this.column = column;
        numOfLinesInBlock = (int)Math.ceil(blockSize/numOfBytesInLine);
        numOfLinesInRam = (int)(RAM / numOfBytesInLine);
    }

    /**
     * sets sorted temp files
     * @param fileIn String, name of input file
     * @param tmpPath String, name of directory to add temp files to
     * @param column int, index of column to determine sorting by
     */
    private void setTempFiles(String fileIn, String tmpPath, int column, boolean flag) throws IOException{
        File inFile = new File(fileIn);
        FileReader inFileReader = new FileReader(inFile);
        BufferedReader myBuffer = new BufferedReader(inFileReader);
        String line;
        Line[] myLines = new Line[numOfLinesInRam];
        int numOfLines = 0;
        String tempName;
        while((line = myBuffer.readLine()) != null){
            if(flag && !getColumn(line, filterColumn).contains(filterString)){
                continue;
            }
            if(numOfLines == numOfLinesInRam){
                Arrays.sort(myLines);
                tempName = tmpPath + fileName1 + depthTemp + fileName2 + numTemp + fileName3;
                writeLines(myLines, tempName);
                numOfLines = 0;
                numTemp++;
                myLines = new Line[numOfLinesInRam];
            }
            myLines[numOfLines] = new Line(line, column);
            numOfLines++;
        }
        fixLines(myLines);
        Arrays.sort(myLines);
        tempName = tmpPath + fileName1 + depthTemp + fileName2 + numTemp + fileName3;
        writeLines(myLines, tempName);
        depthTemp++;
        numTemp++;
        myBuffer.close();
    }

    /**
     * adds null Lines into given array
     * @param lines fixed array
     */
    private void fixLines(Line[] lines){
        for(int i = 0; i < lines.length; i++){
            if(lines[i] == null){
                lines[i] = new Line(null, column);
            }
        }
    }

    /**
     * merging one level of files
     * @param tmpPath temp path to find old files
     * @param outFile String, name of out file
     * @return true if all files merged into one file, false otherwise
     * @throws IOException in case of unable to read file
     */
    private boolean mergeSort(String tmpPath, String outFile) throws IOException{
        String mergedName;
        String tempName;
        int newTempNum = 0, numOfTempRead = 0;
        Line[] myLines  = new Line[numTemp];
        while(numOfTempRead < numTemp)
        {
            mergedName = tmpPath + fileName1 + depthTemp + fileName2 + newTempNum + fileName3;
            if(numTemp < numOfLinesInRam){
                mergedName = outFile;
            }
            BufferedWriter myWriter = new BufferedWriter(new FileWriter(mergedName));
            BufferedReader[] myReaders = new BufferedReader[numOfLinesInRam - 1];
            for (int i = 0; i < (numOfLinesInRam - 1); i++)
            {
                if(numOfTempRead >= numTemp){
                    break;
                }
                tempName = tmpPath + fileName1 + (depthTemp-1) + fileName2 + numOfTempRead + fileName3;
                File tempFile = new File(tempName);
                tempFile.deleteOnExit();
                FileReader tempFileReader = new FileReader(tempFile);
                myReaders[i] = new BufferedReader(tempFileReader);
                myLines[i] = getLine(myReaders[i]);
                numOfTempRead++;
            }
            fixLines(myLines);
            while(notFinished(myLines))
            {
                myWriter.write( getMinLine(myReaders, myLines) + '\n');
            }
            myWriter.close();
            newTempNum++;
        }
        numTemp = newTempNum;
        return numTemp == 1;
    }

//    /**
//     * creates BufferReader
//     * @param mergedName
//     * @return
//     * @throws IOException
//     */
//    private BufferedReader getBufferReader(String mergedName) throws IOException{
//	    return new BufferedReader(new FileReader(mergedName));
//    }

    /**
     * get Line object while making sure Buffered reader and line are not null
     * @param reader BufferedReader to read from
     * @return Line object
     * @throws IOException if cant read line
     */
    private Line getLine(BufferedReader reader) throws IOException {
        String line;
        if (reader == null || (line = reader.readLine()) == null) {
            return null;
        }
        return new Line(line, column);
    }

    /**
     * finds the minimum line in Line array, and update the Line array to the next line
     * @param myReaders BufferedReader Array
     * @param lines Lines array
     * @return line to write in the output file
     * @throws IOException in case of unable to read file
     */
    private String getMinLine(BufferedReader[] myReaders, Line[] lines) throws IOException{
        int minIndex = 0;
        for(int i = 0; i < lines.length - 1; i++){
            minIndex = getMin(minIndex, i+1, lines);
        }
        String oldLine = lines[minIndex].getLine();
        String line;
        if((line = myReaders[minIndex].readLine()) != null){
            lines[minIndex] = new Line(line, column);
        }
        else{
            lines[minIndex].killLine();
            myReaders[minIndex] = null;
        }
        return oldLine;
    }

    /**
     * return the minimum line from 2 given lines
     * @param line1 index of line 1
     * @param line2 index of line 2
     * @param lines Line array
     * @return index of minimum line
     */
    private int getMin(int line1, int line2, Line[] lines){
        if(lines[line1] == null){
            return line2;
        }
        if(lines[line2] == null){
            return line1;
        }
        if(lines[line1].compareTo(lines[line2]) > 0){
            return line2;
        }
        return line1;
    }

    /**
     * check if we finished all given buffers
     * @param myLines Line array
     * @return boolean, true for unfinished array false otherwise
     */
    private boolean notFinished(Line[] myLines){
        for(Line line : myLines){
            if(line.getLine() != null){
                return true;
            }
        }
        return false;
    }


    /**
     * writing lines into file given Line[] and file name
     * @param myLines Line array
     * @param fileName temp file name
     * @throws IOException if file can not be written to
     */
    private void writeLines(Line[] myLines, String fileName) throws IOException{
        BufferedWriter myWriter = new BufferedWriter(new FileWriter(fileName));
        for(int i = 0; i < myLines.length; i++){
            if (myLines[i].getLine() == null){
                continue;
            }
            myWriter.write(myLines[i].getLine() + '\n');
            myLines[i] = null;
        }
        myWriter.close();
    }




}