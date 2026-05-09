cmake_minimum_required(VERSION 3.10.2)

project("strapxml")

include_directories(${CMAKE_CURRENT_SOURCE_DIR})

add_library(
        optimizer-lib
        SHARED
        optimizer-lib.cpp)

target_include_directories(
        optimizer-lib 
        PRIVATE 
        ${CMAKE_CURRENT_SOURCE_DIR}/eigen)

find_library(
        log-lib
        log)

target_link_libraries(
        optimizer-lib
        ${log-lib})