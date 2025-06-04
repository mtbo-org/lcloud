// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

#include "discovery.h"

#include <iostream>
#include <bits/ostream.tcc>
#include <rx.hpp>
#include <utility>
#include "repo/instances.h"

namespace Rx {
    using namespace rxcpp;
    using namespace rxcpp::sources;
    using namespace rxcpp::operators;
    using namespace rxcpp::util;
}

using namespace Rx;


namespace lcloud {
    class sql_discovery : public discovery {
    public:
        std::function<std::shared_ptr<instances<ResultReader<pqxx::result> > >(std::string service)>::result_type
        instances_;

        sql_discovery(const std::string &service_name,
                      const std::string &instance_name) : discovery(service_name, instance_name) {
            instances_ = create_instances<ResultReader<pqxx::result> >(service_name);
        }

        void initialize() const override {
            instances_->initialize();
        }

        void lookup() const override {
        }

        composite_subscription ping() const override {
            std::cout << "Ping to: " << service_name << ":" << instance_name << std::endl;

            const auto published_observable = observable<>::interval(std::chrono::milliseconds(1000),
                                                                     observe_on_new_thread())
                                              | flat_map(
                                                  [this](int v) {
                                                      return
                                                              observable<>::create<std::nullptr_t>(
                                                                  [&](const subscriber<std::nullptr_t> &s) {
                                                                      std::cout << "ADD" << std::endl;
                                                                      instances_->add(instance_name);
                                                                      s.on_completed();
                                                                  });
                                                  })
                                              | on_error_resume_next([](std::exception_ptr ep) {
                                                  printf("Resuming after: %s\n", what(std::move(ep)).c_str());
                                                  return observable<>::just(nullptr);
                                              })
                                              | publish()
                                              | ref_count();


            const auto subscription = published_observable.
                    finally([] {
                        std::cout << "unsubscribed" << std::endl << std::endl;
                    }).
                    subscribe(
                        [](std::nullptr_t v) { printf("OnNext: \n"); },
                        [](std::exception_ptr ep) {
                            try { std::rethrow_exception(std::move(ep)); } catch (const std::exception &ex) {
                                printf("OnError: %s\n", ex.what());
                            }
                        },
                        [] { printf("OnCompleted\n"); }
                    );

            return subscription;
        }
    };
} // lcloud
