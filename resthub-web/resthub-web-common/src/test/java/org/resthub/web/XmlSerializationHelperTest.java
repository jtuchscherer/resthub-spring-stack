package org.resthub.web;

import java.util.ArrayList;
import java.util.List;
import org.fest.assertions.api.Assertions;
import org.resthub.web.exception.SerializationException;
import org.resthub.web.model.SampleResource;
import org.testng.annotations.Test;

public class XmlSerializationHelperTest {

    private String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><sampleResource><description>desc</description><id>123</id><name>Albert</name></sampleResource>";

    @Test
    public void testSerialization() {
        SampleResource r = new SampleResource();
        r.setId(123L);
        r.setName("Albert");
        r.setDescription("desc");
        String result = XmlHelper.serialize(r);
        Assertions.assertThat(result).contains("<id>123</id>");
        Assertions.assertThat(result).contains("<name>Albert</name>");
        Assertions.assertThat(result).contains("<description>desc</description>");
    }

//    We wait for this issue resolution https://github.com/FasterXML/jackson-dataformat-xml/issues/38
//    @Test
//    public void testListSerialization() {
//        SampleResource r1 = new SampleResource();
//        r1.setId(123L);
//        r1.setName("Albert");
//        r1.setDescription("desc");
//        
//        SampleResource r2 = new SampleResource();
//        r2.setId(123L);
//        r2.setName("Albert");
//        r2.setDescription("desc");
//        
//        List<SampleResource> l = new ArrayList<SampleResource>();
//        l.add(r1);
//        l.add(r2);
//        
//        String result = XmlHelper.serialize(l);
//        Assertions.assertThat(result).contains("<id>123</id>");
//        Assertions.assertThat(result).contains("<name>Albert</name>");
//        Assertions.assertThat(result).contains("<description>desc</description>");
//    }

    @Test
    public void testDeserialization() {
        SampleResource r = (SampleResource) XmlHelper.deserialize(xml, SampleResource.class);
        Assertions.assertThat(r.getId()).isEqualTo(123);
        Assertions.assertThat(r.getName()).isEqualTo("Albert");
        Assertions.assertThat(r.getDescription()).isEqualTo("desc");
    }

    @Test(expectedExceptions = SerializationException.class)
    public void testInvalidDeserialization() {
        XmlHelper.deserialize("Invalid content", SampleResource.class);
    }
}
