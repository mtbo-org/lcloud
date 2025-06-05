// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

#include "database.h"

#include <iostream>
#include <pqxx/pqxx>

class postgres_database final
    : public lcloud::database<lcloud::result_reader<pqxx::result>> {
  std::mutex mutex;

 public:
  explicit postgres_database(const std::string& connection_string) noexcept(
      false) {
    connection_ = std::make_shared<pqxx::connection>(connection_string);
  }

  std::shared_ptr<lcloud::result_reader<pqxx::result>> exec(
      const std::string& query_string) noexcept(false) override {
    return exec(query_string, pqxx::params{});
  }

  std::shared_ptr<lcloud::result_reader<pqxx::result>> exec(
      const std::string& query_string,
      const pqxx::params& params) noexcept(false) override {
    std::lock_guard lock(mutex);

    pqxx::work tx{*connection_};

    try {
      auto r = std::make_shared<lcloud::result_reader<pqxx::result>>(
          params.size() > 0 ? tx.exec(query_string, params)
                            : tx.exec(query_string));
      tx.commit();
      return r;
    } catch (pqxx::plpgsql_error const& e) {
      std::cerr << "PL SQL error: " << e.what() << std::endl;
      std::cerr << "PL Query was: " << e.query() << std::endl;
      return nullptr;
    } catch (pqxx::sql_error const& e) {
      std::cerr << "SQL error: " << e.what() << std::endl;
      std::cerr << "Query was: " << e.query() << std::endl;
      return nullptr;
    } catch (std::exception const& e) {
      std::cerr << "DB error: " << e.what() << ", sql: " << query_string
                << std::endl;
      return nullptr;
    }
  }

  std::string read_string(const pqxx::row& row, const char* str) override {
    return row[str].c_str();
  }

 private:
  std::shared_ptr<pqxx::connection> connection_;
};

template <>
std::function<
    std::shared_ptr<lcloud::database<lcloud::result_reader<pqxx::result>>>()>
    lcloud::create_database<lcloud::result_reader<pqxx::result>>;

std::shared_ptr<lcloud::database<lcloud::result_reader<pqxx::result>>>
lcloud::create_postgres_database(const std::string& connection_string) {
  return std::make_shared<postgres_database>(connection_string);
}

template <>
size_t lcloud::result_reader<pqxx::result>::affected_rows() const {
  return result.affected_rows();
}
