// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

//
// Created by bekamk on 03.06.2025.
//

#include "instances.h"

#include <utility>
#include <pqxx/result>
#include <pqxx/transaction>

lcloud::instances::instances(std::string service) : service(std::move(service)) {
}


class db_instances final : public lcloud::instances {
public:
    explicit db_instances(const std::string &service, std::shared_ptr<lcloud::database> database): instances(service) {
        database_ = std::move(database);
    }

    void initialize() override {
        database_->exec(R"(
create table if not exists instances
(
    id      uuid not null primary key,
    service varchar(255),
    name    varchar(255),
    last    timestamp
        with time zone default now(),
    constraint instances_uniq
    unique(name, service)
);

create index if not exists last_index
    on instances(last);
)");
    }

    std::list<std::string> get_all() override {
        std::list<std::string> result;

        pqxx::work *tx{};

        auto rowset = database_->exec("select name from instances where service=" + database_->quote(service));
        if (rowset == nullptr) {
            return {};
        }

        auto results = std::list<std::string>();

        for (auto row: *rowset) {
            results.emplace_back(row["name"].c_str());
        }

        return result;
    }

    void add(std::string instance) override {
    }
};

std::function<std::shared_ptr<lcloud::instances>(std::string service)> lcloud::create_instances;

std::shared_ptr<lcloud::instances> lcloud::create_db_instances(const std::string &service) {
    return std::make_shared<db_instances>(db_instances(service, create_database()));
}
