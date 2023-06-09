package ma.ensa.wallet.bitcoin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import ma.ensa.wallet.response.Balance;
import ma.ensa.wallet.response.PublicAddress;
import ma.ensa.wallet.response.SendResponse;
import ma.ensa.wallet.response.TransactionInfo;
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class WalletCore {

    @JsonIgnore
    @Autowired
    private WalletAppKit walletAppKit;

    @JsonIgnore
    @Autowired
    private NetworkParameters networkParameters;

    private ObjectMapper objectMapper;


    @PostConstruct
    public void start() {
        walletAppKit.startAsync();
        walletAppKit.awaitRunning();

        this.objectMapper = new ObjectMapper();

        walletAppKit.wallet().addCoinsReceivedEventListener(
                (wallet, tx, prevBalance, newBalance) -> {
                    Coin value = tx.getValueSentToMe(wallet);
                    System.out.println("Received tx for " + value.toFriendlyString());
                    Futures.addCallback(tx.getConfidence().getDepthFuture(1),
                            new FutureCallback<TransactionConfidence>() {
                                @Override
                                public void onSuccess(TransactionConfidence result) {
                                    System.out.println("Received tx " +
                                            value.toFriendlyString() + " is confirmed. ");
                                }

                                @Override
                                public void onFailure(Throwable t) {}
                            }, MoreExecutors.directExecutor());
                });

        Address sendToAddress = LegacyAddress.fromKey(networkParameters, walletAppKit.wallet().currentReceiveKey());
        System.out.println("Send coins to: " + sendToAddress);
    }

    public PublicAddress getAddress() {
        PublicAddress address = new PublicAddress();
        Wallet wallet = walletAppKit.wallet();
        Address currentAddress = wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);

        address.setAddress(currentAddress.toString());
        return address;
    }

    public Balance getBalance() {
        Balance balance = new Balance();
        Wallet wallet = walletAppKit.wallet();

        balance.setBalance(wallet.getBalance().toFriendlyString());
        return balance;
    }

    public List<TransactionInfo> getTransactions() {
        List<TransactionInfo> transactionInfos = new ArrayList<>();
        Wallet wallet = walletAppKit.wallet();

        for (Transaction transaction : wallet.getTransactionsByTime()) {
            TransactionInfo transactionInfo = new TransactionInfo();
            transactionInfo.setValue(transaction.getValue(wallet).toFriendlyString());
            transactionInfo.setHash(transaction.getHashAsString());
            long timestamp = transaction.getUpdateTime().getTime();
            LocalDateTime date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
            transactionInfo.setDate(date.toString());

            transactionInfos.add(transactionInfo);
        }

        return transactionInfos;
    }

    public SendResponse send(String value, String to) {
        Coin amountToSend = Coin.parseCoin(value);
        Wallet wallet = walletAppKit.wallet();
        Coin balance = wallet.getBalance();

        SendResponse response = new SendResponse();

        if (amountToSend.isGreaterThan(balance)) {
            response.setError(1);
            response.setMessage("Insufficient balance. You can't send more than your current balance.");
            return response;
        }
        try {
            Address toAddress = LegacyAddress.fromBase58(networkParameters, to);
            SendRequest sendRequest = SendRequest.to(toAddress, Coin.parseCoin(value));
            sendRequest.feePerKb = Coin.parseCoin("0.0005");
            Wallet.SendResult sendResult = walletAppKit.wallet().sendCoins(walletAppKit.peerGroup(), sendRequest);
            sendResult.broadcastComplete.addListener(() ->
                            System.out.println("Sent coins onwards! Transaction hash is " + sendResult.tx.getTxId()),
                    MoreExecutors.directExecutor());
            response.setError(0);
            response.setMessage("Your Transactions is completed successfully.");
            return response;
        } catch (InsufficientMoneyException e) {
            response.setError(1);
            response.setMessage("An Error occur during the transaction, please try again.");
            return response;
        }
    }
}
