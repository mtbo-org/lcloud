// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

#include "discovery.h"

#include <bits/ostream.tcc>
#include <iostream>
#include <rx.hpp>
#include <utility>

#include "discovery.h"
#include "repo/instances.h"

namespace Rx {
using namespace rxcpp;
using namespace rxcpp::sources;
using namespace rxcpp::operators;
using namespace rxcpp::util;
}  // namespace Rx

using namespace Rx;

namespace lcloud {
class sql_discovery : public discovery {
 public:
  typedef result_reader<pqxx::result> reader_t;
  typedef std::shared_ptr<instances> instances_ptr;

 private:
  instances_ptr instances_;

 public:
  sql_discovery(const std::string& service_name,
                const std::string& instance_name,
                const std::chrono::milliseconds& interval =
                    std::chrono::milliseconds(1000))
      : discovery(service_name, instance_name, interval) {
    instances_ = create_instances<reader_t>(service_name);
  }

  void initialize() const override { instances_->initialize(); }

  [[nodiscard]] observable<lookup_t> lookup() const override {
    std::cout << "Lookup on: " << service_name << ":" << instance_name
              << std::endl;

    return observable<>::create<lookup_t>([&](const subscriber<lookup_t>& s) {
             auto all = instances_->get_all(interval);
             s.on_next(all);
             s.on_completed();
           }) |
           on_error_resume_next([this](std::exception_ptr ep) {
             printf("Lookup resuming after: %s\n", what(std::move(ep)).c_str());
             return observable<>::empty<lookup_t>();
           }) |
           delay(interval, observe_on_new_thread()) | repeat() | publish() |
           ref_count() |
           finally([] { std::cout << "Lookup unsubscribed" << std::endl; });
  }

  [[nodiscard]] observable<bool> ping() const override {
    std::cout << "Ping to: " << service_name << ":" << instance_name
              << std::endl;

    return observable<>::create<bool>([&](const subscriber<bool>& s) {
             instances_->add(instance_name);
             s.on_completed();
           }) |
           on_error_resume_next([this](std::exception_ptr ep) {
             printf("Ping resuming after: %s\n", what(std::move(ep)).c_str());
             return observable<>::empty<bool>();
           }) |
           delay(interval / 2, observe_on_new_thread()) | repeat() | publish() |
           ref_count() | finally([] { std::cout << "Ping unsubscribed\n"; });
  }
};

std::shared_ptr<discovery> create_postgres_discovery(
    const std::string& service_name, const std::string& instance_name,
    const std::chrono::milliseconds& interval) {
  return std::make_shared<sql_discovery>(service_name, instance_name, interval);
}
}  // namespace lcloud
