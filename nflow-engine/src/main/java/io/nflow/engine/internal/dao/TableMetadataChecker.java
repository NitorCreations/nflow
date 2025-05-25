package io.nflow.engine.internal.dao;

import static java.lang.String.format;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.model.ModelObject;

@Named
public class TableMetadataChecker {
  private final JdbcTemplate jdbc;

  @Inject
  public TableMetadataChecker(@NFlow JdbcTemplate jdbcTemplate) {
    this.jdbc = jdbcTemplate;
  }

  public void ensureCopyingPossible(String sourceTable, String destinationTable) {
    Map<String, ColumnMetadata> sourceMetadataMap = getMetadata(sourceTable);
    Map<String, ColumnMetadata> destMetadataMap = getMetadata(destinationTable);
    if (destMetadataMap.size() < sourceMetadataMap.size()) {
      throw new IllegalArgumentException(
          format("Source table %s has more columns than destination table %s", sourceTable, destinationTable));
    }
    Set<String> sourceKeySet = sourceMetadataMap.keySet();
    Set<String> destKeySet = destMetadataMap.keySet();
    if (!destKeySet.containsAll(sourceKeySet)) {
      Set<String> missingColumns = new LinkedHashSet<>(sourceKeySet);
      missingColumns.removeAll(destKeySet);
      throw new IllegalArgumentException(format("Destination table %s is missing columns %s that are present in source table %s",
          destinationTable, missingColumns, sourceTable));
    }
    for (Entry<String, ColumnMetadata> entry : sourceMetadataMap.entrySet()) {
      ColumnMetadata sourceMetadata = entry.getValue();
      ColumnMetadata destMetadata = destMetadataMap.get(entry.getKey());
      if (!sourceMetadata.typeName.equals(destMetadata.typeName)) {
        throw new IllegalArgumentException(format(
            "Source column %s.%s has type %s and destination column %s.%s has mismatching type %s", sourceTable,
            sourceMetadata.columnName, sourceMetadata.typeName, destinationTable, destMetadata.columnName,
            destMetadata.typeName));
      }
      if (sourceMetadata.size > destMetadata.size) {
        throw new IllegalArgumentException(format("Source column %s.%s has size %s and destination column %s.%s smaller size %s",
            sourceTable, sourceMetadata.columnName, sourceMetadata.size, destinationTable, destMetadata.columnName,
            destMetadata.size));
      }
    }
  }

  private Map<String, ColumnMetadata> getMetadata(String tableName) {
    return jdbc.query("select * from " + tableName + " where 1 = 0", new MetadataExtractor());
  }

  static class MetadataExtractor implements ResultSetExtractor<Map<String, ColumnMetadata>> {
    private final Map<String, String> typeAliases = Map.of("serial", "int4", "bigserial", "int8");

    @Override
    public Map<String, ColumnMetadata> extractData(ResultSet rs) throws SQLException {
      ResultSetMetaData metadata = rs.getMetaData();
      Map<String, ColumnMetadata> metadataMap = new LinkedHashMap<>(metadata.getColumnCount() * 2);
      for (int col = 1; col <= metadata.getColumnCount(); col++) {
        String columnName = metadata.getColumnName(col);
        String typeName = metadata.getColumnTypeName(col);
        int size = metadata.getColumnDisplaySize(col);
        metadataMap.put(columnName, new ColumnMetadata(columnName, resolveTypeAlias(typeName), size));
      }
      return metadataMap;
    }

    private String resolveTypeAlias(String type) {
      return typeAliases.getOrDefault(type, type);
    }

  }

  private static class ColumnMetadata extends ModelObject {
    public final String columnName;
    public final String typeName;
    public final int size;

    public ColumnMetadata(String columnName, String typeName, int size) {
      this.columnName = columnName;
      this.typeName = typeName;
      this.size = size;
    }
  }

}
