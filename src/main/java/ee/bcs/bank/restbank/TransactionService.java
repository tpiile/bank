package ee.bcs.bank.restbank;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    public static final String ATM = "ATM";
    public static final char NEW_ACCOUNT = 'n';
    public static final char DEPOSIT = 'd';
    public static final char WITHDRAWAL = 'w';
    public static final char SEND_MONEY = 's';
    public static final char RECEIVE_MONEY = 'r';

    @Resource
    private AccountService accountService;

    @Resource
    private BalanceService balanceService;

//    transactionType
//    n - new account
//    d - deposit
//    w - withdrawal
//    s - send money
//    r - receive money


    // TODO:    createExampleTransaction()
    //  account id 123
    //  balance 1000
    //  amount 100
    //  transactionType 's'
    //  receiver EE123
    //  sender EE456

    public TransactionDto createExampleTransaction() {
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setAccountId(123);
        transactionDto.setBalance(1000);
        transactionDto.setAmount(100);
        transactionDto.setTransactionType(SEND_MONEY);
        transactionDto.setReceiverAccountNumber("EE123");
        transactionDto.setSenderAccountNumber("EE456");
        transactionDto.setLocalDateTime(LocalDateTime.now());
        return transactionDto;
    }

    public RequestResult addNewTransaction(Bank bank, TransactionDto transactionDto) {
        // loo vajalikud objektid (tühjad)

        RequestResult requestResult = new RequestResult();

        // vajalike andmete lisamine muutujatesse
        List<AccountDto> accounts = bank.getAccounts();
        int accountId = transactionDto.getAccountId();

        // kontrolli kas konto eksisteerib
        if (!accountService.accountIdExists(accounts, accountId)) {
            requestResult.setAccountId(accountId);
            requestResult.setError("Account ID " + accountId + " dose not exist!");
            return requestResult;
        }

        Character transactionType = transactionDto.getTransactionType();
        Integer amount = transactionDto.getAmount();
        int transactionId = bank.getTransactionIdCount();

        // päri välja accountId abiga õige konta
        AccountDto account = accountService.getAccountById(accounts, accountId);
        Integer balance = account.getBalance();

        // töötleme läbi erinevad olukorrad
        int newBalance;
        String receiverAccountNumber;


        switch (transactionType) {

            case NEW_ACCOUNT:

                // täidame ära transactionDto
                transactionDto.setSenderAccountNumber(null);
                transactionDto.setReceiverAccountNumber(null);
                transactionDto.setBalance(0);
                transactionDto.setAmount(0);
                transactionDto.setLocalDateTime(LocalDateTime.now());
                transactionDto.setId(transactionId);

                // lisame tehingu transactionte alla (pluss incrementeerime)
                bank.addTransactionToTransaction(transactionDto);
                bank.incrementTransactionId();

                // meisterdame valemis result objekti
                requestResult.setTransactionId(transactionId);
                requestResult.setAccountId(accountId);
                requestResult.setMessage("Successfully added 'new account' transaction");
                return requestResult;


            case DEPOSIT:
                // arvutame uue balance
                newBalance = balance + amount;
                // täidame ära transactionDto
                transactionDto.setSenderAccountNumber(ATM);
                transactionDto.setSenderAccountNumber(account.getAccountNumber());
                transactionDto.setBalance(newBalance);
                transactionDto.setLocalDateTime(LocalDateTime.now());
                transactionDto.setId(transactionId);

                // lisame tehingu transactionte alla (pluss inkrementeerime)
                bank.addTransactionToTransaction(transactionDto);
                bank.incrementTransactionId();

                // uuendame konto balance
                account.setBalance(newBalance);

                // meisterdame valemis result objekti
                requestResult.setTransactionId(transactionId);
                requestResult.setAccountId(accountId);
                requestResult.setMessage("Successfully made deposited " + amount);
                return requestResult;

            case WITHDRAWAL:
                // arvutame uue balance
                if (!balanceService.enoughMoneyOnAccount(balance, amount)) {
                    requestResult.setAccountId(accountId);
                    requestResult.setError("Not enough money: " + amount);
                    return requestResult;
                }

                newBalance = balance - amount;

                // täidame ära transactionDto
                transactionDto.setSenderAccountNumber(ATM);
                transactionDto.setSenderAccountNumber(account.getAccountNumber());

                transactionDto.setBalance(newBalance);
                transactionDto.setLocalDateTime(LocalDateTime.now());
                transactionDto.setId(transactionId);

                // lisame tehingu transactionte alla (pluss inkrementeerime)
                bank.addTransactionToTransaction(transactionDto);
                bank.incrementTransactionId();

                // uuendame konto balance
                account.setBalance(newBalance);

                // meisterdame valemis result objekti
                requestResult.setTransactionId(transactionId);
                requestResult.setAccountId(accountId);
                requestResult.setMessage("Successfully made withdrawal " + amount);
                return requestResult;

            case SEND_MONEY:
                // kontrollime kas saatjal on piisavalt raha
                if (!balanceService.enoughMoneyOnAccount(balance, amount)) {
                    requestResult.setAccountId(accountId);
                    requestResult.setError("Not enough money: " + amount);
                    return requestResult;
                }
                // arvutame välja uue balance
                newBalance = balance - amount;

                // täidame ära transactionDto
                transactionDto.setSenderAccountNumber(account.getAccountNumber());
                transactionDto.setBalance(newBalance);
                transactionDto.setLocalDateTime(LocalDateTime.now());
                transactionDto.setId(transactionId);

                // lisame tehingu transactionte alla (pluss inkrementeerime)
                bank.addTransactionToTransaction(transactionDto);
                bank.incrementTransactionId();

                // uuendame konto balance
                account.setBalance(newBalance);

                // meisterdame valemis result objekti
                requestResult.setTransactionId(transactionId);
                requestResult.setAccountId(accountId);
                requestResult.setMessage("Successfully sent money");

                //teeme SAAJA transaktsiooni
                receiverAccountNumber = transactionDto.getReceiverAccountNumber();

                // kontrollime kas saaja kontonumber eksisteerib meie andmebaasis (bank)
                if (accountService.accountNumberExists(accounts, receiverAccountNumber)) {
                    AccountDto receiverAccount = accountService.getAccountByNumber(accounts, receiverAccountNumber);
                    int receiverNewBalance = receiverAccount.getBalance() + amount;


                    // loome uue transaktsiooni objekti
                    TransactionDto receiverTransactionDto = new TransactionDto();

                    // täidame ära transactionDto
                    receiverTransactionDto.setSenderAccountNumber(account.getAccountNumber());
                    receiverTransactionDto.setReceiverAccountNumber(receiverAccountNumber);
                    receiverTransactionDto.setBalance(receiverNewBalance);
                    receiverTransactionDto.setLocalDateTime(LocalDateTime.now());
                    receiverTransactionDto.setId(bank.getTransactionIdCount());
                    receiverTransactionDto.setAmount(amount);
                    receiverTransactionDto.setTransactionType(RECEIVE_MONEY);

                    //lisame tehingu translatsionide alla (pluss inkrementeerimine)
                    bank.addTransactionToTransaction(receiverTransactionDto);
                    bank.incrementTransactionId();
                    receiverAccount.setBalance(receiverNewBalance);

                }

                return requestResult;


            default:
                requestResult.setError("Unknown transaction type: " + transactionType);
                return requestResult;
        }

    }

    public RequestResult receiveNewTransaction(Bank bank, TransactionDto transactionDto) {
        RequestResult requestResult = new RequestResult();
        String receiverAccountNumber = transactionDto.getReceiverAccountNumber();
        List<AccountDto> accounts = bank.getAccounts();

        if (!accountService.accountNumberExists(accounts, receiverAccountNumber)) {
            requestResult.setError("No such account in our bank. " + receiverAccountNumber);
            return requestResult;
        }

        AccountDto receiverAccount = accountService.getAccountByNumber(accounts, receiverAccountNumber);
        int transactionId = bank.getTransactionIdCount();
        int receiverNewBalance = receiverAccount.getBalance() + transactionDto.getAmount();

        transactionDto.setTransactionType(RECEIVE_MONEY);
        transactionDto.setBalance(receiverNewBalance);
        transactionDto.setId(transactionId);
        transactionDto.setAccountId(receiverAccount.getBalance());
        transactionDto.setLocalDateTime(LocalDateTime.now());

        bank.addTransactionToTransaction(transactionDto);
        bank.incrementTransactionId();
        receiverAccount.setBalance(receiverNewBalance);

        requestResult.setTransactionId(transactionId);
        requestResult.setMessage("Transaction received");
        return requestResult;



    }


    // TODO:    createTransactionForNewAccount()
    //  account number
    //  balance 0
    //  amount 0
    //  transactionType 'n'
    //  receiver jääb null
    //  sender jääb null


}
