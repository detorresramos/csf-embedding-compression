mkdir testing
cd testing
M_sizes=(2 5 10 15 20)
for M in ${M_sizes[@]};
do
    mkdir testing_M$M
done