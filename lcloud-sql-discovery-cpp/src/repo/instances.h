// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

//
// Created by bekamk on 03.06.2025.
//

#ifndef INSTANCES_H
#define INSTANCES_H
#include <functional>
#include <list>
#include <memory>
#include <string>

#include "database.h"


namespace lcloud {
    template<class ResultReaderT, typename RowT>
    class database;

    template<class ResultReaderT, typename RowT = typename ResultReaderT::row_t>
    class instances {
    public:
        instances(std::string service) {
            service_ = std::move(service);
        }

        virtual ~instances() = default;

        virtual void initialize() = 0;

        virtual std::list<std::string> get_all() = 0;

        virtual void add(std::string instance) = 0;

    protected:
        std::string service_;
    };

    template<class ResultReaderT, typename RowT = typename ResultReaderT::row_t>
    class db_instances : public instances<ResultReaderT> {
    public:
        db_instances(const std::string &service,
                     std::shared_ptr<database<ResultReaderT, RowT> > database)
            : instances<ResultReaderT>(service), database_(std::move(database)) {
        }

        virtual std::string init_query_string() = 0;

        virtual std::string add_query_string() = 0;

        void initialize() noexcept(false) override {
            database_->exec(init_query_string());
        }

        std::list<std::string> get_all() override {
            const auto rowset =
                    database_->exec(add_query_string());

            if (rowset == nullptr) {
                return {};
            }

            auto results = std::list<std::string>();

            for (const auto &row: *rowset) {
                results.emplace_back(database_->read_string(row, "name"));
            }

            return results;
        }

        void add(std::string instance) override {
        }

    protected:
        const std::shared_ptr<database<ResultReaderT, RowT> > database_;
    };


    template<class ResultReaderT, typename RowT = typename ResultReaderT::row_t>
    extern std::function<std::shared_ptr<instances<ResultReaderT, RowT> >(std::string service)> create_instances;

    template<class ResultReaderT, typename RowT = typename ResultReaderT::row_t>
    extern std::function<std::shared_ptr<db_instances<ResultReaderT, RowT> >(
        std::string service, std::shared_ptr<database<ResultReaderT, RowT> > database)> create_db_instances;

    std::shared_ptr<instances<ResultReader<pqxx::result> > > create_postgres_instances(const std::string &service);
}


#endif //INSTANCES_H
