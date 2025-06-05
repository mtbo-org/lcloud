// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

//
// Created by bekamk on 03.06.2025.
//

#ifndef DISCOVERY_H_
#define DISCOVERY_H_
#include <memory>
#include <rx-observable.hpp>
#include <string>

namespace lcloud {
class discovery {
 public:
  typedef std::list<std::string> lookup_t;

  discovery(
      std::string service_name, std::string instance_name,
      std::chrono::milliseconds interval = std::chrono::milliseconds(1000))
      : service_name(std::move(service_name)),
        instance_name(std::move(instance_name)),
        interval(std::move(interval)) {}

  virtual ~discovery() = default;

  virtual void initialize() const noexcept(false) = 0;

  [[nodiscard]] virtual rxcpp::observable<lookup_t> lookup() const = 0;

  [[nodiscard]] virtual rxcpp::observable<bool> ping() const = 0;

 protected:
  const std::string service_name;
  const std::string instance_name;
  const std::chrono::milliseconds interval;
};

std::shared_ptr<discovery> create_postgres_discovery(
    const std::string& service_name, const std::string& instance_name,
    const std::chrono::milliseconds& interval =
        std::chrono::milliseconds(1000));
}  // namespace lcloud

#endif  // DISCOVERY_H_
