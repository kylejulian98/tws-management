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
}
