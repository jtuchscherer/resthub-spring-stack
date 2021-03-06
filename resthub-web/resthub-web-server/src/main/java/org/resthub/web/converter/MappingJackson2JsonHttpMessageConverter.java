package org.resthub.web.converter;

/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.fasterxml.jackson.databind.*;
import org.resthub.common.view.DataView;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.resthub.web.PageResponse;
import org.springframework.data.domain.Page;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter} that can read
 * and write JSON using <a href="http://jackson.codehaus.org/">Jackson 2's</a> {@link ObjectMapper}.
 * 
 * <p>
 * This converter can be used to bind to typed beans, or untyped {@link java.util.HashMap HashMap} instances.
 * 
 * <p>
 * By default, this converter supports {@code application/json}. This can be overridden by setting the
 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} property.
 * 
 * @author Arjen Poutsma
 * @author Keith Donald
 * @since 3.2
 * @see org.springframework.web.servlet.view.json.MappingJackson2JsonView
 */
public class MappingJackson2JsonHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private ObjectMapper objectMapper;

    private boolean prefixJson = false;

    private Boolean prettyPrint;

    /**
     * Construct a new {@code BindingJacksonHttpMessageConverter}.
     */
    public MappingJackson2JsonHttpMessageConverter() {
        super(new MediaType("application", "json", DEFAULT_CHARSET));
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addAbstractTypeMapping(Page.class, PageResponse.class);
        objectMapper.registerModule(module);
        AnnotationIntrospector introspector = new JacksonAnnotationIntrospector();
        objectMapper.setAnnotationIntrospector(introspector);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);

    }

    /**
     * Set the {@code ObjectMapper} for this view. If not set, a default {@link ObjectMapper#ObjectMapper()
     * ObjectMapper} is used.
     * <p>
     * Setting a custom-configured {@code ObjectMapper} is one way to take further control of the JSON serialization
     * process. For example, an extended {@link org.codehaus.jackson.map.SerializerFactory} can be configured that
     * provides custom serializers for specific types. The other option for refining the serialization process is to use
     * Jackson's provided annotations on the types to be serialized, in which case a custom-configured ObjectMapper is
     * unnecessary.
     */
    public void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        this.objectMapper = objectMapper;
        configurePrettyPrint();
    }

    private void configurePrettyPrint() {
        if (this.prettyPrint != null) {
            this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
        }
    }

    /**
     * Return the underlying {@code ObjectMapper} for this view.
     */
    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    /**
     * Indicate whether the JSON output by this view should be prefixed with "{} &&". Default is false.
     * <p>
     * Prefixing the JSON string in this manner is used to help prevent JSON Hijacking. The prefix renders the string
     * syntactically invalid as a script so that it cannot be hijacked. This prefix does not affect the evaluation of
     * JSON, but if JSON validation is performed on the string, the prefix would need to be ignored.
     */
    public void setPrefixJson(boolean prefixJson) {
        this.prefixJson = prefixJson;
    }

    /**
     * Whether to use the {@link DefaultPrettyPrinter} when writing JSON. This is a shortcut for setting up an
     * {@code ObjectMapper} as follows:
     * 
     * <pre>
     * ObjectMapper mapper = new ObjectMapper();
     * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
     * converter.setObjectMapper(mapper);
     * </pre>
     */
    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        configurePrettyPrint();
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        JavaType javaType = getJavaType(clazz);
        return (this.objectMapper.canDeserialize(javaType) && canRead(mediaType));
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return (this.objectMapper.canSerialize(clazz) && canWrite(mediaType));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // should not be called, since we override canRead/Write instead
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException,
            HttpMessageNotReadableException {

        JavaType javaType = getJavaType(clazz);
        try {
            return this.objectMapper.readValue(inputMessage.getBody(), javaType);
        } catch (IOException ex) {
            throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void writeInternal(Object object, HttpOutputMessage outputMessage) throws IOException,
            HttpMessageNotWritableException {

        JsonEncoding encoding = getJsonEncoding(outputMessage.getHeaders().getContentType());
        JsonGenerator jsonGenerator = this.objectMapper.getFactory().createJsonGenerator(outputMessage.getBody(),
                encoding);

        try {
            if (this.prefixJson) {
                jsonGenerator.writeRaw("{} && ");
            }
            if (object instanceof DataView && ((DataView) object).hasView())
            {
                ObjectWriter writter = this.objectMapper.writerWithView(((DataView) object).getView());
                writter.writeValue(jsonGenerator, ((DataView) object).getData());
            } else {
                this.objectMapper.writeValue(jsonGenerator, object);
            }
        } catch (JsonProcessingException ex) {
            throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }

    /**
     * Return the Jackson {@link JavaType} for the specified class.
     * <p>
     * The default implementation returns {@link ObjectMapper#constructType(java.lang.reflect.Type)}, but this can be
     * overridden in subclasses, to allow for custom generic collection handling. For instance:
     * 
     * <pre class="code">
     * protected JavaType getJavaType(Class&lt;?&gt; clazz) {
     *     if (List.class.isAssignableFrom(clazz)) {
     *         return objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, MyBean.class);
     *     } else {
     *         return super.getJavaType(clazz);
     *     }
     * }
     * </pre>
     * 
     * @param clazz
     *            the class to return the java type for
     * @return the java type
     */
    protected JavaType getJavaType(Class<?> clazz) {
        return objectMapper.constructType(clazz);
    }

    /**
     * Determine the JSON encoding to use for the given content type.
     * 
     * @param contentType
     *            the media type as requested by the caller
     * @return the JSON encoding to use (never <code>null</code>)
     */
    protected JsonEncoding getJsonEncoding(MediaType contentType) {
        if (contentType != null && contentType.getCharSet() != null) {
            Charset charset = contentType.getCharSet();
            for (JsonEncoding encoding : JsonEncoding.values()) {
                if (charset.name().equals(encoding.getJavaName())) {
                    return encoding;
                }
            }
        }
        return JsonEncoding.UTF8;
    }
}
