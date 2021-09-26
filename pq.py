#!/usr/bin/env python
# coding: utf-8

from sklearn.metrics import silhouette_score
from sklearn.cluster import KMeans
import math
import numpy as np


# English CoNLL17 corpus, Word2Vec Continuous Skipgram, 4027169 word embeddings
# from http://vectors.nlpl.eu/repository/
f = open('data/word2vec/model.txt', encoding="ISO-8859-1")
lines = f.readlines()

keys = []
embeddings = []
for i in range(1, 100001):  # use len(lines) for the whole table, testing on first 1000000
    line = lines[i].split(" ")
    keys.append(line[0])
    embeddings.append(line[1:-1])


def product_quantization(embeddings, M, k, verbose=False, silhouette_scoring=False, inertia_scoring=False):
    """
    keys: embeddings keys
    embeddings: embedding vectors
    M: size of vector subsections
    k: number of clusters for k-means clustering

    Runs product quantization on the embeddings.
    Returns each embedding as an array of integers, each representing the id of a centroid in that region
    """
    if verbose:
        print(embeddings)
    # build split embeddings
    # split_embeddings[i] -> list of vectors, each which represents the ith section of M units of embeddings
    num_subsections = math.ceil(len(embeddings[0]) / M)
    print(
        f"Splitting {len(embeddings)} embeddings of size {len(embeddings[0])} into {num_subsections} subsections of size {M}")
    split_embeddings = [[] for _ in range(num_subsections)]
    for embedding in embeddings:
        subsections = [embedding[i:i + M] for i in range(0, len(embedding), M)]
        for i in range(len(subsections)):
            split_embeddings[i].append(subsections[i])

    if verbose:
        print(split_embeddings)

    print(f"Performing k means search with k = {k}")
    embeddings_as_centroid_ids = [[] for _ in range(len(embeddings))]

    # given a subsection index and a centroid id within that subsection, return the centroid
    # codebooks = [
    #   [centroid0, centroid1, ..., centroidk], # subsection_0
    #   [centroid0, centroid1, ..., centroidk], # subsection_1
    #   ...
    #   [centroid0, centroid1, ..., centroidk], # subsection_num_subsections
    # ]
    codebooks = [[] for _ in range(num_subsections)]
    section_index = 0
    for section in split_embeddings:
        print(f"Starting k means for section {section_index}")
        X = np.array(section)
        if verbose:
            print("performing k means with ...")
            print(X)
        kmeans = KMeans(n_clusters=k, random_state=0).fit(X)
        labels = kmeans.predict(X)
        centroids = kmeans.cluster_centers_
        if silhouette_scoring:
            return silhouette_score(X, kmeans.labels_, metric='euclidean')
        if inertia_scoring:
            return kmeans.inertia_
        for i in range(len(labels)):
            centroid_id = labels[i]
            embeddings_as_centroid_ids[i].append(centroid_id)
        for i in range(len(centroids)):
            codebooks[section_index].append(centroids[i])
        section_index += 1

    return embeddings_as_centroid_ids, codebooks


quantized, codebooks = product_quantization(embeddings, M=20, k=256)

np.savetxt('data/word2vec/keys.txt', np.array(keys), fmt="%s")
np.savetxt('data/word2vec/quantized.txt', quantized, fmt='%i')
np.savetxt('data/word2vec/codebooks.txt',
           np.array(codebooks).reshape(np.array(codebooks).shape[0], -1), fmt='%1.3f')
print(len(codebooks[0][0]))


def main():
    pass


main()
