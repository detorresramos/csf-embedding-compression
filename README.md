# Embedding Table Compression with CSFs and Product Quantization

see testing.sh for how to run 

embedding.jar builds CSFs from a matrix using the following command:
java -jar embedding.jar data/genomes/genomes_minH_block100.txt 125367 data/genomes/result.txt data/genomes

the .txt file is the output of numpy.savetxt()
125367 is the number of rows
