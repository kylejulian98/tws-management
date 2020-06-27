package dev.kylejulian.tws.data;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kylejulian.tws.data.entities.MojangUserModel;
import dev.kylejulian.tws.data.entities.MojangUserNameModel;

public class MojangApi {

	private final Logger logger;
	private final Map<String, UUID> cachedIds;

	public MojangApi(Logger logger) {
		this.logger = logger;
		this.cachedIds = new HashMap<>();
	}

    public CompletableFuture<UUID> getPlayerId(String name) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				if (this.cachedIds.containsKey(name)) {
					this.logger.log(Level.FINE, "[MojangApi] Cache hit");
					return this.cachedIds.get(name);
				}

				this.logger.log(Level.FINE, "[MojangApi] Cache miss");

				String url = "https://api.mojang.com/users/profiles/minecraft/" + name;
				URL obj = new URL(url);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();

				con.setRequestMethod("GET");
				con.setRequestProperty("User-Agent", "Mozilla/5.0");

				ObjectMapper mapper = new ObjectMapper();
				MojangUserModel user = mapper.readValue(con.getInputStream(), MojangUserModel.class);

				UUID userId = user.getId();
				this.cachedIds.put(name, userId);

				return userId;
			}
			catch (IOException e) {
				this.logger.log(Level.INFO, "Unable to find Player {0} from the Mojang Api", name);
			}

			return null;
		});
	}
	
	public CompletableFuture<String> getPlayerName(UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				if (this.cachedIds.containsValue(playerId)) {
					for (Map.Entry<String, UUID> entry : this.cachedIds.entrySet()) {
						if (entry.getValue().equals(playerId)) {
							this.logger.log(Level.FINE, "[MojangApi] Cache hit");
							return entry.getKey();
						}
					}
				}

				this.logger.log(Level.FINE, "[MojangApi] Cache miss");

				String playerIdString = playerId.toString();

				String url = "https://api.mojang.com/user/profiles/" + playerIdString.replace("-", "") + "/names";
				URL obj = new URL(url);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();

				ObjectMapper mapper = new ObjectMapper();
				MojangUserNameModel[] userNames = mapper.readValue(con.getInputStream(), MojangUserNameModel[].class);

				int numberOfNames = userNames.length;
				MojangUserNameModel lastNameChanged = userNames[numberOfNames - 1];

				String name = lastNameChanged.getName();
				this.cachedIds.put(name, playerId);

				return name;
			}
			catch (IOException e) {
				this.logger.log(Level.INFO, "Unable to find PlayerId {0} from the Mojang Api", playerId);
			}

			return null;
		});
	}
}
