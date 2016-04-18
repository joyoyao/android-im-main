#!bash

#gradle build

cp ./kit/build/intermediates/bundles/release/classes.jar ./release/libs/kit.jar
cp ./ipc/build/intermediates/bundles/release/classes.jar ./release/libs/ipc.jar
cp ./lib/build/intermediates/bundles/release/classes.jar ./release/libs/lib.jar
cp ./msg/build/intermediates/bundles/release/classes.jar ./release/libs/msg.jar

gradle build
