// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// export PATH=/c/Program Files/Microsoft Visual
// Studio/2022/Community/VC/Tools/MSVC/14.39.33519/bin/Hosts64/x64:$PATH
//-Dlibpqxx_DIR=D:\Programs\libpqxx\lib\cmake\libpqxx
//-DPostgreSQL_ROOT=D:\Programs\postgres-dev
//-DGTest_DIR=D:\Programs\googletest\lib\cmake\GTest
//-Drxcpp_DIR=D:\Programs\RxCpp\share\rxcpp\cmake

#include <discovery.h>
#include <repo/instances.h>

#include <csignal>
#include <cstdio>
#include <iostream>

typedef lcloud::result_reader<pqxx::result> reader_t;

void DI() {
  lcloud::create_database<reader_t> = [] {
    return lcloud::create_postgres_database(
        "postgresql://user:user@localhost:5432/demo");
  };

  lcloud::create_instances<reader_t> = [](const std::string& service) {
    return lcloud::create_postgres_instances(service);
  };
}

namespace {
std::function<void(int)> shutdown_handler;
void signal_handler(const int signal) {
  shutdown_handler(signal);
}
}  // namespace

int main() {
  DI();

  const auto discovery = lcloud::create_postgres_discovery("lcloud", "aaa");

  discovery->initialize();

  std::cout << "DB initialized\n";

  const auto ping_subscription = discovery->ping();

  std::signal(SIGINT, signal_handler);
  std::atomic_bool running{true};

  shutdown_handler = [&](int _) {
    std::cout << "Discovery shutdown...\n";
    ping_subscription.unsubscribe();
    running = false;
    running.notify_all();
  };

  running.wait(true);

  std::cout << "Exit\n";

  return 0;
}
