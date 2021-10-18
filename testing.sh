# dataset_name="sift"
# source_data="sift-128-euclidean.hdf5"
# M_sizes=(2 4 8 16 32 64)

dataset_name="word2vec"
source_data="model.txt"
M_sizes=(2 5 10 20 50)

# for M in ${M_sizes[@]};
# do
#     # python3 pq.py data/$dataset_name/$source_data data/$dataset_name/testing/testing_M$M 256 $M &
#     python3 pq.py data/$dataset_name/$source_data data/$dataset_name/testing/testing_M$M 256 $M &
# done

# TODO: can parallelize better
# wait

for M in ${M_sizes[@]};
do
    java -jar embedding.jar data/$dataset_name/testing/testing_M$M data/$dataset_name/testing/testing_M$M/results_k256_M$M.txt 256 $M &
done

wait