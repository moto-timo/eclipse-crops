package org.yocto.crops.zephyr.model;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class KernelTypesDeserializer implements JsonDeserializer<KernelTypes> {
	@Override
	public KernelTypes deserialize(final JsonElement json, final Type typeOf, final JsonDeserializationContext context)
		throws JsonParseException {
		
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(KernelType.class, new KernelTypeDeserializer());
		Gson gson = gsonBuilder.create();
		
		final JsonArray jsonArray = json.getAsJsonArray();
		KernelType[] kernelTypeArray = (KernelType[]) gson.fromJson(jsonArray.getAsJsonArray(), KernelType[].class);
		Set<KernelType> kernelTypeSet = new HashSet<>(Arrays.asList(kernelTypeArray));
		final KernelTypes kernelTypes = new KernelTypes();
		kernelTypes.setKernelTypes(kernelTypeSet);
		return kernelTypes;
	}
}
