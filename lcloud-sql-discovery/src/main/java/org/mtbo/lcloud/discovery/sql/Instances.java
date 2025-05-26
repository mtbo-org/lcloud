/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.sql;

import java.time.Instant;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity, holding discovery instance lock
 *
 * @param id primaru key
 * @param service service name
 * @param name instance name
 * @param last last ping time
 */
@Table
public record Instances(String id, String service, String name, Instant last) {}
