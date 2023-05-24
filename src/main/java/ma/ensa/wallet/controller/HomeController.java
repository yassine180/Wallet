package ma.ensa.wallet.controller;

import ma.ensa.wallet.bitcoin.MyWallet;
import ma.ensa.wallet.bitcoin.TransactionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class HomeController {
    @Autowired
    private MyWallet myWallet;

    @RequestMapping(value = "/index")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping(value = "/getBalance")
    public String getBalance() {
        return myWallet.getBalance().toFriendlyString();
    }

    @RequestMapping(value = "/getTransactions")
    public List<TransactionInfo> getTransactions() {
        return myWallet.getTransactions();
    }

    @RequestMapping(value = "/send")
    public String send(@RequestParam String amount, @RequestParam String address) {
        myWallet.send(amount, address);
        return "Done!";
    }
}
