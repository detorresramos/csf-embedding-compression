package csf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.sux4j.mph.GV3CompressedFunction;

class Csf {

    static String[] readKeys() {
        String keys[] = new String[100000];

        try {
            File file = new File("../data/word2vec/keys.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));

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

        public Long[][] getCsfCentroidIndies() {
            return second;
        }
    }

    static QuantizedResult readQuantized() {
        Integer quantizedVectors[][] = new Integer[100000][5];

        Long csfCentroidIndies[][] = new Long[5][100000];

        try {
            File file = new File("../data/word2vec/quantized.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));

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

    public static void main(String args[]) {
        System.out.println("HELLO WORLD");

        String keys[] = readKeys();
        Iterable<String> keysIterable = Arrays.asList(keys);

        QuantizedResult result = readQuantized();

        Integer quantized[][] = result.getQuantizedVectors();
        final Long csfCentroidIndies[][] = result.getCsfCentroidIndies();

        LongIterable centroidIndicesIterable[] = new LongIterable[5];
        for (int i = 0; i < 5; i++) {
            final int ii = i;
            centroidIndicesIterable[i] = new LongIterable() {

                @Override
                public LongIterator iterator() {
                    return (LongIterator) Arrays.asList(csfCentroidIndies[ii]).iterator();
                }
            };
        }

        Float codebooks[][][] = readCodebooks();

        GV3CompressedFunction.Builder<String> csf = new GV3CompressedFunction.Builder<>();

        Hashtable<String, Integer[]> wordsToEmbeddings = createEmbeddingHashtable(keys, quantized);

    }
}