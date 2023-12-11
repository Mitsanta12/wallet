
package com.td2.wallet.repository;

import com.td2.wallet.model.*;
import com.td2.wallet.repository.interfacegenerique.CrudOperations;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.management.Query;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Repository
@AllArgsConstructor
public class TransactionCrudOperations implements CrudOperations<Transaction> {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private AccountCrudOperation accountCrudOperation;
    private TransactionCrudOperations transactionCrudOperations;
    @Override
    public List<Transaction> findAll() {
        List<Transaction> transaction = new ArrayList<>();
        String query = "SELECT * FROM transaction";
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                String id = resultSet.getString("id");
                BigDecimal amount = resultSet.getBigDecimal("amount");
                LocalDate transactionDate = resultSet.getDate("transaction_date").toLocalDate();
                Transaction.Label label = Transaction.Label.valueOf(resultSet.getString("label"));
                Transaction.Type transactionType = Transaction.Type.valueOf(resultSet.getString("type"));
                transaction.add(new Transaction(id,label,transactionType,amount,transactionDate));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transaction;
    }

    @Override
    public List<Transaction> saveAll(List<Transaction> toSave) {
        String query = "INSERT INTO transaction(id, account_id, label, type, amount, transaction_date ) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET account_id = excluded.account_id ,label = excluded.label,type = excluded.type,  amount = excluded.amount, transaction_date = excluded.transaction_date";
        jdbcTemplate.batchUpdate(query, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                Transaction transaction = toSave.get(i);
                preparedStatement.setString(1, transaction.getId());
                preparedStatement.setString(3, String.valueOf(transaction.getLabel()));
                preparedStatement.setString(4, String.valueOf(transaction.getTransactionType()));
                preparedStatement.setBigDecimal(6, transaction.getAmount());
                preparedStatement.setDate(7, java.sql.Date.valueOf(transaction.getTransactionDate()));
            }
            @Override
            public int getBatchSize() {
                return toSave.size();
            }
        });

        return toSave;
    }
    @Override
    public Transaction save(Transaction toSave) {
        String query = "INSERT INTO transaction(id, label, transaction_type, amount, transaction_date) " +
                "VALUES (?, CAST(? AS label), CAST(? AS transaction_type), ?, ?) " +
                "ON CONFLICT (id) DO UPDATE " +
                "SET label = excluded.label, " +
                "    transaction_type = excluded.transaction_type, " +
                "    amount = excluded.amount, " +
                "    transaction_date = excluded.transaction_date";

        int rowsAffected = jdbcTemplate.update(query,
                toSave.getId(),
                (toSave.getLabel() != null) ? toSave.getLabel().name() : null,
                toSave.getTransactionType().name(),
                toSave.getAmount(),
                toSave.getTransactionDate()
        );

        if (rowsAffected > 0) {
            return toSave;
        } else {
            return null;
        }
    }
    @Transactional
    public void transferMoney(String debitAccountId, String creditAccountId, BigDecimal amount) {
        // Check if both accounts exist
        if (!accountExists(debitAccountId) || !accountExists(creditAccountId)) {
            throw new RuntimeException("Invalid account IDs");
        }

        // Check if the debit and credit accounts are the same
        if (debitAccountId.equals(creditAccountId)) {
            throw new RuntimeException("Cannot transfer money to the same account");
        }

        // Check if the debit account has sufficient balance for the transfer
        if (getAccountBalance(debitAccountId).compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance for the transfer");
        }

        // Perform the transfer using JDBC Template
        jdbcTemplate.update("UPDATE accounts SET balance = balance - ? WHERE id = ?", amount, debitAccountId);
        jdbcTemplate.update("UPDATE accounts SET balance = balance + ? WHERE id = ?", amount, creditAccountId);

        // Record transfer history
        Transaction debitTransaction = new Transaction();
        debitTransaction.setTransactionType(Transaction.Type.debit);
        debitTransaction.setAmount(amount);

        Transaction creditTransaction = new Transaction();
        creditTransaction.setTransactionType(Transaction.Type.credit);
        creditTransaction.setAmount(amount);

        Transaction debitTransactionId = transactionCrudOperations.save(debitTransaction);
        Transaction creditTransactionId = transactionCrudOperations.save(creditTransaction);

        jdbcTemplate.update("INSERT INTO transfer_history (debit_transaction_id, credit_transaction_id) VALUES (?, ?)",
                debitTransactionId, creditTransactionId);
    }

    private boolean accountExists(String accountId) {
        String query = "SELECT COUNT(*) FROM accounts WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, accountId);
        return count != null && count > 0;
    }

    private BigDecimal getAccountBalance(String accountId) {
        String query = "SELECT balance FROM accounts WHERE id = ?";
        return jdbcTemplate.queryForObject(query, BigDecimal.class, accountId);
    }
    public void saveTransferHistory(TransferHistory transferHistory) {
        String insertQuery = "INSERT INTO transfer_history (transaction_type, transfer_date) VALUES (?, ?, ?)";
        jdbcTemplate.update(insertQuery,
                transferHistory.getTransactionType(),
                transferHistory.getTransferDate());
    }

    public List<TransferHistory> findByTransferDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        String queryString = "SELECT th FROM Transfer_history th WHERE th.transfer_date BETWEEN :startDate AND :endDate";
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, String.valueOf(startDate));
            statement.setString(2, String.valueOf( endDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String id = resultSet.getString("id");
                    String typeTransaction = resultSet.getString("transaction_type");
                    LocalDateTime date = resultSet.getDate("transfer_date").toLocalDate().atStartOfDay();
                    Transaction transactionType = findTransactionByType(typeTransaction);

                    // Convert the single transaction to a list
                    List<Transaction> transactionsType = new ArrayList<>();
                    if (transactionType != null) {
                        transactionsType.add(transactionType);
                    }

                    return (List<TransferHistory>) new TransferHistory(id, transactionsType, date);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Return null if no account is found with the given ID
    }

    public Transaction findTransactionByType(String transactionType){
        String queryString = "SELECT * FROM Transaction WHERE TYPE = ?";
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(queryString)) {
            statement.setString(1, transactionType);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToTransaction(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    private Transaction mapResultSetToTransaction(ResultSet resultSet) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(resultSet.getString("id"));
        transaction.setTransactionType(Transaction.Type.valueOf((resultSet.getString("transaction_credit_id"))));
        return transaction;
    }
}










