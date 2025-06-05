// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

//
// Created by bekamk on 03.06.2025.
//

#ifndef INSTANCES_H_
#define INSTANCES_H_
#include <chrono>
#include <functional>
#include <list>
#include <memory>
#include <string>

#include "database.h"

namespace lcloud {
template <class ResultReaderT, typename RowT>
class database;

class instances {
 public:
  explicit instances(std::string service) { service_ = std::move(service); }

  virtual ~instances() = default;

  virtual void initialize() = 0;

  virtual std::list<std::string> get_all(
      const std::chrono::milliseconds& interval) = 0;

  virtual void add(const std::string& instance) = 0;

 protected:
  std::string service_;
};

template <class ResultReaderT, typename RowT = typename ResultReaderT::row_t>
class db_instances : public instances {
 public:
  db_instances(const std::string& service,
               std::shared_ptr<database<ResultReaderT, RowT>> database)
      : instances(service), database_(std::move(database)) {}

  virtual std::string init_query_string() = 0;

  virtual std::string get_all_query_string(
      const std::chrono::milliseconds& interval) = 0;

  virtual std::string update_query_string(const std::string& instance_name) = 0;

  virtual std::string add_query_string(const std::string& instance_name) = 0;

  void initialize() noexcept(false) override {
    database_->exec(init_query_string());
  }

  std::list<std::string> get_all(
      const std::chrono::milliseconds& interval) override {
    const auto result_reader =
        database_->exec(get_all_query_string(interval), pqxx::params{service_});

    if (result_reader == nullptr) {
      return {};
    }

    auto results = std::list<std::string>();

    for (const auto& row : *result_reader) {
      results.emplace_back(database_->read_string(row, "name"));
    }

    return results;
  }

  void add(const std::string& instance) override {
    const auto result_reader = database_->exec(
        update_query_string(instance), pqxx::params{service_, instance});

    auto affected_rows = result_reader ? result_reader->affected_rows() : 0;

    if (affected_rows < 1) {
      database_->exec(add_query_string(instance),
                      pqxx::params{service_, instance});
    }
  }

 protected:
  const std::shared_ptr<database<ResultReaderT, RowT>> database_;
};

template <class ResultReaderT, typename RowT = typename ResultReaderT::row_t>
extern std::function<std::shared_ptr<instances>(std::string service)>
    create_instances;

template <class ResultReaderT, typename RowT = typename ResultReaderT::row_t>
extern std::function<std::shared_ptr<db_instances<ResultReaderT, RowT>>(
    std::string service,
    std::shared_ptr<database<ResultReaderT, RowT>> database)>
    create_db_instances;

std::shared_ptr<instances> create_postgres_instances(
    const std::string& service);
}  // namespace lcloud

#endif  // INSTANCES_H_
