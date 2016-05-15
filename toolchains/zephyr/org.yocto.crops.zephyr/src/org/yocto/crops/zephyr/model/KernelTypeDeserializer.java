package org.yocto.crops.zephyr.model;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class KernelTypeDeserializer implements JsonDeserializer<KernelType> {
	@Override
	public KernelType deserialize(final JsonElement json, final Type typeOf, final JsonDeserializationContext context)
		throws JsonParseException {
		
		final JsonObject jsonObject = json.getAsJsonObject();
		
		// Delegate deserialization to the context
		final KernelType kernelType = context.deserialize(jsonObject.getAsJsonObject(), KernelType.class);
		return kernelType;
	}
}
