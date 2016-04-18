#!/bin/sh

ndk-build -C lib/src/main/ DEBUG=true
ndk-build -C kit/src/voip/ DEBUG=true

