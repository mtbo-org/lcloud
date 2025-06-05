// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

#include "database.h"

#include <iostream>
#include <pqxx/pqxx>

class postgres_database final
    : public lcloud::database<lcloud::result_reader<pqxx::result>, pqxx::row> {
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
    pqxx::work tx{*connection_};

    try {
      auto r = std::make_shared<lcloud::result_reader<pqxx::result>>(
          params.size() > 0 ? tx.exec(query_string, params)
                            : tx.exec(query_string));
      tx.commit();
      return r;
    } catch (pqxx::sql_error const& e) {
      std::cerr << "SQL error: " << e.what() << std::endl;
      std::cerr << "Query was: " << e.query() << std::endl;
      return nullptr;
    } catch (std::exception const& e) {
      std::cerr << "Error: " << e.what() << std::endl;
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
