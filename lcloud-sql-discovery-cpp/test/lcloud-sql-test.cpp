// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <algorithm>

#include "../src/repo/database.h"
#include "../src/repo/instances.h"
#include "catch2/catch_all.hpp"
#include "rxcpp/rx-test.hpp"

#pragma region gmock using
using testing::Exactly;
using testing::Invoke;
using testing::IsEmpty;
using testing::NiceMock;
using testing::Not;
using testing::Return;
using testing::ReturnNull;
using testing::StartsWith;
#pragma endregion

#pragma region MockDatabase

typedef std::map<std::string, std::string> test_row_t;
typedef std::list<test_row_t> test_result_t;

typedef lcloud::result_reader<test_result_t> test_result_reader;

template <>
size_t lcloud::result_reader<test_result_t>::affected_rows() const {
  return result.size();
}

typedef lcloud::database<test_result_reader, test_row_t> database_t;

typedef std::shared_ptr<database_t> database_ptr;

// ReSharper disable once CppClassCanBeFinal
class MockDatabase : public database_t {
 public:
  MOCK_METHOD(std::shared_ptr<test_result_reader>, exec,
              (const std::string& query_string), (override));

  MOCK_METHOD(std::shared_ptr<test_result_reader>, exec,
              (const std::string& query_string, const pqxx::params& params),
              (override));

  MOCK_METHOD(std::string, read_string,
              (const test_row_t& row, const char* str), (override));
};
#pragma endregion

#pragma region MockInstances

class MockInstances final
    : public lcloud::db_instances<test_result_reader, test_row_t> {
 public:
  MockInstances(const std::string& service, const database_ptr& database)
      : db_instances(service, database) {}

  std::string init_query_string() override {
    return R"(
create table if not exists instances
(
    id      uuid not null primary key default gen_random_uuid(),
    service varchar(255),
    name    varchar(255),
    last    timestamp
        with time zone default now(),
    constraint instances_uniq
    unique(name, service)
);

create index if not exists last_index
    on instances(last);
)";
  }

  std::string get_all_query_string(
      const std::chrono::milliseconds& interval) override {
    return "select name from instances where service = $1 AND last > now() - "
           "interval '" +
           std::to_string(interval.count()) + " milliseconds'";
  }

  std::string update_query_string(const std::string& instance_name) override {
    return "update instances set last = now() where service = $1 and name = $2";
  }

  std::string add_query_string(const std::string& instance_name) override {
    return "insert into instances (service, name) values ($1, $2)";
  }
};

template <>
std::function<database_ptr()>
    lcloud::create_database<test_result_reader, test_row_t>;

template <>
std::function<
    std::shared_ptr<lcloud::db_instances<test_result_reader, test_row_t>>(
        std::string service, database_ptr database)>
    lcloud::create_db_instances<test_result_reader, test_row_t>;
#pragma endregion

#pragma region DatabaseTestFixture
struct DatabaseTestFixture : testing::Test {
  DatabaseTestFixture() {
    database = std::make_shared<NiceMock<MockDatabase>>();

    ON_CALL(*database, read_string(testing::_, testing::_))
        .WillByDefault(Invoke([](const test_row_t& row, const char* str) {
          return row.at(str);
        }));

    lcloud::create_database<test_result_reader, test_row_t> = [this] {
      return database;
    };

    lcloud::create_db_instances<test_result_reader, test_row_t> =
        [this](const std::string& service, const database_ptr& database) {
          return std::make_shared<MockInstances>(service, database);
        };
  }

  std::shared_ptr<NiceMock<MockDatabase>> database;
};

#pragma endregion

#pragma region Test: Init
TEST_F(DatabaseTestFixture, Init) {
  // ON_CALL(*database, exec(_)).WillByDefault(
  //     Return());

  EXPECT_CALL(*database, exec(testing::_)).Times(Exactly(1));

  const auto instances =
      lcloud::create_db_instances<test_result_reader, test_row_t>("test",
                                                                  database);

  instances->initialize();
}
#pragma endregion

#pragma region Test: GetAllReturnList
TEST_F(DatabaseTestFixture, GetAllReturnList) {
  test_result_t result{
      test_row_t{{"name", "a"}},
      test_row_t{{"name", "b"}},
      test_row_t{{"name", "c"}},
  };

  ON_CALL(*database, exec(testing::_, testing::_))
      .WillByDefault(Return(std::make_shared<test_result_reader>(result)));

  EXPECT_CALL(*database, exec(testing::_, testing::_)).Times(Exactly(1));

  EXPECT_CALL(*database, read_string(testing::_, testing::_))
      .Times(testing::AtLeast(1));

  const auto instances =
      lcloud::create_db_instances<test_result_reader, test_row_t>("test",
                                                                  database);
  const auto all = instances->get_all(std::chrono::milliseconds(1));

  EXPECT_EQ(all, std::list<std::string>({"a", "b", "c"}));
}
#pragma endregion

#pragma region Test: GetAllReturnNothing
TEST_F(DatabaseTestFixture, GetAllReturnNothing) {
  ON_CALL(*database, exec(testing::_)).WillByDefault(Return(nullptr));

  EXPECT_CALL(*database, exec(testing::_, testing::_)).Times(Exactly(1));

  const auto instances =
      lcloud::create_db_instances<test_result_reader, test_row_t>("test",
                                                                  database);
  const auto all = instances->get_all(std::chrono::milliseconds(1));

  EXPECT_EQ(all, std::list<std::string>({}));
}
#pragma endregion

#pragma region Test: AddNewInstance
TEST_F(DatabaseTestFixture, AddNewInstance) {
  ON_CALL(*database, exec(StartsWith("update"))).WillByDefault(Return(nullptr));

  ON_CALL(*database, exec(StartsWith("insert"))).WillByDefault(Return(nullptr));

  EXPECT_CALL(*database, exec(StartsWith("update"), testing::_))
      .Times(Exactly(1));

  EXPECT_CALL(*database, exec(StartsWith("insert"), testing::_))
      .Times(Exactly(1));

  const auto instances =
      lcloud::create_db_instances<test_result_reader, test_row_t>("test",
                                                                  database);
  instances->add("test");
}
#pragma endregion

#pragma region Test: UpdateExistingInstance
TEST_F(DatabaseTestFixture, UpdateExistingInstance) {
  test_result_t result{
      test_row_t{{"count", "1"}},
  };

  ON_CALL(*database, exec(StartsWith("update")))
      .WillByDefault(Return(std::make_shared<test_result_reader>(result)));

  ON_CALL(*database, exec(StartsWith("insert"))).WillByDefault(Return(nullptr));

  EXPECT_CALL(*database, exec(StartsWith("update"), testing::_))
      .WillOnce(Return(std::make_shared<test_result_reader>(result)));

  EXPECT_CALL(*database, exec(StartsWith("insert"), testing::_))
      .Times(Exactly(0));

  const auto instances =
      lcloud::create_db_instances<test_result_reader, test_row_t>("test",
                                                                  database);
  instances->add("test");
}
#pragma endregion
