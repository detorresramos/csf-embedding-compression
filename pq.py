#!/usr/bin/env python
# coding: utf-8

from sklearn.metrics import silhouette_score
from sklearn.cluster import KMeans
from collections import defaultdict
import argparse
import math
import numpy as np
import faiss


class FaissKMeans:
    def __init__(self, n_clusters=8, n_init=10, max_iter=300):
        self.n_clusters = n_clusters
        self.n_init = n_init
        self.max_iter = max_iter
        self.kmeans = None
        self.cluster_centers_ = None
        self.inertia_ = None

    def fit(self, X):
        self.kmeans = faiss.Kmeans(d=X.shape[1],
                                   k=self.n_clusters,
                                   niter=self.max_iter,
                                   nredo=self.n_init)
        self.kmeans.train(X.astype(np.float32))
        self.cluster_centers_ = self.kmeans.centroids
        self.inertia_ = self.kmeans.obj[-1]

    def predict(self, X):
        return self.kmeans.index.search(X.astype(np.float32), 1)[1]


def handle_big(filename, with_keys=False):
    print("YANDEX")
    N = 1000000000
    d = 200
    with open(filename, 'rb') as f:  # Must be opened as binary file
        X = np.fromfile(file=f, dtype=np.float32, count=N*d, offset=8)
        print("Finished reading data")
        # dataset is in C-contiguous order (not Fortran), so
        # we can re-view X instead of np.reshape (which may copy).
        embeddings = X.view()
        embeddings.shape = (N, d)  # Throws AttributeError if len(X) != N*d
        # I checked using that the float values are correct
        # (and in the correct order) using a hex-viewer.
        if with_keys:
            keys = np.arange(N)
            return keys, embeddings
        else:
            return embeddings


def get_embeddings_from_file(filename):
    if filename[-5:] == ".hdf5":
        import h5py

        with h5py.File(filename, "r") as f:
            # List all groups
            # distances = list(f.keys())[0]
            # neighbors = list(f.keys())[1]
            # test = list(f.keys())[2]
            train = list(f.keys())[3]
            # Get the data
            train = list(f[train])
            keys = list(range(len(train)))
            embeddings = [list(x) for x in train]
            return keys, embeddings
    elif filename == "data/word2vec/model.txt":
        f = open(filename, encoding="ISO-8859-1")
        lines = f.readlines()

        keys = []
        embeddings = []
        for i in range(1, len(lines)):  # use len(lines) for the whole table, testing on first 1000000
            line = lines[i].split(" ")
            keys.append(line[0])
            embeddings.append(line[1:-1])
        print("READ EMBEDDINGS FROM FILE")
        return keys, embeddings
    elif filename == "data/ABCHeadlines/abcnews-date-text.csv" or filename == "data/ABCHeadlines_freq/abcnews-date-text.csv":
        # create embeddings from headlines by averaging the embeddings of the words
        # finds word embeddings from word2vec

        f = open("data/word2vec/model.txt", encoding="ISO-8859-1")
        lines = f.readlines()

        word2vec = defaultdict(list) #dictionary of words to embedding
        for i in range(1, len(lines)):  # use len(lines) for the whole table, testing on first 1000000
            line = lines[i].split(" ")
            word2vec[line[0]] = line[1:-1]
            embedding_dim = len(line) - 1

        headline_file = open(filename)
        headlines_lines = headline_file.readlines()

        freq_weighting = False
        if "freq" in filename:
            freq_weighting = True
            num_words = 0
            word_frequencies = defaultdict(int)
            for i in range(1, len(headlines_lines)):
                line = headlines_lines[i].split(",")
                for word in line[1].split(" "):
                    word_frequencies[word] += 1
                    num_words += 1
        
        keys = []
        embeddings = []
        for i in range(1, len(headlines_lines)):
            line = headlines_lines[i].split(",")
            keys.append(i)
            headline_embedding = []
            sums = [0] * embedding_dim
            for word in line[1].split(" "):
                word_embedding = word2vec[word] 
                for i, val in enumerate(word_embedding):
                    if freq_weighting:
                        sums[i] += float(val) * word_frequencies[word] / num_words
                    else:
                        sums[i] += float(val)
            for val in sums:
                headline_embedding.append(val / len(sums))
            embeddings.append(headline_embedding)
        return keys, embeddings

    print(f"File not explicitly processed {filename}")


def big_pq(filename, k, M):
    keys, embeddings = handle_big(filename, with_keys=True)
    num_subsections = math.ceil(len(embeddings[0]) / M)
    embeddings_as_centroid_ids = [[] for _ in range(len(embeddings))]

    for section_index in range(num_subsections):
        section = embeddings[:,section_index:section_index+M]
        section = section.copy()
        del embeddings

        print(f"Starting k means for section {section_index} on M={M}")
        kmeans = FaissKMeans(n_clusters=k)
        kmeans.fit(section)
        labels = kmeans.predict(section)
        centroids = kmeans.cluster_centers_
        for i in range(len(labels)):
            centroid_id = labels[i]
            embeddings_as_centroid_ids[i].append(centroid_id[0])
        if section_index != num_subsections - 1:
            embeddings = handle_big(filename, with_keys=False)
    
    return keys, embeddings_as_centroid_ids
    


def product_quantization(embeddings, M, k, verbose=False):
    """
    embeddings: embedding vectors
    M: size of vector subsections
    k: number of clusters for k-means clustering

    Runs product quantization on the embeddings.
    Returns each embedding as an array of integers, each representing the id of a centroid in that region
    """
    # build split embeddings
    # split_embeddings[i] -> list of vectors, each which represents the ith section of M units of embeddings
    num_subsections = math.ceil(len(embeddings[0]) / M)
    print(
        f"Splitting {len(embeddings)} embeddings of size {len(embeddings[0])} into {num_subsections} subsections of size {M}")
    # split_embeddings = [[] for _ in range(num_subsections)]
    # for embedding in embeddings:
    #     subsections = [embedding[i:i + M] for i in range(0, len(embedding), M)]
    #     for i in range(len(subsections)):
    #         split_embeddings[i].append(subsections[i])

    print(f"Performing k means search with k = {k}")
    embeddings_as_centroid_ids = [[] for _ in range(len(embeddings))]

    # codebooks = [[] for _ in range(num_subsections)]
    # section_index = 0
    # for section in split_embeddings:

    for section_index in range(num_subsections):
        section = embeddings[:,section_index:section_index+M]

        print(f"Starting k means for section {section_index} on M={M}")
        kmeans = FaissKMeans(n_clusters=k)
        kmeans.fit(section)
        labels = kmeans.predict(section)
        centroids = kmeans.cluster_centers_
        for i in range(len(labels)):
            centroid_id = labels[i]
            embeddings_as_centroid_ids[i].append(centroid_id[0])
        # for i in range(len(centroids)):
        #     codebooks[section_index].append(centroids[i])
        # section_index += 1

    return embeddings_as_centroid_ids, codebooks


def save_outputs_in_directory(keys, quantized, codebooks, directory):
    np.savetxt(directory + '/keys.txt', np.array(keys), fmt="%s")
    np.savetxt(directory + '/quantized.txt', quantized, fmt='%i')
    # np.savetxt(directory + '/codebooks.txt',
    #            np.array(codebooks).reshape(np.array(codebooks).shape[0], -1), fmt='%1.3f')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('input_file', action="store", metavar="<input_file>",
                        help="File with input embeddings, each line following format: key 1.0, 6.7, ..., -5.2")
    parser.add_argument('output_directory', action="store",
                        metavar="<output_directory>", help="Directory to output results to.")
    parser.add_argument("k", action="store", metavar="<k>",
                        help="Desired k for k-means.")
    parser.add_argument("M", action="store", metavar="<M>",
                        help="Desired M for subvector sizes")
    args = parser.parse_args()
    if "yandex" in args.input_file:
        keys, quantized = big_pq(args.input_file, k=int(args.k), M=int(args.M))
        save_outputs_in_directory(keys, quantized, None, args.output_directory)
    else:
        keys, embeddings = get_embeddings_from_file(args.input_file)
        quantized, codebooks = product_quantization(embeddings, k=int(args.k), M=int(args.M), verbose=True)
        save_outputs_in_directory(
            keys, quantized, codebooks, args.output_directory)


main()
