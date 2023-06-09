package ma.ensa.wallet.response;


import lombok.Data;

@Data
public class TransactionInfo {
    private String value;
    private String hash;
    private String date;
}
