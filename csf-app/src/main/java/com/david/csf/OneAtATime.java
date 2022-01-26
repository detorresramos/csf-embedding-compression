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

import javax.swing.plaf.synth.SynthSplitPaneUI;

import it.unimi.dsi.bits.TransformationStrategies;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.sux4j.mph.GV3CompressedFunction;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

public class OneAtATime {
    static ArrayList<Long> readKeys(String filename) {
        ArrayList<Long> keys = new ArrayList<>();

        try {
            File file = new File(filename);
            BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));

            int i = 0;
            String input = null;
            while ((input = reader.readLine()) != null) {
                keys.add(i, Long.parseLong(input));
                i++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return keys;
    }


    private static Long[] readQuantizedAtIthChunk(String filename, int N, int startIdx) {
        Long csfCentroidIndices[];

        try {
            File file = new File(filename);
            BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));

            String input = null;
            input = reader.readLine();
            String[] splited = input.split("\\s+");
            int i = 0;
            csfCentroidIndices = new Long[N];
            while (input != null) {
                csfCentroidIndices[i] = Long.parseLong(splited[startIdx]);
                input = reader.readLine();
                if (input != null)
                    splited = input.split("\\s+");
                i++;
            }
            reader.close();
            return csfCentroidIndices;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    private static GV3CompressedFunction<Long> buildSingleCsf(ArrayList<Long> keys,
            Long[] csfCentroidIndices, int numCsfs) {
        Iterable<Long> keysIterable = keys;

        // values for CSFs
        final Iterator<Long> iterator = Arrays.asList(csfCentroidIndices).iterator();
        LongIterable centroidIndicesIterable = new LongIterable() {

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
        };

        try {
            GV3CompressedFunction.Builder<Long> csf = new GV3CompressedFunction.Builder<>();
            csf.keys(keysIterable);
            csf.values(centroidIndicesIterable);
            csf.transform(TransformationStrategies.fixedLong());
            return csf.build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static void outputResults(ArrayList<GV3CompressedFunction<Long>> csfArray, ArrayList<Long> keys, int numChunks,
            String outputFilename) {

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

            Long N = Long.valueOf(keys.size());

            myWriter.write("Total keys = " + N);

            myWriter.write("\n\nTesting memory: \n");

            Long bytesForKeys = 0L;
            for (int i = 0; i < N; i++) {
                bytesForKeys += keys.get(i) * 8; // 8 bytes for long
            }
            bytesForKeys += N * 2; // add 2 bytes per key. one for pointer to key and one for null terminator
            Long bytesForCentroidEmbeddings = N * numChunks; // 1 byte integers
            Long bytesForHashtable = bytesForKeys + bytesForCentroidEmbeddings;

            // total size of csf array in bits
            Long bytesForCsfArray = 0L;
            for (int i = 0; i < numChunks; i++) {
                bytesForCsfArray += (int) Math.round(csfArray.get(i).numBits() / 8.0);
            }

            double bitsPerElem = (bytesForCsfArray * 8.0) / (numChunks * N);

            // total compression
            float totalCompression = (float) bytesForHashtable / (float) bytesForCsfArray;

            myWriter.write("\nBytes embedding keys = ~" + Long.toString(bytesForKeys));
            myWriter.write("\nBytes for centroid embeddings = ~" + Long.toString(bytesForCentroidEmbeddings));
            myWriter.write("\nBytes for Java Hashtable = ~" + Long.toString(bytesForHashtable));
            myWriter.write("\nBytes for CSF Array = ~" + Long.toString(bytesForCsfArray));
            myWriter.write("\nBits per elem = ~" + Double.toString(bitsPerElem));
            myWriter.write("\nTotal Compression = ~" + Float.toString(totalCompression));

            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred in writing to the file.");
            e.printStackTrace();
        }
    }


    private static void dumpCsfs(ArrayList<GV3CompressedFunction<Long>> csfArray, int M, int numChunks, String datasetName) {

        for (int i = 0; i < numChunks; i++) {
            try {
                System.out.println("DUMPING " + Integer.toString(i));
                csfArray.get(i).dump("data/"+ datasetName + "/testing/testing_M" + Integer.toString(M) + "/dump/csf" + Integer.toString(i));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String args[]) {
        String datasetName = args[0];
        int M = Integer.parseInt(args[2]);
        int numChunks = Integer.parseInt(args[3]);
        String inputDirectory = "data/" + datasetName + "/testing/testing_M" + Integer.toString(M);
        String outputFilename = "data/" + datasetName + "/testing/testing_M" + Integer.toString(M) + "/results_k256_M" + Integer.toString(M) + ".txt";
        String keysFilename = inputDirectory + "/keys.txt";
        String quantizedVectorsFilename = inputDirectory + "/quantized.txt";
        ArrayList<Long> byteKeys = readKeys(keysFilename);

        PrintStream dummyStream = new PrintStream(new OutputStream() {
            public void write(int b) {
                // NO-OP
            }
        });

        System.setOut(dummyStream);

        ArrayList<GV3CompressedFunction<Long>> csfArray = new ArrayList<>();
        for (int i = 0; i < numChunks; i++) {
            Long[] quantizedIndices = readQuantizedAtIthChunk(quantizedVectorsFilename, byteKeys.size(), i);
            GV3CompressedFunction<Long> csf = buildSingleCsf(byteKeys, quantizedIndices, numChunks);
            csfArray.add(csf);
        }

        outputResults(csfArray, byteKeys, numChunks, outputFilename);

        dumpCsfs(csfArray, M, numChunks, datasetName);

    }
}
