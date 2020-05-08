package dev.kylejulian.tws.extensions;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import dev.kylejulian.tws.data.MojangUserModel;

public class MojangUserModelDeserializer extends JsonDeserializer<MojangUserModel> {

	@Override
	public MojangUserModel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		MojangUserModel user = new MojangUserModel();
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		
		final UUID id = UUID.fromString(insertDashUUID(node.get("id").asText()));
		final String name = node.get("name").asText();
		
		user.setId(id);
		user.setName(name);
		
		return user;
	}
	
	private String insertDashUUID(String uuid) {
        StringBuffer sb = new StringBuffer(uuid);
        sb.insert(8, "-");
         
        sb = new StringBuffer(sb.toString());
        sb.insert(13, "-");
         
        sb = new StringBuffer(sb.toString());
        sb.insert(18, "-");
         
        sb = new StringBuffer(sb.toString());
        sb.insert(23, "-");
         
        return sb.toString();
    }

}
