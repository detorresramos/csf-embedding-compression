#include <iostream>
#include <fstream>
#include <string>
#include <unordered_map>
#include <vector>
#include <sstream>
#include <chrono>
#include <ctime>   
#include <algorithm>
using namespace std;

int main(int argc, char *argv[]) {
    cout << "Hello World!\n";

    string baseDirectory = argv[1];

    string strkey;
    string strValue;
    ifstream keysFile(baseDirectory + "keys.txt");
    ifstream valuesFile(baseDirectory + "quantized.txt");

    vector<string> queryKeys;
    int i = 0;

    // find length of quantized arrays, new vectors
    unordered_map<string, vector<int>> map;
    while (getline(keysFile, strkey)) {
        getline(valuesFile, strValue);

        vector<int> quantized;
        stringstream ss(strValue);
        string tok;
        while(getline(ss, tok, ' ')) { 
            quantized.push_back(stoi(tok)); 
        }
        map[strkey] = quantized;

        if (i % 3 == 0 && queryKeys.size() < 10000) {
            queryKeys.push_back(strkey);
        }
        i++;
    }
    keysFile.close();
    valuesFile.close();


    // for (int i: map[map.size()])
    //     cout << i;
    

    // do timing tests
    vector<chrono::duration<double>> timings;
    for (string i : queryKeys) {
        auto start = std::chrono::high_resolution_clock::now();
        map[i];
        auto end = std::chrono::high_resolution_clock::now();
        std::chrono::duration<double, std::milli> ms_double = end - start;
        timings.push_back(ms_double);
    }

    sort(timings.begin(), timings.end());

    cout << "Median: " << timings[5000].count() << "s\n";
    cout << "P99: " << timings[9900].count() << "s\n";
    cout << "P99.9: " << timings[9990].count() << "s\n";
    cout << "P99.99: " << timings[9999].count() << "s\n";

    return 0;
}