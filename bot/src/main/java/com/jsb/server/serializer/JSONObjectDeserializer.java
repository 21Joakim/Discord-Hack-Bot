package com.jsb.server.serializer;

import java.io.IOException;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class JSONObjectDeserializer extends StdDeserializer<JSONObject> {
	
	private static final long serialVersionUID = 1L;
	
	public JSONObjectDeserializer() {
		this(null);
	}
	
	public JSONObjectDeserializer(Class<?> valueClass) {
		super(valueClass);
	}
	
	public JSONObject deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
		return new JSONObject(parser.getCodec().readTree(parser).toString());
	}
}