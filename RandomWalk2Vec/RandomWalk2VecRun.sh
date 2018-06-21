#!/bin/bash
./randomWalk2vec -size 128 -train DBLP-V3.meta.graph.random.walk.txt -output DBLP-V3.meta.graph.pp1.emb.txt -pp 1 -prefixes apv -objtype 0
./randomWalk2vec -size 128 -train DBLP-V3.meta.graph.random.walk.txt -output DBLP-V3.meta.graph.pp0.emb.txt -pp 0 -prefixes apv -objtype 0
./randomWalk2vec -size 128 -train DBLP-V3.meta.path.random.walk.apapa.txt -output DBLP-V3.meta.path.apapa.pp1.emb.txt -pp 1 -prefixes apv -objtype 0 
./randomWalk2vec -size 128 -train DBLP-V3.meta.path.random.walk.apapa.txt -output DBLP-V3.meta.path.apapa.pp0.emb.txt -pp 0 -prefixes apv -objtype 0
./randomWalk2vec -size 128 -train DBLP-V3.meta.path.random.walk.apvpa.txt -output DBLP-V3.meta.path.apvpa.pp1.emb.txt -pp 1 -prefixes apv -objtype 0
./randomWalk2vec -size 128 -train DBLP-V3.meta.path.random.walk.apvpa.txt -output DBLP-V3.meta.path.apvpa.pp0.emb.txt -pp 0 -prefixes apv -objtype 0
./randomWalk2vec -size 128 -train DBLP-V3.meta.path.random.walk.mix.txt -output DBLP-V3.meta.path.mix.pp1.emb.txt -pp 1 -prefixes apv -objtype 0
./randomWalk2vec -size 128 -train DBLP-V3.meta.path.random.walk.mix.txt -output DBLP-V3.meta.path.mix.pp0.emb.txt -pp 0 -prefixes apv -objtype 0
./randomWalk2vec -size 128 -train DBLP-V3.uniform.random.walk.txt -output DBLP-V3.uniform.pp1.emb.txt -pp 1 -prefixes apv -objtype 0
./randomWalk2vec -size 128 -train DBLP-V3.uniform.random.walk.txt -output DBLP-V3.uniform.pp0.emb.txt -pp 0 -prefixes apv -objtype 0
