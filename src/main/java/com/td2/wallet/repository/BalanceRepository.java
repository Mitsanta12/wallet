package com.td2.wallet.repository;

import com.td2.wallet.model.Balance;
import com.td2.wallet.model.BalanceHistory;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Repository
public class BalanceRepository {
    private final AccountCrudOperation accountCrudOperation;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Balance saveBalanceEntry(Balance toSave) {
        String insertBalanceQuery = "INSERT INTO balance (account_id, balance_value, balance_date) VALUES ( ?, ?, ?) ON CONFLICT (account_id) DO UPDATE SET account_id = excluded.account_id, balance_value = excluded.balance_value, balance_date = excluded.balance_date";
        int rowsAffected = jdbcTemplate.update(insertBalanceQuery,
                toSave.getId(),
                toSave.getBalance_value(),
                toSave.getBalance_date()
        );

        if (rowsAffected > 0) {
            return toSave;
        } else {
            return null;
        }
    }
    public BigDecimal getBalanceByDateTime(String id, LocalDateTime dateTime) {
        String selectBalanceQuery = "SELECT balance_value FROM balance WHERE id = ? AND balance_date <= ? ORDER BY balance_date DESC LIMIT 1";
        BigDecimal balanceValue = jdbcTemplate.queryForObject(
                selectBalanceQuery,
                BigDecimal.class,
                id,
                Timestamp.valueOf(dateTime)
        );
        return (balanceValue != null) ? balanceValue : BigDecimal.ZERO;
    }
    public List<BalanceHistory> findByAccountIdAndTimestampBetween(String accountId, LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT * FROM balance_history " +
                "WHERE account_id = ? AND history_date BETWEEN ? AND ?";

        return jdbcTemplate.query(sql, new Object[]{accountId, start, end}, (resultSet, rowNum) -> {
            BalanceHistory entry = new BalanceHistory();
            entry.setId(resultSet.getString("id"));
            // Assuming "account_id" and "balance_id" are UUIDs
            entry.setAccountId(resultSet.getString("account_id"));
            entry.setBalanceId(resultSet.getString("balance_id"));
            entry.setHistoryDate(resultSet.getTimestamp("history_date").toLocalDateTime());
            // Set other fields accordingly
            return entry;
        });
    }
    private Balance findBalanceById(Long balanceId) {
        String query = "SELECT * FROM balance WHERE id = ?";

        try {
            return jdbcTemplate.queryForObject(query, new Object[]{balanceId}, (resultSet, rowNum) -> {
                Balance balance = new Balance();
                balance.setId(resultSet.getString("id"));
                balance.setBalance_value(resultSet.getDouble("balance_value"));
                balance.setBalance_date(LocalDate.from(resultSet.getTimestamp("balance_date").toLocalDateTime()));
                // Set other fields accordingly
                return balance;
            });
        } catch (EmptyResultDataAccessException e) {
            // Handle the case where no result is found
            return null;
        } catch (DataAccessException e) {
            // Handle other exceptions
            throw new RuntimeException("Error fetching Balance by ID", e);
        }
    }

}

