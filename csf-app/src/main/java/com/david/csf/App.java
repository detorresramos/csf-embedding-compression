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

public class App {
    static ArrayList<String> readKeys(String filename) {
        ArrayList<String> keys = new ArrayList<>();

        try {
            File file = new File(filename);
            BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));

            int i = 0;
            String input = null;
            while ((input = reader.readLine()) != null) {
                keys.add(i, input);
                i++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return keys;
    }

    static class QuantizedResult {
        private final ArrayList<ArrayList<Integer>> first;
        private final Long[][] second;

        public QuantizedResult(ArrayList<ArrayList<Integer>> first, Long[][] second) {
            this.first = first;
            this.second = second;
        }

        public ArrayList<ArrayList<Integer>> getQuantizedVectors() {
            return first;
        }

        public Long[][] getCsfCentroidIndices() {
            return second;
        }
    }

    static QuantizedResult readQuantized(String filename, int N) {
        ArrayList<ArrayList<Integer>> quantizedVectors = new ArrayList<ArrayList<Integer>>(N);
        Long csfCentroidIndices[][];

        try {
            File file = new File(filename);
            BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));

            int i = 0;
            String input = null;
            input = reader.readLine();
            String[] splited = input.split("\\s+");
            csfCentroidIndices = new Long[splited.length][N];
            while (input != null) {

                ArrayList<Integer> vector = new ArrayList<Integer>();
                for (int j = 0; j < splited.length; j++) {
                    vector.add(j, Integer.parseInt(splited[j]));
                    csfCentroidIndices[j][i] = Long.parseLong(splited[j]);
                }
                quantizedVectors.add(i, vector);
                i++;
                input = reader.readLine();
                if (input != null)
                    splited = input.split("\\s+");
            }
            reader.close();
            return new QuantizedResult(quantizedVectors, csfCentroidIndices);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    static Float[][][] readCodebooks(String filename, int k, int M, int numCodebooks) {
        Float codebooks[][][] = new Float[numCodebooks][k][M];

        try {
            File file = new File(filename);
            BufferedReader reader = new BufferedReader(new FileReader(file));

            int bookNum = 0;
            String input = null;
            while ((input = reader.readLine()) != null) {
                String[] splited = input.split("\\s+");
                Float[][] centroids = new Float[k][M];
                int spilittedIndex = 0;
                int codebookIndex = 0;
                for (int j = 0; j < k; j++) {
                    Float[] centroid = new Float[M];
                    for (int centroidNum = 0; centroidNum < M; centroidNum++) {
                        centroid[centroidNum] = Float.parseFloat(splited[spilittedIndex]);
                        spilittedIndex++;
                    }
                    centroids[codebookIndex] = centroid;
                    codebookIndex++;
                }
                codebooks[bookNum] = centroids;
                bookNum++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return codebooks;
    }

    static Hashtable<String, ArrayList<Integer>> createEmbeddingHashtable(ArrayList<String> keys,
            ArrayList<ArrayList<Integer>> quantized) {
        Hashtable<String, ArrayList<Integer>> wordsToEmbeddings = new Hashtable<String, ArrayList<Integer>>(
                (int) Math.round(keys.size() * 1.3));
        for (int i = 0; i < keys.size(); i++) {
            wordsToEmbeddings.put(keys.get(i), quantized.get(i));
        }
        return wordsToEmbeddings;
    }

    static ArrayList<GV3CompressedFunction<byte[]>> buildCsfArray(ArrayList<byte[]> keys,
            final Long[][] csfCentroidIndices, int numCsfs) {
        Iterable<byte[]> keysIterable = keys;

        // values for CSFs
        ArrayList<LongIterable> centroidIndicesIterable = new ArrayList<>();
        for (int i = 0; i < numCsfs; i++) {
            final int ii = i;
            final Iterator<Long> iterator = Arrays.asList(csfCentroidIndices[ii]).iterator();
            centroidIndicesIterable.add(new LongIterable() {

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
        for (int i = 0; i < numCsfs; i++) {
            try {
                GV3CompressedFunction.Builder<byte[]> csf = new GV3CompressedFunction.Builder<>();
                csf.keys(keysIterable);
                csf.values(centroidIndicesIterable.get(i));
                csf.transform(TransformationStrategies.rawByteArray());
                csfArray.add(csf.build());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ;
        return csfArray;
    }

    private static void outputResults(ArrayList<GV3CompressedFunction<byte[]>> csfArray,
            Hashtable<String, ArrayList<Integer>> wordsToEmbeddings, ArrayList<String> keys, int numChunks,
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

            int N = keys.size();

            myWriter.write("Total keys = " + N);

            myWriter.write("\n\nTesting memory: \n");

            int bytesForKeys = 0;
            for (int i = 0; i < N; i++) {
                bytesForKeys += keys.get(i).length();
            }
            bytesForKeys += N * 2; // add 2 bytes per key. one for pointer to key and one for null terminator
            int bytesForCentroidEmbeddings = N * numChunks; // 1 byte integers
            int bytesForHashtable = bytesForKeys + bytesForCentroidEmbeddings;

            // total size of csf array in bits
            int bytesForCsfArray = 0;
            for (int i = 0; i < numChunks; i++) {
                bytesForCsfArray += (int) Math.round(csfArray.get(i).numBits() / 8.0);
            }

            double bitsPerElem = (bytesForCsfArray * 8.0) / (numChunks * N);

            // total compression
            float totalCompression = (float) bytesForHashtable / (float) bytesForCsfArray;

            myWriter.write("\nBytes embedding keys = ~" + Integer.toString(bytesForKeys));
            myWriter.write("\nBytes for centroid embeddings = ~" + Integer.toString(bytesForCentroidEmbeddings));
            myWriter.write("\nBytes for Java Hashtable = ~" + Integer.toString(bytesForHashtable));
            myWriter.write("\nBytes for CSF Array = ~" + Integer.toString(bytesForCsfArray));
            myWriter.write("\nBits per elem = ~" + Double.toString(bitsPerElem));
            myWriter.write("\nTotal Compression = ~" + Float.toString(totalCompression));

            myWriter.write("\n\nTesting latency: \n");

            int numQueries = 100000;
            long csfTimes[] = new long[numQueries];
            long csfSingleTimes[] = new long[numQueries];
            long hashTableTimes[] = new long[numQueries];

            Random random = new Random();
            for (int i = 0; i < numQueries; i++) {
                String query = keys.get(random.nextInt(keys.size()));

                long startTime = System.nanoTime();
                for (int j = 0; j < numChunks; j++) {
                    csfArray.get(j).getLong(query);
                }
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);
                csfTimes[i] = duration;

                long startTime0 = System.nanoTime();
                csfArray.get(0).getLong(query);
                long endTime0 = System.nanoTime();
                long duration0 = (endTime0 - startTime0);
                csfSingleTimes[i] = duration0;

                long startTime1 = System.nanoTime();
                wordsToEmbeddings.get(query);
                long endTime1 = System.nanoTime();
                long duration1 = (endTime1 - startTime1);
                hashTableTimes[i] = duration1;
            }

            Arrays.sort(csfTimes);
            Arrays.sort(csfSingleTimes);
            Arrays.sort(hashTableTimes);

            myWriter.write("\nMedian, P99, P99.9 Latency for:");
            myWriter.write(String.format("\nGetting the entire centroid vector from CSF       : %s, %s, %s",
                    Long.toString(csfTimes[50000]), Long.toString(csfTimes[99000]), Long.toString(csfTimes[99900])));
            myWriter.write(String.format("\nGetting one centroid id from CSF                  : %s, %s, %s",
                    Long.toString(csfSingleTimes[50000]), Long.toString(csfSingleTimes[99000]),
                    Long.toString(csfSingleTimes[99900])));
            myWriter.write(String.format("\nGetting entire centroid vector from Java Hashtable: %s, %s, %s",
                    Long.toString(hashTableTimes[50000]), Long.toString(hashTableTimes[99000]),
                    Long.toString(hashTableTimes[99900])));

            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred in writing to the file.");
            e.printStackTrace();
        }
    }

    private static void dumpCsfs(ArrayList<GV3CompressedFunction<byte[]>> csfArray, int M) {

        for (int i = 0; i < M; i++) {
            try {
                System.out.println("DUMPING " + Integer.toString(i));
                csfArray.get(i).dump("data/ABCHeadlines_freq/testing/testing_M" + Integer.toString(M) + "/dump/csf" + Integer.toString(i));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        String inputDirectory = args[0];
        String outputFilename = args[1];
        // int k = Integer.parseInt(args[2]);
        int M = Integer.parseInt(args[3]);

        // PrintStream dummyStream = new PrintStream(new OutputStream() {
        //     public void write(int b) {
        //         // NO-OP
        //     }
        // });

        // System.setOut(dummyStream);

        String keysFilename = inputDirectory + "/keys.txt";
        String quantizedVectorsFilename = inputDirectory + "/quantized.txt";
        // String codebooksFilename = inputDirectory + "/codebooks.txt";

        ArrayList<String> keys = readKeys(keysFilename);
        QuantizedResult result = readQuantized(quantizedVectorsFilename, keys.size());
        ArrayList<ArrayList<Integer>> quantized = result.getQuantizedVectors();
        int numChunks = result.getCsfCentroidIndices().length;
        // Float codebooks[][][] = readCodebooks(codebooksFilename, k, M, numChunks);

        // build csfArray from keys to values
        ArrayList<byte[]> byteKeys = new ArrayList<>();
        for (String key : keys) {
            byteKeys.add(key.getBytes());
        }
        ArrayList<GV3CompressedFunction<byte[]>> csfArray = buildCsfArray(byteKeys, result.getCsfCentroidIndices(),
                numChunks);

        // build standard java hash table for keys to values
        Hashtable<String, ArrayList<Integer>> wordsToEmbeddings = createEmbeddingHashtable(keys, quantized);

        // outputResults(csfArray, wordsToEmbeddings, keys, numChunks, outputFilename);

        dumpCsfs(csfArray, M);

    }
}
