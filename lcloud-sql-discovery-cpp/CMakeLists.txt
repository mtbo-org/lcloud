cmake_minimum_required(VERSION 3.31)
project(lcloud_sql_discovery_cpp LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_CXX_STANDARD_LIBRARIES "-static-libgcc -static-libstdc++ -lwsock32 -lws2_32 ${CMAKE_CXX_STANDARD_LIBRARIES}")

set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,-Bstatic,--whole-archive -lwinpthread -Wl,--no-whole-archive")


find_package(GTest CONFIG REQUIRED)

find_package(rxcpp CONFIG REQUIRED)

find_package(libpqxx CONFIG REQUIRED)

add_library(lcloud_sql_discovery
        src/discovery.cpp
        src/discovery.h
        src/repo/database.cpp
        src/repo/database.h
        src/repo/instances.cpp
        src/repo/instances.h
        #        PUBLIC rxcpp::rx
)

add_executable(lcloud_sql_discovery_example example/main.cpp)

target_compile_options(lcloud_sql_discovery PUBLIC ${RX_COMPILE_OPTIONS})
target_compile_features(lcloud_sql_discovery PUBLIC ${RX_COMPILE_FEATURES})

target_link_libraries(lcloud_sql_discovery PRIVATE rxcpp libpqxx::pqxx)
target_include_directories(lcloud_sql_discovery PUBLIC rxcpp libpqxx::pqxx)

MESSAGE(STATUS "RXCPP_DIR: " ${rxcpp})

target_include_directories(
        lcloud_sql_discovery_example PRIVATE
        src
        rxcpp
        libpqxx::pqxx
)

target_link_libraries(
        lcloud_sql_discovery_example
        lcloud_sql_discovery
        rxcpp
        libpqxx::pqxx
)


enable_testing()

add_executable(
        lcloud_test
        test/lcloud-sql-test.cpp
)

target_link_libraries(
        lcloud_test
        lcloud_sql_discovery
        GTest::gtest_main
)

include(GoogleTest)
gtest_discover_tests(lcloud_test)