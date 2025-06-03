// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

//
// Created by bekamk on 03.06.2025.
//

#ifndef SQL_DISCOVERY_H
#define SQL_DISCOVERY_H
#include <string>
#include <utility>

#include "repo/instances.h"

namespace lcloud {
    class discovery {
    public:
        discovery(std::string service_name, std::string instance_name);

        void initialize() const;

        void lookup() const;

        void ping() const;

    private:
        const std::string service_name;
        const std::string instance_name;
        std::shared_ptr<instances> instances_;
    };
} // lcloud

#endif //SQL_DISCOVERY_H
