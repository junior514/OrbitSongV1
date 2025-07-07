package com.sise.orbitsongv1.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * TypeAdapter personalizado para manejar el campo "artistas" que puede venir
 * como String o como Array desde diferentes endpoints del backend.
 */
public class ArtistasTypeAdapter extends TypeAdapter<List<String>> {

    @Override
    public void write(JsonWriter out, List<String> value) throws IOException {
        if (value == null || value.isEmpty()) {
            out.nullValue();
            return;
        }

        // Siempre serializar como array
        out.beginArray();
        for (String artista : value) {
            out.value(artista);
        }
        out.endArray();
    }

    @Override
    public List<String> read(JsonReader in) throws IOException {
        List<String> artistas = new ArrayList<>();

        JsonToken token = in.peek();

        switch (token) {
            case BEGIN_ARRAY:
                // Caso 1: Es un array ["Artista1", "Artista2"]
                in.beginArray();
                while (in.hasNext()) {
                    String artista = in.nextString();
                    if (artista != null && !artista.trim().isEmpty()) {
                        artistas.add(artista.trim());
                    }
                }
                in.endArray();
                break;

            case STRING:
                // Caso 2: Es un string "Artista1, Artista2" o "Artista1"
                String artistasString = in.nextString();
                if (artistasString != null && !artistasString.trim().isEmpty()) {
                    // Si contiene comas, dividir
                    if (artistasString.contains(",")) {
                        String[] artistasArray = artistasString.split(",");
                        for (String artista : artistasArray) {
                            String trimmedArtista = artista.trim();
                            if (!trimmedArtista.isEmpty()) {
                                artistas.add(trimmedArtista);
                            }
                        }
                    } else {
                        // Un solo artista
                        artistas.add(artistasString.trim());
                    }
                }
                break;

            case NULL:
                // Caso 3: Es null
                in.nextNull();
                break;

            default:
                throw new JsonParseException("Expected String or Array for artistas field, but was " + token);
        }

        return artistas;
    }
}

/**
 * Alternativa usando JsonDeserializer (puedes usar cualquiera de los dos enfoques)
 */
/*
public class ArtistasDeserializer implements JsonDeserializer<List<String>>, JsonSerializer<List<String>> {

    @Override
    public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        List<String> artistas = new ArrayList<>();

        if (json.isJsonNull()) {
            return artistas;
        }

        if (json.isJsonArray()) {
            // Caso 1: Es un array
            for (JsonElement element : json.getAsJsonArray()) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    String artista = element.getAsString();
                    if (artista != null && !artista.trim().isEmpty()) {
                        artistas.add(artista.trim());
                    }
                }
            }
        } else if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            // Caso 2: Es un string
            String artistasString = json.getAsString();
            if (artistasString != null && !artistasString.trim().isEmpty()) {
                if (artistasString.contains(",")) {
                    String[] artistasArray = artistasString.split(",");
                    for (String artista : artistasArray) {
                        String trimmedArtista = artista.trim();
                        if (!trimmedArtista.isEmpty()) {
                            artistas.add(trimmedArtista);
                        }
                    }
                } else {
                    artistas.add(artistasString.trim());
                }
            }
        } else {
            throw new JsonParseException("Expected String or Array for artistas field");
        }

        return artistas;
    }

    @Override
    public JsonElement serialize(List<String> src, Type typeOfSrc, JsonSerializationContext context) {
        // Siempre serializar como array
        return context.serialize(src);
    }
}
*/