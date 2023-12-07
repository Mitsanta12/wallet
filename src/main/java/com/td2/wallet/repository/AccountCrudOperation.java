package com.td2.wallet.repository;


import com.td2.wallet.model.Account;
import com.td2.wallet.model.Devise;
import com.td2.wallet.repository.interfacegenerique.CrudOperations;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
@AllArgsConstructor
@Repository
public class AccountCrudOperation implements CrudOperations<Account> {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        String query = "SELECT * FROM accounts";
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                String id = resultSet.getString("id");
                String name = resultSet.getString("name");
                String deviseId = resultSet.getString("devise_id");
                Devise devise = findDeviseById(deviseId);
                accounts.add(new Account(id,name,devise));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    @Override
    public List<Account> saveAll(List<Account> toSave) {
        String query = "INSERT INTO accounts (id, name, devise_id) VALUES (?, ?, ?)";

        jdbcTemplate.batchUpdate(query, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                Account account = toSave.get(i);
                preparedStatement.setString(1, account.getId());
                preparedStatement.setString(2, account.getName());
                if (account.getDeviseId().getId() != null) {
                    preparedStatement.setString(3, account.getDeviseId().getId());
                } else {
                    // Handle the case where Devise ID is null (throw an exception or set a default value)
                    // For now, let's assume a default value "UNKNOWN"
                    preparedStatement.setString(3, "UNKNOWN");
                }
            }

            @Override
            public int getBatchSize() {
                return toSave.size();
            }
        });

        return toSave;
    }

    @Override
    public Account save(Account toSave) {
        String query = "INSERT INTO accounts (id, name, devise_id) VALUES (?, ?, ?)";

        int rowsAffected = jdbcTemplate.update(query,
                toSave.getId(),
                toSave.getName(),
                toSave.getDeviseId().getId()
        );

        if (rowsAffected > 0) {
            return toSave;
        } else {

            return null;
        }
    }


    public Devise findDeviseById(String deviseId) {
        String query = "SELECT * FROM devise WHERE id = ?";
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, deviseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToDevise(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    private Devise mapResultSetToDevise(ResultSet resultSet) throws SQLException {
        Devise devise = new Devise();
        devise.setId(resultSet.getString("id"));
        devise.setName(resultSet.getString("name"));
        devise.setSymbol(resultSet.getString("symbol"));
        return devise;
    }
}
