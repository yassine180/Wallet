package ma.ensa.wallet.bitcoin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;

@Component
public class MyWallet {

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

    public Coin getBalance() {
        Wallet wallet = walletAppKit.wallet();
        return wallet.getBalance();
    }

    public List<TransactionInfo> getTransactions() {
        List<TransactionInfo> transactionInfos = new ArrayList<>();
        Wallet wallet = walletAppKit.wallet();

        for (Transaction transaction : wallet.getTransactionsByTime()) {
            TransactionInfo transactionInfo = new TransactionInfo();
            transactionInfo.setTxId(transaction.getTxId().toString());
            transactionInfo.setValue(transaction.getValue(wallet).toFriendlyString());
            transactionInfo.setHash(transaction.getHashAsString());
            transactionInfo.setReceiver(getReceiverAddress(transaction));
            transactionInfo.setSender(getSenderAddress(transaction));

            transactionInfos.add(transactionInfo);
        }

        return transactionInfos;
    }

    private String getReceiverAddress(Transaction transaction) {
        for (TransactionOutput output : transaction.getOutputs()) {
            Script script = output.getScriptPubKey();
            if (script.isSentToAddress()) {
                return script.getToAddress(networkParameters).toString();
            }
        }
        return "";
    }

    private String getSenderAddress(Transaction transaction) {
        Wallet wallet = walletAppKit.wallet();
        for (TransactionInput input : transaction.getInputs()) {
            Script scriptSig = input.getScriptSig();
            if (scriptSig.isSentToAddress()) {
                Address address = scriptSig.getToAddress(networkParameters);
                if (wallet.isAddressMine(address)) {
                    return address.toString();
                }
            } else if (scriptSig.isPayToScriptHash()) {
                List<ScriptChunk> chunks = scriptSig.getChunks();
                if (chunks.size() >= 2) {
                    byte[] redeemScriptBytes = chunks.get(chunks.size() - 2).data;
                    Script redeemScript = new Script(redeemScriptBytes);
                    Address address = redeemScript.getToAddress(networkParameters);
                    if (wallet.isAddressMine(address)) {
                        return address.toString();
                    }
                }
            }
        }
        return "";
    }

    public void send(String value, String to) {
        try {
            Address toAddress = LegacyAddress.fromBase58(networkParameters, to);
            SendRequest sendRequest = SendRequest.to(toAddress, Coin.parseCoin(value));
            sendRequest.feePerKb = Coin.parseCoin("0.0005");
            Wallet.SendResult sendResult = walletAppKit.wallet().sendCoins(walletAppKit.peerGroup(), sendRequest);
            sendResult.broadcastComplete.addListener(() ->
                            System.out.println("Sent coins onwards! Transaction hash is " + sendResult.tx.getTxId()),
                    MoreExecutors.directExecutor());
        } catch (InsufficientMoneyException e) {
            throw  new RuntimeException(e);
        }
    }
}
