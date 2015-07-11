package com.nitorcreations.nflow.engine.internal.dao;

import com.nitorcreations.nflow.engine.internal.config.NFlow;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.inject.Inject;
import javax.inject.Named;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.lang.String.format;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

@Named
public class TableMetadataChecker {
  private JdbcTemplate jdbc;

  public void ensureCopyingPossible(String sourceTable, String destinationTable) {
    Map<String, ColumnMetadata> sourceMetadataMap = getMetadata(sourceTable);
    Map<String, ColumnMetadata> destMetadataMap = getMetadata(destinationTable);
    if(destMetadataMap.size() < sourceMetadataMap.size()) {
      throw new IllegalArgumentException(format("Source table %s has more columns than destination table %s",
              sourceTable, destinationTable));
    }
    if(! destMetadataMap.keySet().containsAll(sourceMetadataMap.keySet())) {
      Set<String> missingColumns = new LinkedHashSet<>(destMetadataMap.keySet());
      missingColumns.removeAll(sourceMetadataMap.keySet());
      throw new IllegalArgumentException(format("Destination table %s is missing columns %s that are present in source table %s",
              destinationTable, missingColumns, sourceTable));
    }
    for(Entry<String, ColumnMetadata> entry: sourceMetadataMap.entrySet()) {
      ColumnMetadata sourceMetadata = entry.getValue();
      ColumnMetadata destMetadata = destMetadataMap.get(entry.getKey());
      if(!sourceMetadata.typeName.equals(destMetadata.typeName)) {
        throw new IllegalArgumentException(format("Source column %s.%s has type %s and destination column %s.%s has mismatching type %s",
                sourceTable, sourceMetadata.columnName, sourceMetadata.typeName,
                destinationTable, destMetadata.columnName, destMetadata.typeName));
      }
      if(sourceMetadata.size > destMetadata.size) {
        throw new IllegalArgumentException(format("Source column %s.%s has size %s and destination column %s.%s smaller size %s",
                sourceTable, sourceMetadata.columnName, sourceMetadata.size,
                destinationTable, destMetadata.columnName, destMetadata.size));
      }
    }
  }

  private Map<String, ColumnMetadata> getMetadata(String tableName) {
    return jdbc.query("select * from " + tableName + " where 1 = 0", new MetadataExtractor());
  }

  private static class MetadataExtractor implements ResultSetExtractor<Map<String, ColumnMetadata>> {

    @Override
    public Map<String, ColumnMetadata> extractData(ResultSet rs) throws SQLException, DataAccessException {
      ResultSetMetaData metadata = rs.getMetaData();
      Map<String, ColumnMetadata> metadataMap = new LinkedHashMap<>();
      for(int col = 1; col <= metadata.getColumnCount(); col ++) {
        String columnName = metadata.getColumnName(col);
        String typeName = metadata.getColumnTypeName(col);
        int size = metadata.getColumnDisplaySize(col);
        metadataMap.put(columnName, new ColumnMetadata(columnName, typeName, size));
      }
      return metadataMap;
    }
  }

  private static class ColumnMetadata {
    public final String columnName;
    public final String typeName;
    public final int size;

    public ColumnMetadata(String columnName, String typeName, int size) {
      this.columnName = columnName;
      this.typeName = typeName;
      this.size = size;
    }

    @Override
    public String toString() {
      return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
    }
  }

  @Inject
  public void setJdbcTemplate(@NFlow JdbcTemplate jdbcTemplate) {
    this.jdbc = jdbcTemplate;
  }

}
