// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

//
// Created by bekamk on 03.06.2025.
//

#include "instances.h"

#include <pqxx/result>
#include <pqxx/transaction>

class postgres_instances final : public lcloud::db_instances<lcloud::ResultReader<pqxx::result> > {
public:
    postgres_instances(const std::string &service,
                       const std::shared_ptr<lcloud::database<lcloud::ResultReader<pqxx::result> > > &database)
        : db_instances(service, database) {
    }

    std::string init_query_string() override {
        return R"(
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
)";
    }

    std::string add_query_string() override {
        return "select name from instances where service=" + database_->quote(service_);
    }
};

template<>
std::function<std::shared_ptr<lcloud::instances<lcloud::ResultReader<pqxx::result> > >(std::string service)>
lcloud::create_instances<lcloud::ResultReader<pqxx::result> >;

std::shared_ptr<lcloud::instances<lcloud::ResultReader<pqxx::result> > > create_postgres_instances(
    const std::string &service) {
    return std::make_shared<
        postgres_instances>(service, lcloud::create_database<lcloud::ResultReader<pqxx::result> >());
}
