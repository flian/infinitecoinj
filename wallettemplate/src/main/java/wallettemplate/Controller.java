package wallettemplate;

import com.google.infinitecoinj.core.*;
import com.google.zxing.common.StringUtils;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;
import org.slf4j.LoggerFactory;
import wallettemplate.controls.ClickableBitcoinAddress;
import wallettemplate.utils.WalletUtils;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static wallettemplate.Main.infinitecoin;
import static wallettemplate.utils.GuiUtils.checkGuiThread;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class Controller {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Controller.class);
    public ProgressBar syncProgress;
    public VBox syncBox;
    public HBox controlsBox;
    public Label balance;
    public Button sendMoneyOutBtn;

    public PasswordField password;
    public Button createNewAddressBtn;

    public PasswordField newPassword;

    public Button changePasswordBtn;

    public ClickableBitcoinAddress addressControl;

    public ListView<ClickableBitcoinAddress> addressListView;

    public Alert msgAlert;

    // Called by FXMLLoader.
    public void initialize() {
        syncProgress.setProgress(-1);
        addressControl.setOpacity(0.0);
        msgAlert = new Alert(Alert.AlertType.CONFIRMATION);
    }

    public void onBitcoinSetup() {
        infinitecoin.wallet().addEventListener(new BalanceUpdater());
        addressControl.setAddress(infinitecoin.wallet().getKeys().get(0).toAddress(Main.params).toString());
        refreshAddress();
        refreshBalanceLabel();
    }

    public void refreshAddress(){
        addressListView.getItems().clear();
        List<ECKey> listKeys = infinitecoin.wallet().getKeys();
        for(ECKey key:listKeys){
            ClickableBitcoinAddress clickableBitcoinAddress = new ClickableBitcoinAddress();
            clickableBitcoinAddress.setAddress(key.toAddress(Main.params).toString());
            addressListView.getItems().add(clickableBitcoinAddress);
        }
    }

    public void createNewAddress(ActionEvent event){
        //create new address here
        if(infinitecoin.wallet().getKeys().size() > 7){
            showAlertMsg("max address reached,max allow address is 8.");
            return;
        }
        String oldPassword = password.getText();
        if(infinitecoin.wallet().isEncrypted()){
            boolean isPassWordOK = infinitecoin.wallet().checkPassword(oldPassword);
            if(isPassWordOK){
                infinitecoin.wallet().addNewEncryptedKey(oldPassword);
            }else {
                showAlertMsg("wrong password!!");
                return;
            }
        }else {
            infinitecoin.wallet().addKey(new ECKey());
        }
        log.info("create new address success:{}",WalletUtils.getLastAddress(infinitecoin.wallet()));
        showAlertMsg("create newAddress success,address is:"+WalletUtils.getLastAddress(infinitecoin.wallet()));
        refreshAddress();
        password.clear();
        newPassword.clear();
    }

    public void changePassword(ActionEvent event){
        String oldPassword = password.getText();
        String newPasswordStr = newPassword.getText();
        if(null == newPasswordStr || newPasswordStr.isEmpty()){
            showAlertMsg("please input your new password!!");
            return;
        }
        if(infinitecoin.wallet().isEncrypted()){
            boolean isPassWordOK = infinitecoin.wallet().checkPassword(oldPassword);
            if(isPassWordOK){
                infinitecoin.wallet().decrypt(infinitecoin.wallet().getKeyCrypter().deriveKey(oldPassword));
                infinitecoin.wallet().encrypt(newPasswordStr);
            }else {
                showAlertMsg("old password wrong,forget your password??");
                return;
            }
        }else {
            infinitecoin.wallet().encrypt(newPasswordStr);
        }
        showAlertMsg("change password ok~~ please remember your new password~~ it's very important！！！！");
        password.clear();
        newPassword.clear();
    }

    public void showAlertMsg(String msg){
        msgAlert.setContentText(msg);
        msgAlert.show();
    }
    public void sendMoneyOut(ActionEvent event) {
        // Hide this UI and show the send money UI. This UI won't be clickable until the user dismisses send_money.
        Main.OverlayUI<SendMoneyController> newUI =  Main.instance.overlayUI("send_money.fxml");
        SendMoneyController sendMoneyController = newUI.controller;
        //set change address select.
        List<String> addressList = infinitecoin.wallet().getKeys().stream().map(k->k.toAddress(Main.params).toString()).collect(Collectors.toList());
        sendMoneyController.changeAddress.setItems(FXCollections.observableArrayList(addressList));
        //set transfer amount
        sendMoneyController.amount.setTextFormatter(new TextFormatter<>(new NumberStringConverter()));
    }

    public class ProgressBarUpdater extends DownloadListener {
        @Override
        protected void progress(double pct, int blocksSoFar, Date date) {
            super.progress(pct, blocksSoFar, date);
            Platform.runLater(() -> syncProgress.setProgress(pct / 100.0));
        }

        @Override
        protected void doneDownload() {
            super.doneDownload();
            Platform.runLater(Controller.this::readyToGoAnimation);
        }
    }

    public void readyToGoAnimation() {
        // Sync progress bar slides out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(600), syncBox);
        leave.setByY(80.0);
        // Buttons slide in and clickable address appears simultaneously.
        TranslateTransition arrive = new TranslateTransition(Duration.millis(600), controlsBox);
        arrive.setToY(0.0);
        FadeTransition reveal = new FadeTransition(Duration.millis(500), addressControl);
        reveal.setToValue(1.0);
        ParallelTransition group = new ParallelTransition(arrive, reveal);
        // Slide out happens then slide in/fade happens.
        SequentialTransition both = new SequentialTransition(leave, group);
        both.setCycleCount(1);
        both.setInterpolator(Interpolator.EASE_BOTH);
        both.play();
    }

    public ProgressBarUpdater progressBarUpdater() {
        return new ProgressBarUpdater();
    }

    public class BalanceUpdater extends AbstractWalletEventListener {
        @Override
        public void onWalletChanged(Wallet wallet) {
            checkGuiThread();
            refreshBalanceLabel();
        }
    }

    public void refreshBalanceLabel() {
        final BigInteger amount = infinitecoin.wallet().getBalance(Wallet.BalanceType.ESTIMATED);
        balance.setText(Utils.bitcoinValueToFriendlyString(amount));
    }
}
