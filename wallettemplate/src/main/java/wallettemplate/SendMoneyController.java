package wallettemplate;

import com.google.infinitecoinj.core.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import org.slf4j.LoggerFactory;
import wallettemplate.controls.BitcoinAddressValidator;

import java.math.BigInteger;

import static wallettemplate.Main.infinitecoin;
import static wallettemplate.utils.GuiUtils.crashAlert;
import static wallettemplate.utils.GuiUtils.informationalAlert;

public class SendMoneyController {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SendMoneyController.class);

    public Button sendBtn;
    public Button cancelBtn;

    public TextField address;
    public TextField amount;
    public PasswordField password;
    public ChoiceBox<String> changeAddress;

    public Label titleLabel;

    public Label addressLabel;
    public Label amountLabel;
    public Label passwordLabel;
    public Label changeAddressLabel;

    public Main.OverlayUI overlayUi;

    public Alert msgAlert;

    private Wallet.SendResult sendResult;

    // Called by FXMLLoader
    public void initialize() {
        new BitcoinAddressValidator(Main.params, address, sendBtn);
        msgAlert = new Alert(Alert.AlertType.CONFIRMATION);
        changeAddress.getSelectionModel().selectFirst();
    }

    public void cancel(ActionEvent event) {
        overlayUi.done();
    }

    public void send(ActionEvent event) {
        String typedPassword = password.getText();
        if(infinitecoin.wallet().isEncrypted() && (null == typedPassword || typedPassword.isEmpty())){
            showAlertMsg("wallet is encrypted,please provide password first!!!");
            return;
        }
        if(infinitecoin.wallet().isEncrypted() && !infinitecoin.wallet().checkPassword(typedPassword)){
            showAlertMsg("password is not right,please try again?!!!");
            return;
        }
        try {
            Address destination = new Address(Main.params, address.getText());
            log.info("send coin to address:{},amount:{}",address.getText(),amount.getText());
            //Wallet.SendRequest req = Wallet.SendRequest.emptyWallet(destination);
            Wallet.SendRequest req = Wallet.SendRequest.to(destination,Utils.toNanoCoins(amount.getText()));
            //set fee and fee perKb to 1
            req.fee = BigInteger.ONE;
            req.feePerKb = BigInteger.ONE;
            Main.infinitecoin.wallet().sendCoins(req);
            if(infinitecoin.wallet().isEncrypted()){
                req.aesKey = infinitecoin.wallet().getKeyCrypter().deriveKey(typedPassword);
            }
            log.info("set change address:{}",changeAddress.getValue());
            req.changeAddress = new Address(Main.params, changeAddress.getValue());
            Futures.addCallback(sendResult.broadcastComplete, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(Transaction result) {
                    Platform.runLater(overlayUi::done);
                }

                @Override
                public void onFailure(Throwable t) {
                    // We died trying to empty the wallet.
                    crashAlert(t);
                }
            });
            sendResult.tx.getConfidence().addEventListener((tx, reason) -> {
                if (reason == TransactionConfidence.Listener.ChangeReason.SEEN_PEERS)
                    updateTitleForBroadcast();
            });
            sendBtn.setDisable(true);
            address.setDisable(true);
            updateTitleForBroadcast();
        } catch (AddressFormatException e) {
            // Cannot happen because we already validated it when the text field changed.
            throw new RuntimeException(e);
        } catch (InsufficientMoneyException e) {
            informationalAlert("Could not empty the wallet",
                    "You may have too little money left in the wallet to make a transaction.");
            overlayUi.done();
        }
    }

    private void updateTitleForBroadcast() {
        final int peers = sendResult.tx.getConfidence().numBroadcastPeers();
        titleLabel.setText(String.format("Broadcasting ... seen by %d peers", peers));
    }

    public void showAlertMsg(String msg){
        msgAlert.setContentText(msg);
        msgAlert.show();
    }
}
