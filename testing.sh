M_sizes=(2 5 10 20 50)

for M in ${M_sizes[@]};
do
    python3 pq.py data/word2vec/model.txt data/word2vec/testing/testing_M$M 256 $M &
done

# TODO: can parallelize better
wait

for M in ${M_sizes[@]};
do
    java -jar embedding.jar data/word2vec/testing/testing_M$M data/word2vec/testing/testing_M$M/results_k256_M$M.txt 256 $M &
done

wait