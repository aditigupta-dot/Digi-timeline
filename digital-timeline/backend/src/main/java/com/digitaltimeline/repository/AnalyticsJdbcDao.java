package com.digitaltimeline.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

/**
 * Plain JDBC (no JPA): hand-written SQL, PreparedStatement, try-with-resources, batching and a manual transaction.
 * Used where raw SQL is the right tool: aggregate analytics and bulk updates.
 */
@Repository
public class AnalyticsJdbcDao {

    public record HeatCell(LocalDate day, int hour, long count) {}

    private final DataSource dataSource;

    public AnalyticsJdbcDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Activity count per (day, hour) since the given instant. The UI folds this into a weekday x hour heatmap. */
    public List<HeatCell> heatmap(UUID userId, Instant since) throws SQLException {
        String sql = "SELECT CAST(occurred_at AS DATE) AS d, EXTRACT(HOUR FROM occurred_at) AS h, COUNT(*) AS c "
                + "FROM activity WHERE user_id = ? AND occurred_at >= ? GROUP BY CAST(occurred_at AS DATE), "
                + "EXTRACT(HOUR FROM occurred_at) ORDER BY d, h";
        List<HeatCell> cells = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ps.setTimestamp(2, Timestamp.from(since));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cells.add(new HeatCell(rs.getDate("d").toLocalDate(), rs.getInt("h"), rs.getLong("c")));
                }
            }
        }
        return cells;
    }

    public Map<String, Long> countByKind(UUID userId) throws SQLException {
        String sql = "SELECT kind, COUNT(*) AS c FROM activity WHERE user_id = ? GROUP BY kind ORDER BY c DESC";
        Map<String, Long> out = new LinkedHashMap<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString("kind"), rs.getLong("c"));
            }
        }
        return out;
    }

    /** Batch update inside ONE manual transaction: all rows change, or none do. */
    public int batchUpdateImportance(UUID userId, Map<UUID, Double> importanceById) throws SQLException {
        String sql = "UPDATE activity SET importance = ? WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection()) {
            boolean previous = c.getAutoCommit();
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (Map.Entry<UUID, Double> e : importanceById.entrySet()) {
                    ps.setDouble(1, e.getValue());
                    ps.setObject(2, e.getKey());
                    ps.setObject(3, userId);
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                c.commit();
                int total = 0;
                for (int r : results) total += Math.max(r, 0);
                return total;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(previous);
            }
        }
    }
}
