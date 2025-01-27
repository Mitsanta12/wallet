

















package com.td2.wallet.controller;

import com.td2.wallet.model.Transaction;
import com.td2.wallet.model.TransferHistory;
import com.td2.wallet.repository.TransactionCrudOperations;
import com.td2.wallet.service.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequestMapping("/transaction")
@RestController
@AllArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    private final TransactionCrudOperations transactionCrudOperations;
    @GetMapping("/list")
    public List<Transaction> getAllTransaction(){
        return transactionService.getAll();
    }
    @PostMapping("/saveAll")
    public  List<Transaction> saveAllAccount(@RequestBody List<Transaction> transaction){
        return transactionService.saveAll(transaction);

    }
    @PostMapping("/money")
    public String transferMoney(@RequestParam String debitAccountId,
                                @RequestParam String creditAccountId,
                                @RequestParam BigDecimal amount) {
        try {
            transactionCrudOperations.transferMoney(debitAccountId, creditAccountId, amount);
            return "Transfer successful.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during transfer: " + e.getMessage();
        }
    }

    @GetMapping("/history")
    public List<TransferHistory> getTransferHistoryBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return transactionService.getTransferHistoryBetween(start, end);
    }
}

