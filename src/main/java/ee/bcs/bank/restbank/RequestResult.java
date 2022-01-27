package ee.bcs.bank.restbank;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestResult {
    private int accountId;
    private int transactionId;
    private String message;
    private String error;
}
