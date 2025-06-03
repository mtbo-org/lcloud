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
    class database;

    class instances {
    public:
        explicit instances(std::string service);

        virtual ~instances() = default;

        virtual void initialize() = 0;

        virtual std::list<std::string> get_all() = 0;

        virtual void add(std::string instance) = 0;

    protected:
        std::string service;
        std::shared_ptr<database> database_;
    };

    extern std::function<std::shared_ptr<instances>(std::string service)> create_instances;

    std::shared_ptr<instances> create_db_instances(const std::string &service);
}


#endif //INSTANCES_H
