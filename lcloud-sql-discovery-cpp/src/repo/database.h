// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

//
// Created by bekamk on 03.06.2025.
//

#ifndef DATABASE_H_
#define DATABASE_H_
#include <functional>
#include <memory>
#include <pqxx/result>

namespace pqxx {
class row;
class connection;
class result;
}  // namespace pqxx

namespace lcloud {
class Result {};

template <typename ResultT, typename RowT = typename ResultT::reference>
class result_reader {
 public:
  explicit result_reader(const ResultT& result) : result(std::move(result)) {}

  typedef ResultT result_t;
  typedef RowT row_t;

  [[nodiscard]] size_t affected_rows() const;

  [[nodiscard]] typename ResultT::const_iterator cbegin() const {
    return result.cbegin();
  }

  [[nodiscard]] typename ResultT::const_iterator cend() const {
    return result.cend();
  }

  [[nodiscard]] typename ResultT::const_iterator begin() {
    return result.cbegin();
  }

  [[nodiscard]] typename ResultT::const_iterator end() { return result.cend(); }

 protected:
  const ResultT result;
};

template <class ResultReaderT, typename RowT = typename ResultReaderT::row_t>
class database {
 public:
  database() noexcept(false) = default;

  virtual ~database() = default;

  virtual std::shared_ptr<ResultReaderT> exec(
      const std::string& query_string) noexcept(false) = 0;

  virtual std::shared_ptr<ResultReaderT> exec(
      const std::string& query_string,
      const pqxx::params& params) noexcept(false) = 0;

  virtual std::string read_string(const RowT& row, const char* str) = 0;
};

template <class ResultReaderT, typename RowT = typename ResultReaderT::row_t>
extern std::function<std::shared_ptr<database<ResultReaderT, RowT>>()>
    create_database;

std::shared_ptr<database<result_reader<pqxx::result>>> create_postgres_database(
    const std::string& connection_string);
}  // namespace lcloud

#endif  // DATABASE_H_
