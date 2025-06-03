// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

//
// Created by bekamk on 03.06.2025.
//

#include "database.h"

#include <iostream>
#include <pqxx/pqxx>

class postgres_database : public lcloud::database {
public:
    explicit postgres_database(const std::string &connection_string) {
        connection_ = std::make_shared<pqxx::connection>(connection_string);
    }

    std::shared_ptr<pqxx::result> exec(const std::string &query_string) override {
        pqxx::work tx{*connection_};

        try {
            auto r = std::make_shared<pqxx::result>(tx.exec(query_string));
            tx.commit();
            return r;
        } catch (pqxx::sql_error const &e) {
            std::cerr << "SQL error: " << e.what() << std::endl;
            std::cerr << "Query was: " << e.query() << std::endl;
            return nullptr;
        }
        catch (std::exception const &e) {
            std::cerr << "Error: " << e.what() << std::endl;
            return nullptr;
        }
    }

    std::string quote(const std::string &string) override {
        return connection_->quote(string);
    }

private:
    std::shared_ptr<pqxx::connection> connection_;
};


std::function<std::shared_ptr<lcloud::database>()> lcloud::create_database;

std::shared_ptr<lcloud::database> lcloud::create_postgres_database(const std::string &connection_string) {
    return std::make_shared<postgres_database>(connection_string);
}
