#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <inttypes.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <assert.h>
#include <time.h>

#include "csf3.h"

void BubbleSort(int a[], int array_size)
{
    int i, j, temp;
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
    numKeys = 20; 
    char keys[256][numKeys]; //TODO not enough space for all keys
    i = 0;
    while (fgets(keys[i], sizeof(keys[i]), actualFp)) {
        if (i >= numKeys) break;
        i++;
    }
    fclose(actualFp);

    // get random subsample of keys
    // int numQueries = 10000;
    // char *queryKeys[numQueries];
    // TODO generate subsample

    // performance testing
    int timings[numKeys];
    for (int i = 0; i < numKeys; i++) {
        char *queryKey = keys[i];
        queryKey[strcspn(queryKey, "\n")] = '\0';
        clock_t start = clock(), diff;
        for (int csfNum = 0; csfNum < numCsfs; csfNum++) {
            csf3_get_byte_array(csfArray[csfNum], queryKey, strlen(queryKey));
        }
        diff = clock() - start;
        int msec = diff * 1000 / CLOCKS_PER_SEC;
        timings[i] = msec;
    }

    BubbleSort(timings, numKeys);
    printf("%d\n", timings[numKeys - 1]);

    // int64_t val = csf3_get_byte_array(csf, "10", 2); 
    // printf("%ld\n", val);


    // #pragma loop through all csf, populate array
    // use same seed for all csf and pass spooky into new lookup function

    // cache efficient if all e's the same
    for (int csfNum = 0; csfNum < numCsfs; csfNum++) {
        destroy_csf(csfArray[csfNum]);
    }

}
