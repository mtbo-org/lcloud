cmake_minimum_required(VERSION 3.31)
project(lcloud_sql_discovery_example LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_executable(lcloud_sql_discovery_example main.cpp)

target_include_directories(lcloud_sql_discovery_example PRIVATE
        ../src
)

target_link_libraries(lcloud_sql_discovery_example PRIVATE
        lcloud_sql_discovery
        rxcpp
        libpqxx::pqxx
)