// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

#include "discovery.h"

#include <iostream>
#include <bits/ostream.tcc>
#include <rx.hpp>

#include "repo/instances.h"

namespace lcloud {
    discovery::discovery(std::string service_name,
                         std::string instance_name): service_name(
                                                         std::move(service_name)),
                                                     instance_name(std::move(instance_name)) {
        instances_ = create_instances(service_name);
    }

    void discovery::initialize() const {
        instances_->initialize();
    }

    void discovery::lookup() const {
        std::cout << "Lookup on: " << service_name << std::endl;

        auto values = rxcpp::observable<>::range(1); // infinite (until overflow) stream of integers

        auto s1 = values.
                map([](int prime) { return std::make_tuple("1:", prime); });

        auto s2 = values.
                map([](int prime) { return std::make_tuple("2:", prime); });

        s1.
                merge(s2).
                take(6).
                as_blocking().
                subscribe(rxcpp::util::apply_to(
                    [](const char *s, int p) {
                        printf("%s %d\n", s, p);
                    }));
    }

    void discovery::ping() const {
        std::cout << "Ping to: " << service_name << ":" << instance_name << std::endl;
    }
} // lcloud
