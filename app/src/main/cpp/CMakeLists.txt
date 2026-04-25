# 안드로이드 스튜디오가 C++ 코드를 어떻게 빌드할지 알려주는 설명서
cmake_minimum_required(VERSION 3.10.2)

project("strapxml")

add_library(
        optimizer-lib
        SHARED
        optimizer-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        optimizer-lib
        ${log-lib})