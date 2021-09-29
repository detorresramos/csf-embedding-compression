package com.david.csf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    static ArrayList<GV3CompressedFunction<String>> buildCsfArray(ArrayList<String> keys,
            final Long[][] csfCentroidIndices, int numCsfs) {
        Iterable<String> keysIterable = keys;

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

        ArrayList<GV3CompressedFunction<String>> csfArray = new ArrayList<>();
        for (int i = 0; i < numCsfs; i++) {
            try {
                GV3CompressedFunction.Builder<String> csf = new GV3CompressedFunction.Builder<>();
                csf.keys(keysIterable);
                csf.values(centroidIndicesIterable.get(i));
                csf.transform(TransformationStrategies.rawUtf16()); // 8.76 bit cost per elem
                // all builtin transformation strategies have same bit cost per elem
                csfArray.add(csf.build());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ;
        return csfArray;
    }

    private static void outputResults(ArrayList<GV3CompressedFunction<String>> csfArray,
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

            myWriter.write("Total keys = " + keys.size());

            myWriter.write("\n\nTesting memory: \n");

            // TODO correct counting for memory of keys??? see ben email
            int bytesForKeys = 0;
            for (int i = 0; i < keys.size(); i++) {
                bytesForKeys += keys.get(i).length();
            }
            int bytesForCentroidEmbeddings = keys.size() * numChunks;
            int bytesForHashtable = bytesForKeys + bytesForCentroidEmbeddings;

            // total size of csf array in bits
            int bytesForCsfArray = 0;
            for (int i = 0; i < numChunks; i++) {
                bytesForCsfArray += (int) Math.round(csfArray.get(i).numBits() / 8.0);
            }

            double bitsPerElem = (bytesForCsfArray * 8.0) / (numChunks * keys.size());

            // total compression
            float totalCompression = (float) bytesForHashtable / (float) bytesForCsfArray;

            myWriter.write("\nBytes for Java Hashtable = ~" + Integer.toString(bytesForHashtable));
            myWriter.write("\nBytes for CSF Array = ~" + Integer.toString(bytesForCsfArray));
            myWriter.write("\nBits per elem = ~" + Double.toString(bitsPerElem));
            myWriter.write("\nTotal Compression = ~" + Float.toString(totalCompression));

            myWriter.write("\n\nTesting latency: \n");

            int numQueries = 100000;
            long csfTimes[] = new long[numQueries];
            long hashTableTimes[] = new long[numQueries];

            Random random = new Random();
            for (int i = 0; i < numQueries; i++) {
                String query = keys.get(random.nextInt(keys.size()));

                long startTime = System.nanoTime();
                for (int j = 0; j < numChunks; j++) {
                    csfArray.get(j).getLong(query);
                }
                // csfArray.get(0).getLong(query);
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);
                csfTimes[i] = duration;

                long startTime1 = System.nanoTime();
                wordsToEmbeddings.get(query);
                long endTime1 = System.nanoTime();
                long duration1 = (endTime1 - startTime1);
                hashTableTimes[i] = duration1;
            }

            Arrays.sort(csfTimes);
            Arrays.sort(hashTableTimes);

            // TODO mean latency instead of median
            myWriter.write("\nMedian latency for csf and hashTables");
            myWriter.write("\nCsf: " + Long.toString(csfTimes[50000]));
            myWriter.write("\nHashtable: " + Long.toString(hashTableTimes[50000]));

            myWriter.write("\nP99 latency for csf and hashTables");
            myWriter.write("\nCsf: " + Long.toString(csfTimes[99000]));
            myWriter.write("\nHashtable: " + Long.toString(hashTableTimes[99000]));

            myWriter.write("\nP99.9 latency for csf and hashTables");
            myWriter.write("\nCsf: " + Long.toString(csfTimes[99900]));
            myWriter.write("\nHashtable: " + Long.toString(hashTableTimes[99900]));

            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred in writing to the file.");
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        String inputDirectory = args[0];
        String outputFilename = args[1];
        int k = Integer.parseInt(args[2]);
        int M = Integer.parseInt(args[3]);

        String keysFilename = inputDirectory + "/keys.txt";
        String quantizedVectorsFilename = inputDirectory + "/quantized.txt";
        String codebooksFilename = inputDirectory + "/codebooks.txt";

        ArrayList<String> keys = readKeys(keysFilename);
        QuantizedResult result = readQuantized(quantizedVectorsFilename, keys.size());
        ArrayList<ArrayList<Integer>> quantized = result.getQuantizedVectors();
        int numChunks = result.getCsfCentroidIndices().length;
        Float codebooks[][][] = readCodebooks(codebooksFilename, k, M, numChunks);

        // build csfArray from keys to values
        ArrayList<GV3CompressedFunction<String>> csfArray = buildCsfArray(keys, result.getCsfCentroidIndices(),
                numChunks);

        // build standard java hash table for keys to values
        Hashtable<String, ArrayList<Integer>> wordsToEmbeddings = createEmbeddingHashtable(keys, quantized);

        outputResults(csfArray, wordsToEmbeddings, keys, numChunks, outputFilename);

    }
}
