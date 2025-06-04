// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

//
// Created by bekamk on 03.06.2025.
//

#ifndef DATABASE_H
#define DATABASE_H
#include <functional>
#include <memory>
#include <pqxx/result>

namespace pqxx {
    class row;
    class connection;
    class result;
}


namespace lcloud {
    class Result {
    };

    template<typename ResultT, typename RowT = typename ResultT::reference>
    class ResultReader {
    public:
        explicit ResultReader(const ResultT &result) : result(result) {
        }

        typedef ResultT result_t;
        typedef RowT row_t;

        const ResultT &result;

        [[nodiscard]] typename ResultT::const_iterator cbegin() const {
            return result.cbegin();
        }

        [[nodiscard]] typename ResultT::const_iterator cend() const {
            return result.cend();
        }

        [[nodiscard]] typename ResultT::const_iterator begin() {
            return result.cbegin();
        }

        [[nodiscard]] typename ResultT::const_iterator end() {
            return result.cend();
        }
    };


    template<class ResultReaderT,
        typename RowT = typename ResultReaderT::row_t>
    class database {
    public:
        database() noexcept(false) = default;

        virtual ~database() = default;

        virtual std::shared_ptr<ResultReaderT> exec(const std::string &query_string) noexcept(false) = 0;

        virtual std::string quote(const std::string &string) = 0;

        virtual std::string read_string(const RowT &row, const char *str) = 0;
    };

    template<class ResultReaderT,
        typename RowT = typename ResultReaderT::row_t>
    extern std::function<std::shared_ptr<database<ResultReaderT, RowT> >()> create_database;

    std::shared_ptr<database<ResultReader<pqxx::result> > > create_postgres_database(
        const std::string &connection_string);
}


#endif //DATABASE_H
