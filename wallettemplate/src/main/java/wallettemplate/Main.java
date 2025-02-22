package wallettemplate;


import com.google.infinitecoinj.core.NetworkParameters;
import com.google.infinitecoinj.kits.WalletAppKit;
import com.google.infinitecoinj.params.MainNetParams;
import com.google.infinitecoinj.params.RegTestParams;
import com.google.infinitecoinj.store.BlockStoreException;
import com.google.infinitecoinj.utils.BriefLogFormatter;
import com.google.infinitecoinj.utils.Threading;
import com.google.common.base.Throwables;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;
import wallettemplate.utils.GuiUtils;
import wallettemplate.utils.TextFieldValidator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;

import static wallettemplate.utils.GuiUtils.*;

public class Main extends Application {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);
    public static String APP_NAME = "WalletTemplate";

    public static NetworkParameters params = MainNetParams.get();
    public static String walletFolder = "mainNet";
    public boolean loadCheckPoint = true;

    public static WalletAppKit infinitecoin;
    public static Main instance;

    private StackPane uiStack;
    private Pane mainUI;

    private static String regtestHost = null;

    private static boolean isRegTest = false;

    @Override
    public void start(Stage mainWindow) throws Exception {
        instance = this;
        // Show the crash dialog for any exceptions that we don't handle and that hit the main loop.
        GuiUtils.handleCrashesOnThisThread();
        try {
            init(mainWindow);
        } catch (Throwable t) {
            // Nicer message for the case where the block store file is locked.
            if (Throwables.getRootCause(t) instanceof BlockStoreException) {
                GuiUtils.informationalAlert("Already running", "This application is already running and cannot be started twice.");
            } else {
                throw t;
            }
        }
    }

    private void init(Stage mainWindow) throws IOException {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            //FIXME Using it will cause issue in mac??
           // AquaFx.style();
        }
        // Load the GUI. The Controller class will be automagically created and wired up.
        URL location = getClass().getResource("main.fxml");
        FXMLLoader loader = new FXMLLoader(location);
        mainUI = loader.load();
        Controller controller = loader.getController();
        // Configure the window with a StackPane so we can overlay things on top of the main UI.
        uiStack = new StackPane(mainUI);
        mainWindow.setTitle(APP_NAME);
        final Scene scene = new Scene(uiStack);
        TextFieldValidator.configureScene(scene);   // Add CSS that we need.
        mainWindow.setScene(scene);

        // Make log output concise.
        BriefLogFormatter.init();
        // Tell bitcoinj to run event handlers on the JavaFX UI thread. This keeps things simple and means
        // we cannot forget to switch threads when adding event handlers. Unfortunately, the DownloadListener
        // we give to the app kit is currently an exception and runs on a library thread. It'll get fixed in
        // a future version.
        Threading.USER_THREAD = Platform::runLater;
        // Create the app kit. It won't do any heavyweight initialization until after we start it.
        infinitecoin = new WalletAppKit(params, new File("./"+walletFolder), APP_NAME);
        if (params == RegTestParams.get() && isRegTest) {
            //bitcoin.connectToLocalHost();   // You should run a regtest mode bitcoind locally.
            infinitecoin.connectToGivenHost(regtestHost);
        } else if (params == MainNetParams.get()) {
            // Checkpoints are block headers that ship inside our app: for a new user, we pick the last header
            // in the checkpoints file and then download the rest from the network. It makes things much faster.
            // Checkpoint files are made using the BuildCheckpoints tool and usually we have to download the
            // last months worth or more (takes a few seconds).
            if(loadCheckPoint){
                log.info("enable checkpoint,load checkPoint...");
                infinitecoin.setCheckpoints(getClass().getClassLoader().getResourceAsStream("mainNet/checkpoints_20250118"));
                log.info("enable checkpoint,load checkPoint done");
            }
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Now configure and start the appkit. This will take a second or two - we could show a temporary splash screen
        // or progress widget to keep the user engaged whilst we initialise, but we don't.
        infinitecoin.setDownloadListener(controller.progressBarUpdater())
               .setBlockingStartup(false)
               .setUserAgent(APP_NAME, "1.0")
               .startAndWait();
        // Don't make the user wait for confirmations for now, as the intention is they're sending it their own money!
        infinitecoin.wallet().allowSpendingUnconfirmedTransactions();
        infinitecoin.peerGroup().setMaxConnections(11);
        System.out.println(infinitecoin.wallet());
        System.out.println("wallet earliest create time:"+df.format(infinitecoin.wallet().getEarliestKeyCreationTime()*1000));
        controller.onBitcoinSetup();
        mainWindow.show();
    }

    public class OverlayUI<T> {
        public Node ui;
        public T controller;

        public OverlayUI(Node ui, T controller) {
            this.ui = ui;
            this.controller = controller;
        }

        public void show() {
            blurOut(mainUI);
            uiStack.getChildren().add(ui);
            fadeIn(ui);
        }

        public void done() {
            checkGuiThread();
            fadeOutAndRemove(ui, uiStack);
            blurIn(mainUI);
            this.ui = null;
            this.controller = null;
        }
    }

    public <T> OverlayUI<T> overlayUI(Node node, T controller) {
        checkGuiThread();
        OverlayUI<T> pair = new OverlayUI<T>(node, controller);
        // Auto-magically set the overlayUi member, if it's there.
        try {
            controller.getClass().getDeclaredField("overlayUi").set(controller, pair);
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }
        pair.show();
        return pair;
    }

    /** Loads the FXML file with the given name, blurs out the main UI and puts this one on top. */
    public <T> OverlayUI<T> overlayUI(String name) {
        try {
            checkGuiThread();
            // Load the UI from disk.
            URL location = getClass().getResource(name);
            FXMLLoader loader = new FXMLLoader(location);
            Pane ui = loader.load();
            T controller = loader.getController();
            OverlayUI<T> pair = new OverlayUI<T>(ui, controller);
            // Auto-magically set the overlayUi member, if it's there.
            try {
                controller.getClass().getDeclaredField("overlayUi").set(controller, pair);
            } catch (IllegalAccessException | NoSuchFieldException ignored) {
            }
            pair.show();
            return pair;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    @Override
    public void stop() throws Exception {
        infinitecoin.stopAndWait();
        super.stop();
    }

    public static void main(String[] args) {
        for(int i=0;i<args.length;i++){
            if(args[i].equals("-regtest")){
                //reg test net
                params = RegTestParams.get();
                walletFolder = "regTestNet";
                isRegTest = true;
                if(i<args.length-1){
                    regtestHost = args[i+1];
                }else {
                    regtestHost = "127.0.0.1";
                }
            }
        }
        launch(args);
    }
}
