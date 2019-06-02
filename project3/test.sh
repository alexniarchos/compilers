#!/bin/bash

cd ./testcases
for file in *.java
do
    echo "Compiling $file"
    javac $file
    name=`echo "$file" | cut -d'.' -f1`
    java $name > $name.java.out
done

cd ../out
for file in *.ll
do
    echo "Compiling $file"
    name=`echo "$file" | cut -d'.' -f1`
    /home/users/thp06/clang/clang -o $name.ll.bin $file
    ./$name.ll.bin > ./$name.ll.out
done

cd ../testcases
for file in *.java.out
do
    name=`echo "$file" | cut -d'.' -f1`
    echo "Comparing $file ../out/$name.ll.out"
    cmp $file ../out/$name.ll.out
done