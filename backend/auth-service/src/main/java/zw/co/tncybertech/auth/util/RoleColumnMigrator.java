package zw.co.tncybertech.auth.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RoleColumnMigrator implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(RoleColumnMigrator.class);
  private final JdbcTemplate jdbc;

  public RoleColumnMigrator(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      String schema = jdbc.queryForObject("select database()", String.class);
      if (schema == null || schema.isBlank()) {
        return;
      }

      List<Map<String, Object>> rows = jdbc.queryForList(
          "select data_type, character_maximum_length " +
              "from information_schema.columns " +
              "where table_schema = ? and table_name = 'users' and column_name = 'role'",
          schema
      );
      if (rows.isEmpty()) {
        return;
      }

      String dataType = String.valueOf(rows.get(0).get("data_type"));
      Object lenValue = rows.get(0).get("character_maximum_length");
      long maxLen = lenValue == null ? -1 : ((Number) lenValue).longValue();

      if ("enum".equalsIgnoreCase(dataType) || (maxLen > 0 && maxLen < 40)) {
        log.warn("Migrating users.role column from {} (len={}) to VARCHAR(40).", dataType, maxLen);
        jdbc.execute("alter table users modify column role varchar(40) not null");
      }
    } catch (Exception e) {
      log.warn("Role column migration skipped: {}", e.getMessage());
    }
  }
}
