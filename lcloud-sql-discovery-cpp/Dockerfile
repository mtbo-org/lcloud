FROM ubuntu:24.04 AS build

RUN apt-get update

RUN apt-get install -y ca-certificates gpg wget

RUN test -f /usr/share/doc/kitware-archive-keyring/copyright || \
    wget -O - https://apt.kitware.com/keys/kitware-archive-latest.asc 2>/dev/null | gpg --dearmor - | tee /usr/share/keyrings/kitware-archive-keyring.gpg >/dev/null

RUN echo 'deb [signed-by=/usr/share/keyrings/kitware-archive-keyring.gpg] https://apt.kitware.com/ubuntu/ noble main' | tee /etc/apt/sources.list.d/kitware.list >/dev/null

RUN apt-get update

RUN apt-get install -y kitware-archive-keyring

RUN apt-get install -y cmake

RUN apt-get install -y software-properties-common

RUN add-apt-repository ppa:ubuntu-toolchain-r/test

RUN apt-get update

RUN apt-get install -y gcc-14 g++-14

RUN update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-14 14 --slave /usr/bin/g++ g++ /usr/bin/g++-14

ADD ./RxCpp /app/RxCpp
ADD ./libpqxx /app/libpqxx
ADD ./googletest /app/googletest

WORKDIR /app/RxCpp/build
RUN cmake .. -DCMAKE_POLICY_VERSION_MINIMUM=3.5
RUN make -j4
RUN make install


RUN apt-get install -y libpq-dev lcov gcovr
WORKDIR /app/libpqxx/build
RUN cmake -DCMAKE_CXX_FLAGS="-DPQXX_PQ_STATIC" ..
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
RUN cmake ..
RUN make -j4
RUN test/unit_tests

# Запуск ---------------------------------------

# В качестве базового образа используем ubuntu:latest
FROM ubuntu:latest
RUN apt-get update
RUN apt-get install -y libpq-dev
RUN groupadd -r user && useradd -r -g user user
USER user

# Установим рабочую директорию нашего приложения
WORKDIR /app

# Скопируем приложение со сборочного контейнера в рабочую директорию
COPY --from=build /app/build/example/lcloud_sql_discovery_example .

# Установим точку входа
ENTRYPOINT ["./lcloud_sql_discovery_example"]