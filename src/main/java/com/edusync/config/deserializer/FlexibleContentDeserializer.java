package com.edusync.config.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Deserializador flexível para campos que aceitam String ou objeto JSON.
 * Usado em {@link com.edusync.dto.request.CreateTaskRequest#content()}.
 */
public class FlexibleContentDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            return parser.getText();
        }
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        // Objeto ou array JSON: serializa de volta para String para persistência.
        return parser.readValueAsTree().toString();
    }
}
