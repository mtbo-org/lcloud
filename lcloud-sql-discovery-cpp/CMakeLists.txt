cmake_minimum_required(VERSION 3.31)
project(lcloud_sql_discovery_cpp LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake/Modules/)

option(CodeCoverage "CodeCoverage" OFF)

if (CodeCoverage MATCHES ON)
    message(STATUS "Enable Code Coverage")
    include(CodeCoverage)
endif ()

#set(CMAKE_CXX_STANDARD_LIBRARIES "-static-libgcc -static-libstdc++ -lwsock32 -lws2_32 ${CMAKE_CXX_STANDARD_LIBRARIES}")
#set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,-Bstatic,--whole-archive -lwinpthread -Wl,--no-whole-archive")

find_package(rxcpp CONFIG REQUIRED)
find_package(libpqxx CONFIG REQUIRED)


add_library(lcloud_sql_discovery
        src/discovery.cpp
        src/discovery.h
        src/repo/database.cpp
        src/repo/database.h
        src/repo/instances.cpp
        src/repo/instances.h
)

target_link_libraries(lcloud_sql_discovery PRIVATE rxcpp libpqxx::pqxx)
target_include_directories(lcloud_sql_discovery PUBLIC rxcpp libpqxx::pqxx)

if (CodeCoverage MATCHES ON)
    setup_target_for_coverage(${PROJECT_NAME}_coverage unit_tests coverage)
endif ()

add_subdirectory(test)
add_subdirectory(example)
