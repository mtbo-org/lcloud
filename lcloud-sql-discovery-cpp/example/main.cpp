// Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
// Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.


//export PATH=/c/Program Files/Microsoft Visual Studio/2022/Community/VC/Tools/MSVC/14.39.33519/bin/Hostx64/x64:$PATH

#include <iostream>
#include <discovery.h>

#include "repo/instances.h"

void DI() {
    lcloud::create_database = [] {
        return lcloud::create_postgres_database("postgresql://user:user@localhost:5432/demo");
    };

    lcloud::create_instances = [](const std::string &service) {
        return lcloud::create_db_instances(service);
    };
}

int main() {
    DI();

    const lcloud::discovery discovery{
        "lcloud", "aaa"
    };

    discovery.initialize();

    std::cout << "DB initialized\n";

    return 0;
}
