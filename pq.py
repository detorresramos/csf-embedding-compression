#!/usr/bin/env python
# coding: utf-8

from sklearn.metrics import silhouette_score
from sklearn.cluster import KMeans
import argparse
import math
import numpy as np


def get_embeddings_from_file(filename):
    # English CoNLL17 corpus, Word2Vec Continuous Skipgram, 4027169 word embeddings
    # from http://vectors.nlpl.eu/repository/
    f = open(filename, encoding="ISO-8859-1")
    lines = f.readlines()

    keys = []
    embeddings = []
    for i in range(1, 100001):  # use len(lines) for the whole table, testing on first 1000000
        line = lines[i].split(" ")
        keys.append(line[0])
        embeddings.append(line[1:-1])
    return keys, embeddings


def product_quantization(embeddings, M, k, verbose=False, silhouette_scoring=False, inertia_scoring=False):
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
    split_embeddings = [[] for _ in range(num_subsections)]
    for embedding in embeddings:
        subsections = [embedding[i:i + M] for i in range(0, len(embedding), M)]
        for i in range(len(subsections)):
            split_embeddings[i].append(subsections[i])

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
            print("performing k means with ... on section " + str(section_index))
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


def save_outputs_in_directory(keys, quantized, codebooks, directory):
    np.savetxt(directory + '/keys.txt', np.array(keys), fmt="%s")
    np.savetxt(directory + '/quantized.txt', quantized, fmt='%i')
    np.savetxt(directory + '/codebooks.txt',
               np.array(codebooks).reshape(np.array(codebooks).shape[0], -1), fmt='%1.3f')


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

    # keys, embeddings = get_embeddings_from_file(args.input_file)
    # quantized, codebooks = product_quantization(
    #     embeddings, k=args.k, M=args.M, verbose=True)
    # save_outputs_in_directory(
    #     keys, quantized, codebooks, args.output_directory)


main()
