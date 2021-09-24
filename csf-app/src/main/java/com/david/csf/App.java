package com.david.csf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

/**
 * Hello world!
 *
 */
public class App {
    static String[] readKeys() {
        String keys[] = new String[100000];

        try {
            File file = new File("data/word2vec/keys.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));

            int i = 0;
            String input = null;
            while ((input = reader.readLine()) != null) {
                keys[i] = input;
                i++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return keys;
    }

    static class QuantizedResult {
        private final Integer[][] first;
        private final Long[][] second;

        public QuantizedResult(Integer[][] first, Long[][] second) {
            this.first = first;
            this.second = second;
        }

        public Integer[][] getQuantizedVectors() {
            return first;
        }

        public Long[][] getCsfCentroidIndices() {
            return second;
        }
    }

    static QuantizedResult readQuantized() {
        Integer quantizedVectors[][] = new Integer[100000][5];

        Long csfCentroidIndies[][] = new Long[5][100000];

        try {
            File file = new File("data/word2vec/quantized.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));

            int i = 0;
            String input = null;

            while ((input = reader.readLine()) != null) {
                String[] splited = input.split("\\s+");
                Integer[] vector = new Integer[5];
                for (int j = 0; j < splited.length; j++) {
                    vector[j] = Integer.parseInt(splited[j]);
                    csfCentroidIndies[j][i] = Long.parseLong(splited[j]);
                }
                quantizedVectors[i] = vector;
                i++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new QuantizedResult(quantizedVectors, csfCentroidIndies);
    }

    static Float[][][] readCodebooks() {
        int numCodebooks = 5;
        int k = 256;
        int M = 20;
        Float codebooks[][][] = new Float[numCodebooks][k][M];

        try {
            File file = new File("../data/word2vec/codebooks.txt");
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

    static Hashtable<String, Integer[]> createEmbeddingHashtable(String keys[], Integer quantized[][]) {
        Hashtable<String, Integer[]> wordsToEmbeddings = new Hashtable<String, Integer[]>(130000);
        for (int i = 0; i < keys.length; i++) {
            wordsToEmbeddings.put(keys[i], quantized[i]);
        }
        return wordsToEmbeddings;
    }

    static ArrayList<GV3CompressedFunction<String>> buildCsfArray(String[] keys, final Long[][] csfCentroidIndices) {
        Iterable<String> keysIterable = Arrays.asList(keys);

        // values for CSFs
        ArrayList<LongIterable> centroidIndicesIterable = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
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
        for (int i = 0; i < 5; i++) {
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

    private static void testMemory(ArrayList<GV3CompressedFunction<String>> csfArray,
            Hashtable<String, Integer[]> wordsToEmbeddings, Float[][][] codebooks) {

        System.out.println("\nTesting memory: \n");
        System.out.println("num keys: " + csfArray.get(0).size64());
        System.out.println("num bits for 1 csf: " + csfArray.get(0).numBits());
        try {
            csfArray.get(0).dump("singleCsf.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testLatency(ArrayList<GV3CompressedFunction<String>> csfArray,
            Hashtable<String, Integer[]> wordsToEmbeddings, String[] keys) {

        System.out.println("\nTesting latency: \n");

        int numQueries = 100000;
        long csfTimes[] = new long[numQueries];
        long hashTableTimes[] = new long[numQueries];

        Random random = new Random();
        for (int i = 0; i < numQueries; i++) {
            String query = keys[random.nextInt(keys.length)];

            long startTime = System.nanoTime();
            // for (int j = 0; j < 5; j++) {
            // csfArray.get(j).getLong(query);
            // }
            csfArray.get(0).getLong(query);
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

        System.out.println("P99 latency for csf and hashTables");
        System.out.println("Csf: " + Long.toString(csfTimes[99000]));
        System.out.println("Hashtable: " + Long.toString(hashTableTimes[99000]));

        System.out.println("P99.9 latency for csf and hashTables");
        System.out.println("Csf: " + Long.toString(csfTimes[99900]));
        System.out.println("Hashtable: " + Long.toString(hashTableTimes[99900]));
    }

    public static void main(String args[]) {
        System.out.println("HELLO WORLD");

        String keys[] = readKeys();
        QuantizedResult result = readQuantized();
        Integer quantized[][] = result.getQuantizedVectors();
        Float codebooks[][][] = readCodebooks();

        // build csfArray from keys to values
        ArrayList<GV3CompressedFunction<String>> csfArray = buildCsfArray(keys, result.getCsfCentroidIndices());

        // build standard java hash table for keys to values
        Hashtable<String, Integer[]> wordsToEmbeddings = createEmbeddingHashtable(keys, quantized);

        testMemory(csfArray, wordsToEmbeddings, codebooks);
        testLatency(csfArray, wordsToEmbeddings, keys);

        // System.out.println(csfArray.get(0).getLong(keys[0]));
        // System.out.println(wordsToEmbeddings.get(keys[0]));

    }
}
