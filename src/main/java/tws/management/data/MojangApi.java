package tws.management.data;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MojangApi {

	public UUID getPlayerId(String name) throws Exception {
		String url = "https://api.mojang.com/users/profiles/minecraft/" + name;
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");

		ObjectMapper mapper = new ObjectMapper();
		MojangUserModel user = mapper.readValue(con.getInputStream(), MojangUserModel.class);

		return user.getId();
	}
	
	public String getPlayerName(UUID playerId) throws Exception {
        String playerIdString = playerId.toString();

        String url = "https://api.mojang.com/user/profiles/" + playerIdString.replace("-", "") + "/names";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        
		ObjectMapper mapper = new ObjectMapper();
		MojangUserNameModel[] userNames = mapper.readValue(con.getInputStream(), MojangUserNameModel[].class);
		
		int numberOfNames = userNames.length;
		MojangUserNameModel lastNameChanged = userNames[numberOfNames - 1];
		
		return lastNameChanged.getName();
	}
}
