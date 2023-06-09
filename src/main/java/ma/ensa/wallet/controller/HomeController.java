package ma.ensa.wallet.controller;

import ma.ensa.wallet.bitcoin.WalletCore;
import ma.ensa.wallet.response.PublicAddress;
import ma.ensa.wallet.response.SendResponse;
import ma.ensa.wallet.response.TransactionInfo;
import ma.ensa.wallet.response.Balance;
import org.bitcoinj.core.Address;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@CrossOrigin(methods = RequestMethod.GET)
public class HomeController {
    @Autowired
    private WalletCore myWallet;

    @GetMapping(value = "/getBalance")
    public Balance getBalance() {
        return myWallet.getBalance();
    }

    @GetMapping(value = "/getAddress")
    public PublicAddress getAddress() {
        return myWallet.getAddress();
    }

    @GetMapping(value = "/getTransactions")
    public List<TransactionInfo> getTransactions() {
        return myWallet.getTransactions();
    }

    @GetMapping (value = "/send")
    public SendResponse send(@RequestParam String amount, @RequestParam String address) {
        return myWallet.send(amount, address);
    }
}
