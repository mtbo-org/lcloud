FROM ubuntu:plucky-20250521 AS distro

RUN apt-get update && \
  apt-get install -y libpq-dev

FROM distro AS build

RUN apt-get install -y cmake gcc g++

ADD ./RxCpp /app/RxCpp
ADD ./libpqxx /app/libpqxx
ADD ./googletest /app/googletest

WORKDIR /app/RxCpp/build
RUN cmake -G"Unix Makefiles" -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
    -DCMAKE_C_COMPILER=gcc -DCMAKE_CXX_COMPILER=g++ \
    -DCMAKE_BUILD_TYPE=Release -DRXCPP_DISABLE_TESTS_AND_EXAMPLES=1 ../projects/CMake
RUN make -j4
RUN make install


RUN apt-get install -y libpq-dev lcov gcovr
WORKDIR /app/libpqxx/build
RUN cmake -G"Unix Makefiles" -DCMAKE_C_FLAGS="-DPQXX_PQ_STATIC=1" -DCMAKE_CXX_FLAGS="-DPQXX_PQ_STATIC=1" \
    -DSKIP_BUILD_TEST=1 -DCMAKE_C_COMPILER=gcc -DCMAKE_CXX_COMPILER=g++ ..
RUN make -j4
RUN make install

WORKDIR /app/googletest/build
RUN cmake ..
RUN make -j4
RUN make install

# Скопируем директорию /src в контейнер
ADD ./CMakeLists.txt /app/CMakeLists.txt
ADD ./src /app/src
ADD ./test /app/test
ADD ./example /app/example
ADD ./cmake /app/cmake

# Установим рабочую директорию для сборки проекта
WORKDIR /app/build

# Выполним сборку нашего проекта, а также его тестирование
RUN cmake -DCMAKE_C_FLAGS="-DPQXX_PQ_STATIC=1" -DCMAKE_CXX_FLAGS="-DPQXX_PQ_STATIC=1" ..
RUN make -j4
RUN test/unit_tests

# Запуск ---------------------------------------

# В качестве базового образа используем ubuntu:plucky
FROM distro

RUN groupadd -r user && useradd -r -g user user
USER user

# Установим рабочую директорию нашего приложения
WORKDIR /app

# Скопируем приложение со сборочного контейнера в рабочую директорию
COPY --from=build /app/build/example/lcloud_sql_discovery_example .


# Установим точку входа
ENTRYPOINT ["./lcloud_sql_discovery_example"]