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
    ifstream keysFile(baseDirectory + "/keys.txt");
    ifstream valuesFile(baseDirectory + "/quantized.txt");

    vector<int> queryKeys;
    int i = 0;

    // find length of quantized arrays, new vectors
    unordered_map<int, vector<int>> map;
    while (getline(keysFile, strkey)) {
        getline(valuesFile, strValue);

        vector<int> quantized;
        stringstream ss(strValue);
        string tok;
        while(getline(ss, tok, ' ')) { 
            quantized.push_back(stoi(tok)); 
        }
        map[stoi(strkey)] = quantized;

        if (i % 3 == 0 && queryKeys.size() < 1000) {
            queryKeys.push_back(stoi(strkey));
        }
        i++;
    }
    keysFile.close();
    valuesFile.close();


    // for (int i: map[map.size()])
    //     cout << i;
    

    // do timing tests
    vector<chrono::duration<double>> timings;
    for (int i : queryKeys) {
        auto start = std::chrono::system_clock::now();
        map[i];
        auto end = std::chrono::system_clock::now();
        timings.push_back(end-start);
    }

    sort(timings.begin(), timings.end());

    cout << timings[1000 - 10].count() << "s\n";




    return 0;
}