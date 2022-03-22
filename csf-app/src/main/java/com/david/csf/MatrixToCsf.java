package com.david.csf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;

import it.unimi.dsi.bits.TransformationStrategies;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.sux4j.mph.GV3CompressedFunction;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

public class MatrixToCsf {
    static class MatrixResult {
        public ArrayList<String> keys;
        public Long[][] matrix;

        public MatrixResult(ArrayList<String> keys, Long[][] matrix) {
            this.keys = keys;
            this.matrix = matrix;
        }
    }

    static MatrixResult readMatrix(String filename, int numRows) {
        ArrayList<String> keys = new ArrayList<>(numRows);
        try {
            File file = new File(filename);
            BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));

            int row = 0;
            String input = reader.readLine();
            String[] splited = input.split("\\s+");
            Long matrix[][] = new Long[splited.length][numRows];
            while (input != null) {
                for (int col = 0; col < splited.length; col++) {
                    matrix[col][row] = Long.parseLong(splited[col]);
                }
                keys.add(Integer.toString(row));
                row++;
                input = reader.readLine();
                if (input != null)
                    splited = input.split("\\s+");
            }
            reader.close();
            return new MatrixResult(keys, matrix);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ArrayList<GV3CompressedFunction<byte[]>> buildCsfArray(ArrayList<byte[]> byteKeys, Long[][] matrix) {
        Iterable<byte[]> keysIterable = byteKeys;
        int numColumns = matrix.length;

        ArrayList<LongIterable> columnsIterable = new ArrayList<>();
        for (int i = 0; i < numColumns; i++) {
            final int ii = i;
            final Iterator<Long> iterator = Arrays.asList(matrix[ii]).iterator();
            columnsIterable.add(new LongIterable() {

                @Override
                public LongIterator iterator() {
                    return new LongIterator() {

                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public long nextLong() {
                            return iterator.next();
                        }

                        @Override
                        public void forEachRemaining(java.util.function.LongConsumer action) {
                        }

                        @Override
                        public Long next() {
                            return iterator.next();
                        }
                    };
                }
            });
        }

        ArrayList<GV3CompressedFunction<byte[]>> csfArray = new ArrayList<>();
        for (int i = 0; i < numColumns; i++) {
            try {
                GV3CompressedFunction.Builder<byte[]> csf = new GV3CompressedFunction.Builder<>();
                csf.keys(keysIterable);
                csf.values(columnsIterable.get(i));
                csf.transform(TransformationStrategies.rawByteArray());
                csfArray.add(csf.build());
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        return csfArray;
    }

    private static void outputResults(ArrayList<GV3CompressedFunction<byte[]>> csfArray, MatrixResult result,
    String outputFilename) {


        int numColumns = result.matrix.length;
        try {
            File file = new File(outputFilename);
            if (file.createNewFile()) {
                System.out.println("Created output file");
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(outputFilename);

            int N = result.keys.size();

            myWriter.write("N: " + Integer.toString(N));
            myWriter.write("\n");
            myWriter.write("NumColumns: " + Integer.toString(numColumns));
            myWriter.write("\n");

            myWriter.write("Total keys = " + N);

            myWriter.write("\n\nTesting memory: \n");

            int bytesForKeys = 0;
            for (int i = 0; i < N; i++) {
                bytesForKeys += result.keys.get(i).length();
            }
            bytesForKeys += N * 2; // add 2 bytes per key. one for pointer to key and one for null terminator
            int bytesForCentroidEmbeddings = N * numColumns * 8; // 8 byte long
            int bytesForHashtable = bytesForKeys + bytesForCentroidEmbeddings;

            myWriter.write("CSF Size / N: ");
            // total size of csf array in bits
            double bytesForCsfArray = 0;
            for (int i = 0; i < numColumns; i++) {
                double bytesForCsf = csfArray.get(i).numBits() / 8.0;
                bytesForCsfArray += bytesForCsf;
                myWriter.write(Double.toString(bytesForCsf / N) + ", ");
            }
            myWriter.write("\nFeatureId: ");
            for (int i = 0; i < numColumns; i++) {
                myWriter.write(Integer.toString(i) + ", ");
            }

            double bitsPerElem = (bytesForCsfArray * 8.0) / (numColumns * N);

            float totalCompression = (float) bytesForHashtable / (float) bytesForCsfArray;

            myWriter.write("\nBytes embedding keys = ~" + Integer.toString(bytesForKeys));
            myWriter.write("\nBytes for centroid embeddings = ~" + Integer.toString(bytesForCentroidEmbeddings));
            myWriter.write("\nBytes for Java Hashtable = ~" + Integer.toString(bytesForHashtable));
            myWriter.write("\nBytes for CSF Array = ~" + Double.toString(bytesForCsfArray));
            myWriter.write("\nBits per elem = ~" + Double.toString(bitsPerElem));
            myWriter.write("\nTotal Compression = ~" + Float.toString(totalCompression));

            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred in writing to the file.");
            e.printStackTrace();
        }
    }

    private static void dumpCsfs(ArrayList<GV3CompressedFunction<byte[]>> csfArray, String dumpBaseName) {

        for (int i = 0; i < csfArray.size(); i++) {
            try {
                System.out.println("DUMPING " + Integer.toString(i));
                csfArray.get(i).dump(dumpBaseName + "/dump/csf" + Integer.toString(i));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String args[]) {
        String inputFilename = args[0];
        int numRows = Integer.parseInt(args[1]);
        String outputFilename = args[2];
        String dumpBaseName = args[3];

        MatrixResult result = readMatrix(inputFilename, numRows);

        // build csfArray from keys to values
        ArrayList<byte[]> byteKeys = new ArrayList<>();
        for (String key : result.keys) {
            byteKeys.add(key.getBytes());
        }

        PrintStream dummyStream = new PrintStream(new OutputStream() {
            public void write(int b) {
                // NO-OP
            }
        });

        System.setOut(dummyStream);

        ArrayList<GV3CompressedFunction<byte[]>> csfArray = buildCsfArray(byteKeys, result.matrix);

        outputResults(csfArray, result, outputFilename);

        System.setOut(System.out);

        dumpCsfs(csfArray, dumpBaseName);
    }
}
