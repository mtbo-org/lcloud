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

namespace pqxx {
    class connection;
    class result;
}

namespace lcloud {
    class database {
    public:
        database() = default;

        virtual ~database() = default;

        virtual std::shared_ptr<pqxx::result> exec(const std::string &query_string) = 0;

        virtual std::string quote(const std::string &string) = 0;
    };

    extern std::function<std::shared_ptr<database>()> create_database;

    std::shared_ptr<database> create_postgres_database(const std::string &connection_string);
}


#endif //DATABASE_H
