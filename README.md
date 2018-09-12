# MetaGraph2Vec

Code for PAKDD-2018 paper "MetaGraph2Vec: Complex Semantic Path Augmented Heterogeneous Network Embedding"

Authors: Daokun Zhang, Jie Yin, Xingquan Zhu and Chengqi Zhang

Contact: Daokun Zhang (daokunzhang2015@gmail.com)

This project contains two folders:

1) The folder "DBLPPreProcess" is the Java project for data preprocessing. It contains three files

	"PreProcessStep1", used to extract subgraph form the "DBLP-Citation-Network-V3.txt" network;

	"PreProcessStep2", used to obtain the class labels for authors;

	"PreProcessStep3", used to generate random walks from the extracted subgraph, including the metagraph guided random walks, metapath guided random walks and uniform random walks that ignore the heterogeneity of nodes.

To run this project, firstly download the "DBLP-Citation-Network-V3.txt" file from https://aminer.org/citation, and move it to the "DBLPPreProcess/dataset" folder, then run "PreProcessStep1", "PreProcessStep2" and "PreProcessStep3" sequentially. The class label file will be output to the folder "DBLPPreProcess/group", and the random walk files will be output to the folder "DBLPPreProcess/randomwalk".

2) The folder "RandomWalk2VecRun" contains the program for learning node embeddings from the generated random walk sequences. Please run "compile.sh" to compile the "randomwalk2vec.c" to the executable file. To run this program, move the generated random walk files from "DBLPPreProcess/randomwalk" to this folder and run the "RandomWalk2VecRun.sh" file.

In this folder, the "randomWalk2vec" program is used to learn node embeddings from random walk sequences with Heterogeneous Skip-Gram or Homogeneous Skip-Gram.

The options of randomWalk2Vec are as follows:

	-train <file>
		Use random walk sequences from <file> to train the model
	-output <file>
		Use <file> to save the learned node vector-format representations
	-size <int>
		Set the dimension of learned node representations; default is 128
	-window <int>
		Set the window size for collecting node context pairs; default is 5
	-pp <int>
		Use Heterogeneous Skip-Gram model (the ++ version) or not; default is 1 (++ version); otherwise, use 0 (Homogeneous Skip-Gram)
	-prefixes <string>
		Prefixes of node Ids for specifying node types, e.g., ap with a for author and p for paper
	-objtype
		The index of the objective node type in the prefixes list, for which representations to be learned
	-alpha <float>
		Set the starting learning rate; default is 0.025
	-negative <int>
		Number of negative examples; default is 5, common values are 3 - 10
	-samples <int>
		Set the number of iterations for stochastic gradient descent as <int> Million; default is 100

If you find this project is useful, please cite this paper:

	@inproceedings{zhang2018metagraph2vec,
		title={MetaGraph2Vec: Complex Semantic Path Augmented Heterogeneous Network Embedding},
		author={Zhang, Daokun and Yin, Jie and Zhu, Xingquan and Zhang, Chengqi},
		booktitle={Pacific-Asia Conference on Knowledge Discovery and Data Mining},
		pages={196--208},
		year={2018},
		organization={Springer}
	}
# Note
The randomWalk2vec.c is compiled on a Linux system with RAND_MAX taking value 2147483647. If you compile randomWalk2vec.c on your system, please carefully check the value of RAND_MAX to make sure it is large enough for the correctness of alias
table sampling.
