N_sizes=(10000 100000 1000000 4027169)


for N in ${N_sizes[@]};
do
    python3 pq.py data/word2vec/model.txt data/word2vec/word2vec_N$N/testing/testing_M10 256 10 $N &
done
wait

for N in ${N_sizes[@]};
do
    java -jar embedding.jar word2vec/testing_$N 256 10 &
done
wait