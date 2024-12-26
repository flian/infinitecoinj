package wallettemplate;

import com.google.infinitecoinj.core.AbstractWalletEventListener;
import com.google.infinitecoinj.core.DownloadListener;
import com.google.infinitecoinj.core.Utils;
import com.google.infinitecoinj.core.Wallet;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;
import wallettemplate.controls.ClickableBitcoinAddress;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static wallettemplate.Main.bitcoin;
import static wallettemplate.utils.GuiUtils.checkGuiThread;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class Controller {
    public ProgressBar syncProgress;
    public VBox syncBox;
    public HBox controlsBox;
    public Label balance;
    public Button sendMoneyOutBtn;

    public PasswordField password;

    public Button createNewAddressBtn;

    public ClickableBitcoinAddress addressControl;

    // Called by FXMLLoader.
    public void initialize() {
        syncProgress.setProgress(-1);
        addressControl.setOpacity(0.0);
    }

    public void onBitcoinSetup() {
        bitcoin.wallet().addEventListener(new BalanceUpdater());
        addressControl.setAddress(bitcoin.wallet().getKeys().get(0).toAddress(Main.params).toString());
        refreshBalanceLabel();
    }

    public void createNewAddress(ActionEvent event){
        //TODO create new address here
    }

    public void sendMoneyOut(ActionEvent event) {
        // Hide this UI and show the send money UI. This UI won't be clickable until the user dismisses send_money.
        Main.OverlayUI<SendMoneyController> newUI =  Main.instance.overlayUI("send_money.fxml");
        SendMoneyController sendMoneyController = newUI.controller;
        //set change address select.
        List<String> addressList = bitcoin.wallet().getKeys().stream().map(k->k.toAddress(Main.params).toString()).collect(Collectors.toList());
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
        final BigInteger amount = bitcoin.wallet().getBalance(Wallet.BalanceType.ESTIMATED);
        balance.setText(Utils.bitcoinValueToFriendlyString(amount));
    }
}
