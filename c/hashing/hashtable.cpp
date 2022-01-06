#include <iostream>
#include <fstream>
#include <string>
#include <unordered_map>
#include <vector>
#include <sstream>
using namespace std;

int main(int argc, char *argv[]) {
    cout << "Hello World!\n";

    string baseDirectory = argv[1];

    string strkey;
    string strValue;
    ifstream keysFile(baseDirectory + "/keys.txt");
    ifstream valuesFile(baseDirectory + "/quantized.txt");


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
    }
    keysFile.close();
    valuesFile.close();

    for (int i: map[map.size()])
        cout << i;
    
    


    // do timing tests



    return 0;
}