package ma.ensa.wallet.response;


import lombok.Data;

@Data
public class SendResponse {
    private Integer error;
    private String message;
}
