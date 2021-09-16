package csf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

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

    static Integer[][] readQuantized() {
        Integer quantizedVectors[][] = new Integer[100000][5];

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
                }
                quantizedVectors[i] = vector;
                i++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return quantizedVectors;
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
        Integer quantized[][] = readQuantized();
        Float codebooks[][][] = readCodebooks();

        Hashtable<String, Integer[]> wordsToEmbeddings = createEmbeddingHashtable(keys, quantized);

    }
}