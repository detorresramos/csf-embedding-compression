import numpy as np
import sys
import time

from collections import defaultdict
import functools

'''
Problem description: 

Given an integer matrix A in Z^(NxM) with N rows and M columns, 
suppose that the entries of the matrix can be permuted along each
row without affecting the validity of the representation. For example,
the following rows are equivalent:
[1, 2, 4, 3, 0]
[0, 4, 2, 3, 1]
Such a situation can arise in product recommendation, where each
row is a set of pre-computed product IDs, or in genomics, where
each row is a set of minhash values. The order of items within a
row does not matter.

The task is to find the permutation with the lowest average column-wise entropy.

Algorithm:

1. Initialize table T from value {v} -> row list [r1, r2, ...]
2. Initialize table E from column {c} -> ineligible rows [r1, r2, ...]
3. While there are still eligible rows (while min_c |E[c]| < M):
4. Sort T by |T[v]| in descending order
5. (c^*, v^*) = (None, None) and Smax = 0 (max size of possible entropy-block)
6. for i = 0 to len(T):
    c' = argmax_c |T[vi] setminus E[c]|
    (i.e. the intersection of rows containing this value with eligiable rows)
    if |T[vi] setminus E[i]| > Smax:
        Smax = |T[vi] setminux E[c]|
        c^*, v^* = c', vi
Note: we use several heuristic optimizations in (6) to early terminate for optimal values c^* and v^*
7. For r in eligible rows (T[vi] setminus E[i])
    find v^* at location c in A[r]
    swap A[c] (containing v^*) and A[c^*] (containing some other value)
    pop r from T[v^*]
    add r to E[c^*] (this slot is "filled in" like sudoku)
'''

def permute_entropy(data, max_iter = None, min_block_size = 1, verbose = 0):
    # data: NxM int array
    # max_iter: maximum number of permutation steps to take
    # min_block_size: minimum size of group to permute. If min_block_size
    # is 100, we will not consider swapping values unless we can group
    # at least 100 of the same value into a single row.
    # verbose: 0 - no output, 1 - iteration-wise output, 2 - full debug info
    num_rows = data.shape[0]
    num_cols = data.shape[1]

    values = set()
    row_lists = defaultdict(lambda: set())  # Table T in the algorithm.
    for row, row_contents in enumerate(data):
        if verbose > 1 and row % 1000 == 0:
            print('.',end='')
            sys.stdout.flush()
        for value in row_contents:
            values.add(value)
            row_lists[value].add(row)
    if verbose > 1:
        print('')

    ineligible_rows = [set() for _ in range(num_cols)]  # Table E in the algorithm.

    # Optimization: search columns starting with "most eligible"
    column_list = list(range(num_cols))
    column_key_fn = functools.cmp_to_key(lambda c1,c2: len(ineligible_rows[c1]) - len(ineligible_rows[c2]))
    column_list.sort(key=column_key_fn)

    # Optimization: Delete the tail, to speed up search / sorting later.
    values = list(values)
    values_key_fn = functools.cmp_to_key(lambda v1,v2: len(row_lists[v1]) - len(row_lists[v2]))
    values.sort(key=values_key_fn)
    for val_idx in range(len(values)):
        # Binary search would be better, but this only happens once.
        if len(row_lists[values[val_idx]]) >= min_block_size:
            break
    for val in values[:val_idx]:
        del row_lists[val]
    values = values[val_idx:]
    num_values = len(values)
    if verbose > 0:
        print("Considering",num_values,"unique values.")

    # List of keys v for row_lists, sorted in order of len(T[v]).
    # Optimization: After the first sorting, sort based on max number of eligible rows
    # rather than just the number of available rows.
    values_max_eligible = {v : len(row_lists[v]) for v in values}
    values_key_fn = functools.cmp_to_key(lambda v1,v2: values_max_eligible[v1] - values_max_eligible[v2])

    for iteration in range(num_values):
        t0 = time.time()
        if max_iter is not None and iteration >= max_iter:
            return data

        best_num_eligible = -1
        best_val = None
        best_val_idx = None
        best_col = None
        best_row_set = None

        # start with the value with highest known number of eligible rows.
        for val_idx in reversed(range(len(values))):
            t00 = time.time()
            val = values[val_idx]
            if best_num_eligible >= len(row_lists[val]):
                # Since row_lists are indexed in descending order of size,
                # if the current list offers fewer eligible values than the
                # best option so far, we can skip all remaining values.
                break
            value_max_eligible = 0
            for col in column_list:  # find argmax over columns.
                eligible_row_set = (row_lists[val] - ineligible_rows[col])
                value_max_eligible = max(value_max_eligible, len(eligible_row_set))
                if len(eligible_row_set) > best_num_eligible:
                    best_num_eligible = len(eligible_row_set)
                    best_row_set = eligible_row_set
                    best_val = val
                    best_val_idx = val_idx
                    best_col = col
                if len(eligible_row_set) == len(row_lists[val]):
                    # Short circuit the evaluation over columns since no other column will do better.
                    break
                if len(eligible_row_set) >= (num_rows - len(ineligible_rows[col])):
                    # Since column_list are indexed in descending order of size,
                    # if the current column offers fewer eligible values than the best
                    # option so far, we can skip all remaining columns.
                    break

            values_max_eligible[val] = value_max_eligible
            t01 = time.time()
            if verbose > 1:
                print("\tTime for col argmax@val",val,":",t01-t00)
                print("\t\t",val,"available:",len(row_lists[val]),"max_eligible:",value_max_eligible,"fraction:",float(value_max_eligible)/len(row_lists[val]))

        if best_val is None:
            return data  # No values remaining in list.
       
        if verbose > 1:
            print("\nChose:",best_val,"best_available:",len(row_lists[best_val]),"best_eligible:",best_num_eligible,"fraction:",float(best_num_eligible)/len(row_lists[best_val]))

        t00 = time.time()
        # For all rows in best_row_set, transfer best_val to location best_col.
        for row in best_row_set:
            # This is brute-force over columns, but it's not a bottleneck.
            initial_col = np.where(data[row,:] == best_val)[0]
            if len(initial_col) >= 1:
                initial_col = initial_col[0]
            else:  # This only occurs if best_val is not in the row, which should not happen.
                continue
            data[row, initial_col] = data[row, best_col]
            data[row, best_col] = best_val
        t01 = time.time()
        if verbose > 1:
            print("Swap time:", t01 - t00)

        t00 = time.time()
        # Mark rows as ineligible if we have assigned them.
        ineligible_rows[best_col] = ineligible_rows[best_col].union(best_row_set)

        # Now delete best_row_set from row_lists[best_val]
        if len(best_row_set) == len(row_lists[best_val]):
            del row_lists[best_val]
            # Delete the value if we've re-sorted all the values.
            del values[best_val_idx]
        else:
            row_lists[best_val] = row_lists[best_val] - best_row_set

        t01 = time.time()
        # Maintain invariants. Consider using bisect.insort if the
        # number of updated values is small and sorting is a bottleneck.
        values.sort(key=values_key_fn)
        column_list.sort(key=column_key_fn)
        t1 = time.time()
        if verbose > 1:
            print("Time to maintain invariants:", t1 - t00, "[Sort time:",t1-t01,"]")

        if verbose > 0:
            print("Iteration time:", t1 - t0)
            sys.stdout.flush()

        if min_block_size is not None and best_num_eligible <= min_block_size:
            return data


if __name__ == "__main__":
    # Correctness test.
    data = np.array(
        [
         [1, 2, 3],
         [3, 2, 1],
         [8, 1, 3],
         [4, 8, 1],
         [7, 4, 1],
         [9, 0, 1],
         [2, 1, 5],
         [4, 5, 6],
         [6, 8, 1],
         [2, 3, 9],
         [8, 7, 1],
         [4, 1, 2],
         [3, 4, 2],
         [3, 4, 10],
         [3, 4, 11],
         ],
        dtype = int)
    print(data)
    data = permute_entropy(data, min_block_size=2, verbose=1)
    print(data)

    # X = np.load('proteomes.npy')
    # X = permute_entropy(X, min_block_size = 100)
    # np.save('proteomes_minH_block100.npy', X)