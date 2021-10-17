M_sizes=(2 4 8 16 32 64)

for M in ${M_sizes[@]};
do
    # python3 pq.py data/word2vec/model.txt data/word2vec/testing/testing_M$M 256 $M &
    python3 pq.py data/sift/sift-128-euclidean.hdf5 data/sift/testing/testing_M$M 256 $M &
done

# TODO: can parallelize better
wait

for M in ${M_sizes[@]};
do
    java -jar embedding.jar data/sift/testing/testing_M$M data/sift/testing/testing_M$M/results_k256_M$M.txt 256 $M &
done

wait