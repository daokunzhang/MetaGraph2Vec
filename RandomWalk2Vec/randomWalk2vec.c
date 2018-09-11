//  The randomWalk2vec project was built upon the The metapath2vec.cpp code from https://ericdongyx.github.io/metapath2vec/m2v.html

//  Modifications Copyright (C) 2018 <daokunzhang2015@gmail.com>
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

//  The metapath2vec.cpp code was built upon the word2vec.c from https://code.google.com/archive/p/word2vec/

//  Modifications Copyright (C) 2016 <ericdongyx@gmail.com>
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

//  Copyright 2013 Google Inc. All Rights Reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>

#define MAX_STRING 100
#define EXP_TABLE_SIZE 1000
#define MAX_EXP 6
#define MAX_WALK_LENGTH 1000

const long long node_hash_size = 10000000;  // Maximum 10M nodes
const long long node_context_hash_size = 1000000000; // Maximum 1G node context pairs

struct Node
{
    long long cn;
    char *node_str;
};

struct Node_Context
{
    long long cn;
    long long source, target;
};

char train_file[MAX_STRING], output_file[MAX_STRING];
struct Node *node_list;
long long window = 5;
long long pp = 1; // pp = 1 with Heterogeneous Skip-Gram, pp = 0 with Homogeneous Skip-Gram
long long *node_hash;
long long node_list_max_size = 100000, node_list_size = 0, layer1_size = 128;
long long node_occur_num = 0;
double alpha = 0.025, starting_alpha;
double *syn0, *syn1, *syn1neg, *expTable;
double **type_syn1;
clock_t start, finish;

long long type_num;
long long **type_tables, *type_counts, *type_indices;
char prefixes[MAX_STRING];
long long obj_type;

long long **type_node_hash;
long long *type_node_list_size, *type_node_index;
struct Node **type_node_list;

long long negative = 5;
const long table_size = 1e8;
long long *table;

long long *node_context_hash;
long long node_context_list_size, node_context_list_max_size;
struct Node_Context *node_context_list;

// Parameters for node context pair sampling
long long *alias;
double *prob;

long long total_samples = 100;

long long GetTypeId(char prefix)
{
    long long i;
    for (i = 0; i < type_num; i++)
    {
        if (prefix == prefixes[i])
            break;
    }
    if (i >= type_num)
        return -1;
    else
        return i;
}

void NodeTypeNeg()
{
    long long i;
    long long target, type_Id;
    type_tables = (long long **)malloc(type_num * sizeof(long long *));
    type_counts = (long long *)malloc(type_num * sizeof(long long));
    type_indices = (long long *)malloc(type_num * sizeof(long long));
    if (type_tables == NULL || type_counts == NULL || type_indices == NULL)
    {
        printf("Memory allocation failed\n");
        exit(1);
    }
    for (i = 0; i < type_num; i++)
    {
        type_counts[i] = 0;
        type_indices[i] = 0;
    }
    for (i = 0; i < table_size; i++)
    {
        target = table[i];
        type_Id = GetTypeId(node_list[target].node_str[0]);
        if (type_Id == -1)
        {
            printf("Unrecognised type\n");
            exit(1);
        }
        type_counts[type_Id]++;
    }
    for (i = 0; i< type_num; i++)
    {
        type_tables[i] = (long long *)malloc(type_counts[i] * sizeof(long long));
        if (type_tables[i] == NULL)
        {
            printf("Memory allocation failed\n");
            exit(1);
        }
    }
    for (i = 0; i < table_size; i++)
    {
        target = table[i];
        type_Id = GetTypeId(node_list[target].node_str[0]);
        type_tables[type_Id][type_indices[type_Id]] = target;
        type_indices[type_Id]++;
    }
    for (i = 0; i < type_num; i++)
        printf("type %c table size: %lld\n", prefixes[i], type_counts[i]);
}

void InitUnigramTable()
{
    long long a, i;
    double train_nodes_pow = 0.0, d1, power = 0.75;
    table = (long long *)malloc(table_size * sizeof(long long));
    if (table == NULL)
    {
        printf("Memory allocation failed\n");
        exit(1);
    }
    for (a = 0; a < node_list_size; a++) train_nodes_pow += pow(node_list[a].cn, power);
    i = 0;
    d1 = pow(node_list[i].cn, power) / train_nodes_pow;
    for (a = 0; a < table_size; a++)
    {
        table[a] = i;
        if (a / (double)table_size > d1)
        {
            i++;
            d1 += pow(node_list[i].cn, power) / (double)train_nodes_pow;
        }
        if (i >= node_list_size) i = node_list_size - 1;
    }
    if (pp == 1)
        NodeTypeNeg();
}

// Reads a single node from a file, assuming space + tab + EOL to be node name boundaries
long long ReadNode(char *node_str, FILE *fin) // return 1, if the boundary is '\n', otherwise 0
{
    long long a = 0, ch, flag = 0;
    while (!feof(fin))
    {
        ch = fgetc(fin);
        if (ch == 13) continue;
        if ((ch == ' ') || (ch == '\t') || (ch == '\n'))
        {
            if (a > 0)
            {
                if (ch == '\n') flag = 1;
                break;
            }
            continue;
        }
        node_str[a] = ch;
        a++;
        if (a >= MAX_STRING - 1) a--;
    }
    node_str[a] = 0;
    return flag;
}

// Return hash value of a node
long long GetNodeHash(char *node_str)
{
    long long a, hash = 0;
    for (a = 0; a < strlen(node_str); a++)
    {
        hash = hash * 257 + node_str[a];
        hash = hash % node_hash_size;
    }
    return hash;
}

// Return hash value for a node context pair
long long GetNodeContextHash(char *node_str, char *context_node_str)
{
    long long a, hash = 0;
    for (a = 0; a < strlen(node_str); a++)
    {
        hash = hash * 257 + node_str[a];
        hash = hash % node_context_hash_size;
    }
    for (a = 0; a < strlen(context_node_str); a++)
    {
        hash = hash * 257 + context_node_str[a];
        hash = hash % node_context_hash_size;
    }
    return hash;
}

// Return position of a node in the node list; if the node is not found, returns -1
long long SearchNode(char *node_str)
{
    long long hash = GetNodeHash(node_str);
    while (1)
    {
        if (node_hash[hash] == -1) return -1;
        if (!strcmp(node_str, node_list[node_hash[hash]].node_str)) return node_hash[hash];
        hash = (hash + 1) % node_hash_size;
    }
    return -1;
}

// Return position of a node in the node list of the specific type; if the node is not found, returns -1
long long TypeSearchNode(char *node_str, long long type_Id)
{
    long long hash = GetNodeHash(node_str);
    while (1)
    {
        if (type_node_hash[type_Id][hash] == -1) return -1;
        if (!strcmp(node_str, type_node_list[type_Id][type_node_hash[type_Id][hash]].node_str)) return type_node_hash[type_Id][hash];
        hash = (hash + 1) % node_hash_size;
    }
    return -1;
}

// Return position of a node context pair in the node context list; if the node is not found, returns -1
long long SearchNodeContextPair(long long node, long long context)
{
    long long hash = GetNodeContextHash(node_list[node].node_str, node_list[context].node_str);
    long long hash_iter = 0;
    long long cur_node, cur_context;
    while(1)
    {
        if (node_context_hash[hash] == -1) return -1;
        cur_node = node_context_list[node_context_hash[hash]].source;
        cur_context = node_context_list[node_context_hash[hash]].target;
        if (cur_node == node && cur_context == context) return node_context_hash[hash];
        hash = (hash + 1) % node_context_hash_size;
        hash_iter++;
        if (hash_iter >= node_context_hash_size)
        {
            printf("The node context hash table is full!\n");
            exit(1);
        }
    }
    return -1;
}

// Adds a node to the node list
long long AddNodeToList(char *node_str)
{
    long long hash;
    long long length = strlen(node_str) + 1;
    if (length > MAX_STRING) length = MAX_STRING;
    node_list[node_list_size].node_str = (char *)calloc(length, sizeof(char));
    if (node_list[node_list_size].node_str == NULL)
    {
        printf("Memory allocation failed\n");
        exit(1);
    }
    strcpy(node_list[node_list_size].node_str, node_str);
    node_list[node_list_size].cn = 1;
    node_list_size++;
    // Reallocate memory if needed
    if (node_list_size >= node_list_max_size)
    {
        node_list_max_size += 10000;
        node_list = (struct Node *)realloc(node_list, node_list_max_size * sizeof(struct Node));
        if (node_list == NULL)
        {
            printf("Memory allocation failed\n");
            exit(1);
        }
    }
    hash = GetNodeHash(node_str);
    while (node_hash[hash] != -1) hash = (hash + 1) % node_hash_size;
    node_hash[hash] = node_list_size - 1;
    return node_list_size - 1;
}

//Add node context pair to the node context list
long long AddNodeContextToList(long long node, long long context)
{
    long long hash;
    long long hash_iter = 0;
    node_context_list[node_context_list_size].source = node;
    node_context_list[node_context_list_size].target = context;
    node_context_list[node_context_list_size].cn = 1;
    node_context_list_size++;
    if (node_context_list_size >= node_context_list_max_size)
    {
        node_context_list_max_size += 100 * node_list_size;
        node_context_list = (struct Node_Context *)realloc(node_context_list, node_context_list_max_size * sizeof(struct Node_Context));
        if (node_context_list == NULL)
        {
            printf("Memory allocation failed\n");
            exit(1);
        }
    }
    hash = GetNodeContextHash(node_list[node].node_str, node_list[context].node_str);
    while (node_context_hash[hash] != -1)
    {
        hash = (hash + 1) % node_context_hash_size;
        hash_iter++;
        if (hash_iter >= node_context_hash_size)
        {
            printf("The node context hash table is full!\n");
            exit(1);
        }
    }
    node_context_hash[hash] = node_context_list_size - 1;
    return node_context_list_size - 1;
}

void LearnNodeListFromTrainFile()
{
    char node_str[MAX_STRING];
    FILE *fin;
    long long a, i;
    for (a = 0; a < node_hash_size; a++) node_hash[a] = -1;
    fin = fopen(train_file, "r");
    if (fin == NULL)
    {
        printf("ERROR: training data file not found!\n");
        exit(1);
    }
    node_list_size = 0;
    printf("Reading nodes...\n");
    while (1)
    {
        ReadNode(node_str, fin);
        if (feof(fin)) break;
        node_occur_num++;
        if (node_occur_num % 100000 == 0)
        {
            printf("Read nodes: %lldK%c", node_occur_num / 1000, 13);
            fflush(stdout);
        }
        i = SearchNode(node_str);
        if (i == -1) AddNodeToList(node_str);
        else node_list[i].cn++;
    }
    printf("Node list size: %lld \n", node_list_size);
    printf("Node occurrence times in train file: %lld\n", node_occur_num);
    fclose(fin);
}

void GetNodeContextTable()
{
    char node_str[MAX_STRING];
    long long a, b, c, node, context_node, walk_length = 0, walk_position = 0;
    long long walk[MAX_WALK_LENGTH + 1];
    long long line_num = 0;
    long long end_flag;
    FILE *fi = fopen(train_file, "r");
    node_context_hash = (long long *)malloc(node_context_hash_size * sizeof(long long));
    node_context_list_max_size = 1000 * node_list_size;
    node_context_list = (struct Node_Context *)malloc(node_context_list_max_size * sizeof(struct Node_Context));
    for (a = 0; a < node_context_hash_size; a++) node_context_hash[a] = -1;
    node_context_list_size = 0;
    printf("Collecting node context pair...\n");
    start = clock();
    while (1)
    {
        if (walk_length == 0)
        {
            while (1)
            {
                end_flag = ReadNode(node_str, fi);
                if (feof(fi)) break;
                node = SearchNode(node_str);
                //if (node == -1) continue;
                walk[walk_length] = node;
                walk_length++;
                if (end_flag || walk_length >= MAX_WALK_LENGTH) break;
            }
            walk_position = 0;
            line_num++;
            if (line_num % 1000 == 0)
            {
                printf("Processed lines: %lldK%c", line_num / 1000, 13);
                fflush(stdout);
            }
        }
        if (feof(fi) && walk_length == 0) break;
        //if (walk_length == 0) continue;
        node = walk[walk_position]; //current node
        for (a = 0; a < window * 2 + 1; a++)
        {
            if (a != window)
            {
                c = walk_position - window + a;
                if (c < 0) continue;
                if (c >= walk_length) continue;
                context_node = walk[c];
                b = SearchNodeContextPair(node, context_node);
                if (b == -1) AddNodeContextToList(node, context_node);
                else node_context_list[b].cn++;
            }
        }
        walk_position++;
        if (walk_position >= walk_length) walk_length = 0;
    }
    finish = clock();
    printf("Total time: %lf secs for collecting node context pairs\n", (double)(finish - start) / CLOCKS_PER_SEC);
    printf("Node context pair number: %lld\n", node_context_list_size);
    printf("----------------------------------------------------\n");
}

void InitNet()
{
    long long a, b;
    unsigned long long next_random = 1;
    syn0 = (double *)malloc(node_list_size * layer1_size * sizeof(double));
    if (syn0 == NULL)
    {
        printf("Memory allocation failed\n");
        exit(1);
    }
    for (a = 0; a < node_list_size; a++)
        for (b = 0; b < layer1_size; b++)
        {
            next_random = next_random * (unsigned long long)25214903917 + 11;
            syn0[a * layer1_size + b] = (((next_random & 0xFFFF) / (double)65536) - 0.5) / layer1_size;
        }
    syn1neg = (double *)malloc(node_list_size * layer1_size * sizeof(double));
    if (syn1neg == NULL)
    {
        printf("Memory allocation failed\n");
        exit(1);
    }
    for (a = 0; a < node_list_size; a++)
        for (b = 0; b < layer1_size; b++)
            syn1neg[a * layer1_size + b] = 0;
    InitUnigramTable();
}

/* The alias sampling algorithm, which is used to sample an node context pair in O(1) time. */
void InitAliasTable()
{
    long long k;
    double sum = 0;
    long long cur_small_block, cur_large_block;
    long long num_small_block = 0, num_large_block = 0;
    double *norm_prob;
    long long *large_block;
    long long *small_block;
    alias = (long long *)malloc(node_context_list_size * sizeof(long long));
    prob = (double *)malloc(node_context_list_size * sizeof(double));
    if (alias == NULL || prob == NULL)
    {
        printf("Memory allocation failed\n");
        exit(1);
    }
    norm_prob = (double*)malloc(node_context_list_size * sizeof(double));
    large_block = (long long*)malloc(node_context_list_size * sizeof(long long));
    small_block = (long long*)malloc(node_context_list_size * sizeof(long long));
    if (norm_prob == NULL || large_block == NULL || small_block == NULL)
    {
        printf("Memory allocation failed\n");
        exit(1);
    }
    for (k = 0; k != node_context_list_size; k++) sum += node_context_list[k].cn;
    for (k = 0; k != node_context_list_size; k++) norm_prob[k] = (double)node_context_list[k].cn * node_context_list_size / sum;
    for (k = node_context_list_size - 1; k >= 0; k--)
    {
        if (norm_prob[k]<1)
            small_block[num_small_block++] = k;
        else
            large_block[num_large_block++] = k;
    }
    while (num_small_block && num_large_block)
    {
        cur_small_block = small_block[--num_small_block];
        cur_large_block = large_block[--num_large_block];
        prob[cur_small_block] = norm_prob[cur_small_block];
        alias[cur_small_block] = cur_large_block;
        norm_prob[cur_large_block] = norm_prob[cur_large_block] + norm_prob[cur_small_block] - 1;
        if (norm_prob[cur_large_block] < 1)
            small_block[num_small_block++] = cur_large_block;
        else
            large_block[num_large_block++] = cur_large_block;
    }
    while (num_large_block) prob[large_block[--num_large_block]] = 1;
    while (num_small_block) prob[small_block[--num_small_block]] = 1;
    free(norm_prob);
    free(small_block);
    free(large_block);
}

long long SampleAPair(double rand_value1, double rand_value2)
{
    long long k = (long long)node_context_list_size * rand_value1;
    return rand_value2 < prob[k] ? k : alias[k];
}

void TrainModel()
{
    long a, b, c, d;
    long long node, context_node, cur_pair;
    long long count = 0, last_count = 0;
    long long l1, l2, target, label;
    long long type_Id;
    unsigned long long next_random = 1;
    double rand_num1, rand_num2;
    double f, g;
    double *neu1e = (double *)calloc(layer1_size, sizeof(double));
    FILE *fp;
    InitNet();
    starting_alpha = alpha;
    printf("Skip-Gram model with Negative Sampling:");
    if (pp == 1) printf(" heterogeneous version\n");
    else printf(" homogeneous version\n");
    printf("Training file: %s\n", train_file);
    printf("Samples: %lldM\n", total_samples / 1000000);
    printf("Dimension: %lld\n", layer1_size);
    printf("Initial Alpha: %f\n", alpha);
    start = clock();
    srand((unsigned)time(NULL));
    while (1)
    {
        if (count >= total_samples) break;
        if (count - last_count > 10000)
        {
            last_count = count;
            printf("Alpha: %f Progress %.3lf%%%c", alpha, (double)count / (double)(total_samples + 1) * 100, 13);
            fflush(stdout);
            alpha = starting_alpha * (1 - (double)count / (double)(total_samples + 1));
            if (alpha < starting_alpha * 0.0001) alpha = starting_alpha * 0.0001;
        }
        rand_num1 = rand() / (RAND_MAX * 1.0 + 1);
        rand_num2 = rand() / (RAND_MAX * 1.0 + 1);
        cur_pair = SampleAPair(rand_num1, rand_num2);
        node = node_context_list[cur_pair].source;
        context_node = node_context_list[cur_pair].target;
        l1 = node * layer1_size;
        for (c = 0; c < layer1_size; c++) neu1e[c] = 0;

        // NEGATIVE SAMPLING
        for (d = 0; d < negative + 1; d++)
        {
            if (d == 0)
            {
                target = context_node;
                label = 1;
            }
            else
            {
                next_random = next_random * (unsigned long long)25214903917 + 11;
                if (pp == 1)
                {
                    type_Id = GetTypeId(node_list[context_node].node_str[0]);
                    target = type_tables[type_Id][(next_random >> 16) % type_counts[type_Id]];
                }
                else
                    target = table[(next_random >> 16) % table_size];
                if (target == context_node) continue;
                label = 0;
            }
            l2 = target * layer1_size;
            f = 0;
            for (c = 0; c < layer1_size; c++) f += syn0[c + l1] * syn1neg[c + l2];
            if (f > MAX_EXP) f = 1;
            else if (f < -MAX_EXP) f = 0;
            else f = expTable[(int)((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))];
            g = (label - f) * alpha;
            // Propagate errors output -> hidden
            for (c = 0; c < layer1_size; c++) neu1e[c] += g * syn1neg[c + l2];
            // Learn weights hidden -> output
            for (c = 0; c < layer1_size; c++) syn1neg[c + l2] += g * syn0[c + l1];
        }
        // Learn weights input -> hidden
        for (c = 0; c < layer1_size; c++) syn0[c + l1] += neu1e[c];
        count++;
    }
    finish = clock();
    printf("Total time: %lf secs for learning node embeddings\n", (double)(finish - start) / CLOCKS_PER_SEC);
    printf("----------------------------------------------------\n");
    fp = fopen(output_file, "w");
    // Save the learned node representations
    for (a = 0; a < node_list_size; a++)
    {
        if (node_list[a].node_str[0] != prefixes[obj_type]) continue;
        fprintf(fp, "%s", node_list[a].node_str);
        for (b = 0; b < layer1_size; b++) fprintf(fp, " %lf", syn0[a * layer1_size + b]);
        fprintf(fp, "\n");
    }
    free(neu1e);
    fclose(fp);
}

int ArgPos(char *str, int argc, char **argv)
{
    int a;
    for (a = 1; a < argc; a++) if (!strcmp(str, argv[a]))
        {
            if (a == argc - 1)
            {
                printf("Argument missing for %s\n", str);
                exit(1);
            }
            return a;
        }
    return -1;
}

int main(int argc, char **argv)
{
    int i;
    if (argc == 1)
    {
        printf("---randomWalk2vec---\n");
        printf("---The code and following instructions are built upon word2vec.c by Mikolov et al.---\n\n");
        printf("Options:\n");
        printf("Parameters for training:\n");
        printf("\t-train <file>\n");
        printf("\t\tUse random walk sequences from <file> to train the model\n");
        printf("\t-output <file>\n");
        printf("\t\tUse <file> to save the learned node vector-format representations\n");
        printf("\t-size <int>\n");
        printf("\t\tSet the dimension of learned node representations; default is 128\n");
        printf("\t-window <int>\n");
        printf("\t\tSet the window size for collecting node context pairs; default is 5\n");
        printf("\t-pp <int>\n");
        printf("\t\tUse Heterogeneous Skip-Gram model (the ++ version) or not; default is 1 (++ version); otherwise, use 0 (Homogeneous Skip-Gram)\n");
        printf("\t-prefixes <string>\n");
        printf("\t\tPrefixes of node Ids for specifying node types, e.g., ap with a for author and p for paper\n");
        printf("\t-objtype\n");
        printf("\t\tThe index of the objective node type in the prefixes list, for which representations to be learned\n");
        printf("\t-alpha <float>\n");
        printf("\t\tSet the starting learning rate; default is 0.025\n");
        printf("\t-negative <int>\n");
        printf("\t\tNumber of negative examples; default is 5, common values are 3 - 10\n");
        printf("\t-samples <int>\n");
        printf("\t\tSet the number of iterations for stochastic gradient descent as <int> Million; default is 100\n");
        return 0;
    }
    if ((i = ArgPos((char *)"-size", argc, argv)) > 0) layer1_size = atoi(argv[i + 1]);
    if ((i = ArgPos((char *)"-train", argc, argv)) > 0) strcpy(train_file, argv[i + 1]);
    if ((i = ArgPos((char *)"-alpha", argc, argv)) > 0) alpha = atof(argv[i + 1]);
    if ((i = ArgPos((char *)"-output", argc, argv)) > 0) strcpy(output_file, argv[i + 1]);
    if ((i = ArgPos((char *)"-window", argc, argv)) > 0) window = atoi(argv[i + 1]);
    if ((i = ArgPos((char *)"-pp", argc, argv)) > 0) pp = atoi(argv[i + 1]);
    if ((i = ArgPos((char *)"-prefixes", argc, argv)) > 0) strcpy(prefixes, argv[i + 1]);
    if ((i = ArgPos((char *)"-objtype", argc, argv)) > 0) obj_type = atoi(argv[i + 1]);
    if ((i = ArgPos((char *)"-negative", argc, argv)) > 0) negative = atoi(argv[i + 1]);
    if ((i = ArgPos((char *)"-samples", argc, argv)) >0) total_samples = atoi(argv[i + 1]);
    total_samples = total_samples * 1000000;
    type_num = strlen(prefixes);
    printf("Number of node types: %lld\n", type_num);
    node_list = (struct Node *)calloc(node_list_max_size, sizeof(struct Node));
    node_hash = (long long *)calloc(node_hash_size, sizeof(long long));
    expTable = (double *)malloc((EXP_TABLE_SIZE + 1) * sizeof(double));
    if (node_list == NULL || node_hash == NULL || expTable == NULL)
    {
        printf("Memory allocation failed\n");
        exit(1);
    }
    for (i = 0; i < EXP_TABLE_SIZE; i++)
    {
        expTable[i] = exp((i / (double)EXP_TABLE_SIZE * 2 - 1) * MAX_EXP); // Precompute the exp() table
        expTable[i] = expTable[i] / (expTable[i] + 1);                   // Precompute f(x) = x / (x + 1)
    }
    LearnNodeListFromTrainFile();
    GetNodeContextTable();
    InitAliasTable();
    TrainModel();
    return 0;
}
