import h5py
from collections import defaultdict
from heapq import heappush, heappop
import numpy as np
import argparse

def read_file(data_name, base_directory):
    filename = base_directory + data_name
    with h5py.File(filename, "r") as f:
        # List all groups
        print("Keys: %s" % f.keys())
        distances = list(f.keys())[0]
        neighbors = list(f.keys())[1]
        test = list(f.keys())[2]
        train = list(f.keys())[3]

        # Get the data
        train = list(f[train])
        distances = list(f[distances])
        neighbors = list(f[neighbors])
        test = list(f[test])
    return neighbors, test



def readQuantAndCodes(rootDirectory, M):
    print(f"Reading quantization codes for M={M}")
    quantFile = rootDirectory + f"testing/testing_M{M}/" + "quantized.txt"
    codesFile = rootDirectory + f"testing/testing_M{M}/" + "codebooks.txt"
    quantizedVectors = []
    codebooks = defaultdict(list)
    with open(quantFile, "r") as f:
        lines = f.readlines()
        for line in lines:
            quantizedVectors.append([int(x) for x in line.split(" ")])

    with open(codesFile, "r") as f:
        lines = f.readlines()
        codebook_index = 0
        for line in lines:
            centroids_in_column = []
            whole_line_as_ints = [float(x) for x in line.split(" ")]
            index = 0
            for i in range(256):
                centroid = []
                for j in range(M):
                    centroid.append(whole_line_as_ints[index])
                    index += 1
                codebooks[codebook_index].append(centroid)
            codebook_index += 1

    return quantizedVectors, codebooks


def calculate_avg_knn_recall(directory, M, test, neighbors):
    quantizedVectors, codebooks = readQuantAndCodes(directory, M)
    full_quantized = []
    for vector in quantizedVectors:
        actual_vector = []
        for i in range(len(vector)):
            for code in codebooks[i][vector[i]]:
                actual_vector.append(code)
        full_quantized.append(actual_vector)
    
    predicted_neighbors_pq = []
    for i1, vector1 in enumerate(test):
        # for each query vector, find distance between all vectors
        # enter into a heap of size 100
        heap = []
        for i2, vector2 in enumerate(full_quantized):
            dist = np.linalg.norm(vector1 - vector2)
            if len(heap) == 100:
                if -dist > heap[0][0]:
                    heappop(heap)
                    heappush(heap, (-dist, i1, i2))
            else:
                heappush(heap, (-dist, i1, i2))
        closest_neighbors = [x[2] for x in heap]
        predicted_neighbors_pq.append(closest_neighbors)
        if i1 == 100:
            break
    
    print(len(predicted_neighbors_pq))
    len_predict = len(predicted_neighbors_pq)
    running_avg_knn_recall = 0
    for i, actual_neighbors in enumerate(neighbors[:len_predict]):
        # how many of the actual top 100 neighbors did you find
        running_avg_knn_recall += len(actual_neighbors) - len(set(actual_neighbors).difference(set(predicted_neighbors_pq[i])))
    
    print(f"Running avg of KNN Recall for M={M} is at {running_avg_knn_recall / len_predict}")
    
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('data_name', action="store", metavar="<data_name>",
                        help="File from ANN test dataset in hdf5 format")
    parser.add_argument('input_directory', action="store", metavar="<input_directory>",
                        help="The base directory where quantized and codebooks are stored")
    parser.add_argument("M", action="store", metavar="<M>",
                        help="Desired M for subvector sizes")
    args = parser.parse_args()

    neighbors, test = read_file(args.data_name, args.input_directory)
    calculate_avg_knn_recall(args.input_directory, int(args.M), test, neighbors)


main()