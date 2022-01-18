#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <inttypes.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <assert.h>
#include <time.h>
#include <omp.h>
#include "spooky.h"

#include "csf3.h"

void BubbleSort(double a[], int array_size)
{
    int i, j;
    double temp;
    for (i = 0; i < (array_size - 1); ++i)
    {
        for (j = 0; j < array_size - 1 - i; ++j )
        {
            if (a[j] > a[j+1])
            {
                temp = a[j+1];
                a[j+1] = a[j];
                a[j] = temp;
            }
        }
    }
}

// Pass in a base filename
// read the csfs from the dump file and create an array of them
// read the keys from /keys.txt and create a random query set of N keys
// query the csfs and report the average time, P99 Latency and P99.9 Latency
int main(int argc, char** argv) {

    char *baseDirectory = argv[1]; // Example: ../../data/ABCHeadlines_freq/testing/testing_M2/

    // load all csfs from files
    int i = 0;
    csf_t *csfArray[64];
    int numCsfs = 0;
    while (1) {
        char dumpDirectory[75] = "";
        strcat(dumpDirectory, baseDirectory);
        strcat(dumpDirectory, "dump/csf");
        char str[12];
        sprintf(str, "%d", i);
        strcat(dumpDirectory, str);
        // printf("Reading CSF from file: %s\n", dumpDirectory);

        int ch = open(dumpDirectory, O_RDONLY);
        // printf("file pointer: %d\n", ch);
        if (ch == -1) break;
        csf_t *csf = load_csf(ch);
        csfArray[i] = csf;
        close(ch);
        i++;
        numCsfs++;
    }
    printf("Read %d CSF files\n", i);

    // generate random set of N query keys
    char keysFilePath[75] = "";
    strcat(keysFilePath, baseDirectory);
    strcat(keysFilePath, "keys.txt");

    // find number of keys in file
    FILE *fp = fopen(keysFilePath, "r");
    int numKeys = 0;
    while(!feof(fp)) {
        char ch = fgetc(fp);
        if (ch == '\n') numKeys++;
    }
    fclose(fp);
    printf("num keys: %d\n", numKeys);

    // create array of all keys
    FILE *actualFp = fopen(keysFilePath, "r");
    char (*keys)[256] = malloc(sizeof(*keys) * numKeys);
    i = 0;
    while (fgets(keys[i], sizeof(keys[i]), actualFp)) {
        if (i >= numKeys) break;
        i++;
    }
    fclose(actualFp);
    // printf("%s\n", keys[49]);


    // performance testing
    int numQueries = 1000;
    double *timings = malloc(sizeof(double) * numQueries);
    for (int i = 0; i < numQueries; i++) {
        char *queryKey = keys[i * 10];
        queryKey[strcspn(queryKey, "\n")] = '\0';
        clock_t start = clock(), diff;
        // #pragma omp parallel
        // #pragma omp for
        // uint64_t signature[4];
        // spooky_short(queryKey, strlen(queryKey), csfArray[0]->global_seed, signature);
        // # pragma omp parallel for
        for (int csfNum = 0; csfNum < numCsfs; csfNum++) {
            // printf("%lu\n", csfArray[csfNum]->global_seed);
            csf3_get_byte_array(csfArray[csfNum], queryKey, strlen(queryKey));
            // csf3_get_byte_array_with_hash(csfArray[csfNum], signature);
            // printf("%s, %ld\n", queryKey, val);
        }
        // printf("LMFAO\n");
        diff = clock() - start;
        double msec = (diff * 1000.0) / CLOCKS_PER_SEC;
        timings[i] = msec;
    }
    // printf("LMFAO\n");
    BubbleSort(timings, numQueries);
    printf("%f\n", timings[500]);

    // int64_t val = csf3_get_byte_array(csf, "10", 2); 
    // printf("%ld\n", val);


    // #pragma loop through all csf, populate array
    // use same seed for all csf and pass spooky into new lookup function

    // cache efficient if all e's the same
    for (int csfNum = 0; csfNum < numCsfs; csfNum++) {
        destroy_csf(csfArray[csfNum]);
    }
    free(keys);
    free(timings);
}
