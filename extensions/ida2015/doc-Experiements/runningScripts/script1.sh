#!/bin/bash
java -Xmx5000M  -cp "./*"  moa.DoTask EvaluatePrequential -l \(bayes.amidstModels -d GLOBAL -w 100 -v 0.1\) -s \(ArffFileStream -f \($1\)\) -f 1000 >& $0_global_w100_v0.1.txt &
java -Xmx5000M  -cp "./*"  moa.DoTask EvaluatePrequential -l \(bayes.amidstModels -d LOCAL -w 100 -v 0.1\) -s \(ArffFileStream -f \($1\)\) -f 1000 >& $0_local_w100_v0.1.txt &