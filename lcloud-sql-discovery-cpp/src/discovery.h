// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

//
// Created by bekamk on 03.06.2025.
//

#ifndef SQL_DISCOVERY_H
#define SQL_DISCOVERY_H
#include <memory>
#include <rx-observable.hpp>
#include <string>

namespace lcloud {
    class discovery {
    public:
        discovery(std::string service_name, std::string instance_name) : service_name(
                                                                             std::move(service_name)),
                                                                         instance_name(std::move(instance_name)) {
        }

        virtual ~discovery() = default;;

        virtual void initialize() const noexcept(false) = 0;

        virtual void lookup() const = 0;

        [[nodiscard]] virtual rxcpp::composite_subscription ping() const = 0;

    protected:
        const std::string service_name;
        const std::string instance_name;
    };
} // lcloud

#endif //SQL_DISCOVERY_H
