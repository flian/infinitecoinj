package wallettemplate.utils;

import com.google.infinitecoinj.core.Address;
import com.google.infinitecoinj.core.ECKey;
import com.google.infinitecoinj.core.NetworkParameters;
import com.google.infinitecoinj.core.Wallet;

import java.util.List;

/**
 * @author : foy
 * @date : 2024/12/27:15:47
 **/
public class WalletUtils {
    public static ECKey getLastKey(Wallet wallet){
        List<ECKey> keys = wallet.getKeys();
        return keys.get(keys.size()-1);
    }

    public static Address getLastAddress(Wallet wallet){
        ECKey key=getLastKey(wallet);
        NetworkParameters parameters = wallet.getNetworkParameters();
        return key.toAddress(parameters);
    }
}
