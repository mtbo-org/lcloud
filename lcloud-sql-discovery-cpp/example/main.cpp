// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

#include <discovery.h>
#include <repo/instances.h>

#include <csignal>
#include <cstdio>
#include <iostream>

namespace rxu = rxcpp::util;

typedef lcloud::result_reader<pqxx::result> reader_t;

void DI(const std::string& connection_string) {
  lcloud::create_database<reader_t> = [connection_string] {
    return lcloud::create_postgres_database(connection_string);
  };

  lcloud::create_instances<reader_t> = [](const std::string& service) {
    return lcloud::create_postgres_instances(service);
  };
}

namespace {
std::function<void(int)> shutdown_handler;
void signal_handler(const int signal) { shutdown_handler(signal); }
}  // namespace

static std::string to_str(
    const std::chrono::high_resolution_clock::time_point& tp) {
  const auto millis = std::chrono::duration_cast<std::chrono::milliseconds>(
                          tp.time_since_epoch())
                          .count() %
                      1000;

  const std::time_t t = std::chrono::system_clock::to_time_t(tp);
  const auto now = std::localtime(&t);

  char buffer[256];
  sprintf(buffer, "%04d-%02d-%02d %02d:%02d:%02d.%03ld", now->tm_year + 1900,
          now->tm_mon + 1, now->tm_mday, now->tm_hour, now->tm_min, now->tm_sec,
          millis);

  return buffer;
}

int main() {
  const auto connection_string =
      std::string(std::getenv("CONNECTION_STRING")
                      ? std::getenv("CONNECTION_STRING")
                      : "postgresql://user:user@localhost:5432/demo");

  const auto service_name = std::string(
      std::getenv("SERVICE_NAME") ? std::getenv("SERVICE_NAME") : "lcloud");

  const auto instance_name = std::string(
      std::getenv("HOSTNAME") ? std::getenv("HOSTNAME") : "localhost");

  DI(connection_string);

  const auto discovery = lcloud::create_postgres_discovery(
      service_name, instance_name, std::chrono::milliseconds(500));

  discovery->initialize();

  std::cout << "DB initialized\n";

  const auto ping_subscription =
      discovery->ping().subscribe([](bool) {},
                                  [](std::exception_ptr ep) {
                                    try {
                                      std::rethrow_exception(std::move(ep));
                                    } catch (const std::exception& ex) {
                                      printf("Ping OnError: %s\n", ex.what());
                                    }
                                  },
                                  [] { printf("Ping OnCompleted\n"); });

  const auto lookup_subscription = discovery->lookup().subscribe(
      [&instance_name, &service_name](lcloud::discovery::lookup_t lookup) {
        std::string s;
        for (const auto& v : lookup) {
          s += v + "\n";
        }

        printf(R"(
*************************************************************
%.24s   %14s   %14s   %-2d
*************************************************************
%s*************************************************************
)",
               to_str(std::chrono::high_resolution_clock::now()).c_str(),
               service_name.c_str(), instance_name.c_str(),
               static_cast<int>(lookup.size()), s.c_str());
      },
      [](std::exception_ptr ep) {
        try {
          std::rethrow_exception(std::move(ep));
        } catch (const std::exception& ex) {
          printf("Lookup OnError: %s\n", ex.what());
        }
      },
      [] { printf("Lookup OnCompleted\n"); });

  std::signal(SIGINT, signal_handler);
  std::atomic_bool running{true};

  shutdown_handler = [&](int _) {
    std::cout << "Discovery shutdown...\n";
    ping_subscription.unsubscribe();
    lookup_subscription.unsubscribe();
    running = false;
    running.notify_all();
  };

  running.wait(true);

  std::cout << "Exit\n";

  return 0;
}
