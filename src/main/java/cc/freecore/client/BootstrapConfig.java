package cc.freecore.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/** Local bootstrap settings read before the remote client configuration. */
public final class BootstrapConfig {
    public static final Gson GSON = new GsonBuilder().create();
    @SerializedName("remote_config_url")
    public String remoteConfigUrl = "YOUR_GITHUB_RAW_CONFIG_URL_HERE";
    /** GitHub Releases API endpoint used for binary client updates. */
    @SerializedName("client_update_api_url")
    public String clientUpdateApiUrl = "https://api.github.com/repos/CoreNyan/FreeCoreClient/releases/latest";
    @SerializedName("client_update_enabled")
    public boolean clientUpdateEnabled = true;
    /** Optional asset filename prefix; the first matching .jar is selected. */
    @SerializedName("client_update_asset_prefix")
    public String clientUpdateAssetPrefix = "freecore-client";
}
