 rem java -jar build\libs\bugs-classification-v1.jar make.es 70 ./test/data/70/before.txt ./test/data/70/after.txt  ./test/es/70.es

 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  code --algorithm=jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  code --algorithm=ext_jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  code --algorithm=full_jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  code --algorithm=fuz_jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  code --algorithm=vec
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  code --algorithm=bow


 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  ngram --algorithm=jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  ngram --algorithm=ext_jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  ngram --algorithm=full_jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  ngram --algorithm=fuz_jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  ngram --algorithm=vec
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  ngram --algorithm=bow

 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  bitset --algorithm=jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  bitset --algorithm=ext_jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  bitset --algorithm=full_jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  bitset --algorithm=fuz_jac
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  bitset --algorithm=vec
 rem java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  bitset --algorithm=bow


 java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  textngram --algorithm=jac
 java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  textngram --algorithm=ext_jac
 java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  textngram --algorithm=full_jac
 java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  textngram --algorithm=fuz_jac
 java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  textngram --algorithm=vec
 java -jar build\libs\bugs-classification-v1.jar prepare.es  ./test/data   ./test/es  textngram --algorithm=bow
pause