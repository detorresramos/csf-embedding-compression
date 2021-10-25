from argparse import ArgumentParser
from collections import defaultdict
import numpy as np

def entropy_calculation():
    parser = ArgumentParser()
    parser.add_argument('quantized_filename', action="store", metavar="<input_file>",
                        help="File with quantized embeddings")
    parser.add_argument("M", action="store", metavar="<M>",
                    help="M for subvector sizes")
    args = parser.parse_args()

    f = open(args.quantized_filename, "r")
    lines = f.readlines()

    quantized_codes = []
    for i in range(1, len(lines)):  # use len(lines) for the whole table, testing on first 1000000
        line = lines[i].split(" ")
        quantized_codes.append([int(x) for x in line])

    quantized_counts = []
    for i in range(len(quantized_codes[0])):
        counts = defaultdict(int)
        for vec in quantized_codes:
            counts[vec[i]] += 1
        quantized_counts.append(counts)
    entropy_vals = []
    for index, counts in enumerate(quantized_counts):
        entropy = 0
        for code, count in counts.items():
            p = count / len(quantized_codes)
            entropy -= p * np.log(p)
        entropy_vals.append(entropy)
    print(f"Average entropy for M = {args.M}: {sum(entropy_vals) / len(entropy_vals)}")

entropy_calculation()