
rem d:\j\bin\java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar prepare.es  D:\Work\CE12800\2020\src_simple   D:\Work\CE12800\2020\result\ngram D:\Work\CE12800\2020\list.txt  ngram  --ngramsize=5
rem d:\j\bin\java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar cluster.es  D:\Work\CE12800\2020\result\ngram  D:\Work\CE12800\2020\result D:\Work\CE12800\2020\list.txt ngram --algorithm=jac 

rem d:\j\bin\java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar prepare.es  D:\Work\Router\2020\src_simple   D:\Work\Router\2020\result\ngram D:\Work\Router\2020\list.txt  ngram  --ngramsize=5
rem d:\j\bin\java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar prepare.es  D:\Work\Router\2020\src_simple   D:\Work\Router\2020\result\ngram D:\Work\Router\2020\list.txt  ngram  --ngramsize=5
rem d:\j\bin\java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar prepare.es  D:\Work\Router\2020\src_simple   D:\Work\Router\2020\result\ngram D:\Work\Router\2020\list.txt  ngram  --ngramsize=5
rem d:\j\bin\java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar prepare.es  D:\Work\Router\2020\src_simple   D:\Work\Router\2020\result\ngram D:\Work\Router\2020\list.txt  ngram  --ngramsize=5
rem d:\j\bin\java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar cluster.es  D:\Work\Router\2020\result\ngram  D:\Work\Router\2020\result D:\Work\Router\2020\list.txt ngram --algorithm=jac 


rem d:\j\bin\java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar prepare.es  D:\Work\WDM\2020\src_simple   D:\Work\WDM\2020\result\ngram D:\Work\WDM\2020\list.txt  ngram  --ngramsize=5
rem d:\j\bin\java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar cluster.es  D:\Work\WDM\2020\result\ngram  D:\Work\WDM\2020\result D:\Work\WDM\2020\list.txt ngram --algorithm=jac 

rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar prepare.es  .\test\dataset   .\test\dataset\result\code .\test\dataset\list.txt  code 
rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar cluster.es  .\test\dataset\result\code .\test\dataset   .\test\dataset\list.txt  code  --algorithm=jac
 rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar prepare.lase  .\test\dataset  .\test\dataset\lase2 .\test\dataset\list.txt concrete 
rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar build.lase    .\test\dataset\lase5 .\test\dataset\cluster_code_jac.txt .\test\dataset\ca concrete 
rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar make.maxforest  .\test\dataset  .\test\dataset\maxtree 60 70 concrete 
rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar make.maxtree  .\test\dataset  .\test\dataset\maxtree 80 70 concrete 

rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar matrix.maxtree  .\test\dataset .\test\dataset\cluster_code_jac.txt .\test\dataset\matrix concrete

 rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar top10.maxtree  .\test\dataset .\test\dataset\list.txt .\test\dataset\top10q  concrete --verbose=yes

rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar looklike  .\test\dataset .\test\dataset\list.txt   .\test\dataset .\test\dataset\top10.txt .\test\dataset\ll concrete 

rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar lase.looklike  .\test\dataset .\test\dataset\list.txt   .\test\dataset .\test\dataset\list.txt .\test\dataset\l.ll concrete --verbose=yes

rem java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar suggestion  .\test\dataset\bad\30\30.c  .\test\dataset .\test\dataset\list.txt .\test\dataset\suggestion --markers=2 --similarity=50 --anynode=true 

java -Xmx12G -Dfile.encoding=UTF-8 -jar build\libs\bugs-classification-v1.jar lase.looklike.cluster  .\test\dataset .\test\dataset\cluster_code_jac.txt .\test\dataset\l.ll.cluster concrete  --markers=2 --anynode=true --verbose=yes