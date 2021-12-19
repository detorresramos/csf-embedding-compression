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


int main(int argc, char** argv) {
    int ch = open("../../data/ABCHeadlines_freq/testing/testing_M2/dump/csf1", O_RDONLY);
    csf_t *csf = load_csf(ch);
    int64_t val = csf3_get_byte_array(csf, "10", 2);

    // #pragma loop through all csf, populate array
    // use same seed for all csf and pass spooky into new lookup function

    // cache efficient if all e's the same

    // do hash table profiling in c++ with std::unordered_map
    printf("%ld\n", val);
    close(ch);
    destroy_csf(csf);
}
