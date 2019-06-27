package com.jsb.server.serializer;

import java.io.IOException;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class JSONObjectSerializer extends StdSerializer<JSONObject> {
	
	private static final long serialVersionUID = 1L;
	
	public JSONObjectSerializer() {
		this(null);
	}
	
	public JSONObjectSerializer(Class<JSONObject> type) {
		super(type);
	}
	
	public void serialize(JSONObject value, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeRaw(value.toString());
	}
}