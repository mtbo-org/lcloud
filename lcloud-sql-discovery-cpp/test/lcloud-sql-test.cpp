// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

#include <algorithm>
#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "rxcpp/rx-test.hpp"
#include "catch2/catch_all.hpp"

#include "../src/repo/database.h"
#include "../src/repo/instances.h"

using testing::NiceMock;
using testing::Return;

typedef std::map<std::string, std::string> test_row_t;
typedef std::list<test_row_t> test_result_t;
typedef lcloud::ResultReader<test_result_t> test_result_reader_r;
typedef lcloud::database<test_result_reader_r, test_row_t> database_t;

typedef std::shared_ptr<database_t> database_ptr;;

class MockDatabase : public database_t {
public:
    MOCK_METHOD(std::shared_ptr<test_result_reader_r>, exec, (const std::string &query_string), (override));
    MOCK_METHOD(std::string, quote, (const std::string &string), (override));

    std::string read_string(const test_row_t &row, const char *str) override {
        return row.at(str);
    }
};

class MockInstances : public lcloud::db_instances<test_result_reader_r, test_row_t> {
public:
    MockInstances(const std::string &service,
                  const database_ptr &database)
        : db_instances(service, database) {
    }

    std::string init_query_string() override { return ""; }

    std::string add_query_string() override { return ""; };
};


using ::testing::_;
using ::testing::Not;
using ::testing::IsEmpty;
using ::testing::Exactly;

template<>
std::function<database_ptr()> lcloud::create_database<
    test_result_reader_r, test_row_t>;


template<>
std::function<std::shared_ptr<lcloud::db_instances<test_result_reader_r, test_row_t> >(
    std::string service,
    database_ptr database)> lcloud::create_db_instances<
    test_result_reader_r, test_row_t>;


struct DatabaseTest : testing::Test {
    DatabaseTest() {
        database = std::make_shared<NiceMock<MockDatabase> >();

        lcloud::create_database<test_result_reader_r, test_row_t> = [this] {
            return database;
        };

        lcloud::create_db_instances<test_result_reader_r, test_row_t> = [this](const std::string &service,
                                                                               database_ptr database) {
            return std::make_shared<MockInstances>(service, database);
        };
    }


    std::shared_ptr<NiceMock<MockDatabase> > database;
};

TEST_F(DatabaseTest, Init) {
    // ON_CALL(*database, exec(_)).WillByDefault(
    //     Return());

    EXPECT_CALL(*database, exec(_)).Times(Exactly(1));

    const auto instances = lcloud::create_db_instances<test_result_reader_r, test_row_t>("test", database);

    instances->initialize();
}

TEST_F(DatabaseTest, GetAll) {
    // lcloud::create_database = [this] {
    //     return lcloud::create_postgres_database("postgresql://user:user@localhost:5432/demo");
    // };

    test_result_t result{
        test_row_t{{"name", "a"}},
        test_row_t{{"name", "b"}},
        test_row_t{{"name", "c"}},
    };

    ON_CALL(*database, exec(_))
            .
            WillByDefault(
                Return(std::make_shared<lcloud::ResultReader<test_result_t> >(result)));
    //
    // EXPECT_CALL(*database, exec(_)).Times(Exactly(1));

    const auto instances = lcloud::create_db_instances<test_result_reader_r, test_row_t>("test", database);
    const auto all = instances->get_all();


    EXPECT_EQ(all, std::list<std::string>({"a", "b", "c"}));
}
