# dataset_name="sift"
# source_data="sift-128-euclidean.hdf5"
# M_sizes=(2 4 8 16 32 64)

# dataset_name="word2vec"
# source_data="model.txt"
# M_sizes=(2 5 10 20 50)

# dataset_name="nytimes"
# source_data="nytimes-256-angular.hdf5"
# M_sizes=(2 4 8 16 32 64)

# dataset_name="lastfm"
# source_data="lastfm-64-dot.hdf5"
# M_sizes=(2 5 10 15 20)

# dataset_name="ABCHeadlines"
# source_data="abcnews-date-text.csv"
# M_sizes=(2 5 10 20 50)

dataset_name="ABCHeadlines_freq"
source_data="abcnews-date-text.csv"
M_sizes=(2 5 10 20 50)

# # run product quantization
# for M in ${M_sizes[@]};
# do
#     python3 pq.py data/$dataset_name/$source_data data/$dataset_name/testing/testing_M$M 256 $M &
# done
# wait

# # calculate entropy
# for M in ${M_sizes[@]};
# do
#     python3 entropy.py data/$dataset_name/testing/testing_M$M/quantized.txt $M &
# done
# wait

# create CSF
for M in ${M_sizes[@]};
do
    java -jar embedding.jar $dataset_name 256 $M &
done
wait
