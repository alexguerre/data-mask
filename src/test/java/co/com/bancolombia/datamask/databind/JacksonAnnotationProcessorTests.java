package co.com.bancolombia.datamask.databind;

import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.Mask;
import co.com.bancolombia.datamask.databind.unmask.StringDeserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JacksonAnnotationProcessorTests {

    @Test
    void testMaskReadOnly() {
        ObjectMapper mapper = new MaskingObjectMapper();

        Person p = new Person("Jhon Doe", "4444555566667777");

        try {
            String jsonValue = mapper.writeValueAsString(p);
            System.out.println(jsonValue);
            assertTrue(jsonValue.contains("************7777"));
            assertFalse(jsonValue.contains(DataMaskingConstants.MASKING_PREFIX));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    void testMaskingInlineFormat() {
        ObjectMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        Customer c = new Customer("Jhon Doe", "jhon.doe12@somedomain.com");

        try {
            String jsonValue = mapper.writeValueAsString(c);
            System.out.println(jsonValue);
            assertTrue(jsonValue.contains("masked_pair=jho******************.com|amhvbi5kb2UxMkBzb21lZG9tYWluLmNvbQ=="));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    void testUnmaskingInlineFormat() {
        ObjectMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        String json = "{\"name\":\"Jhon Doe\",\"email\":\"masked_pair=jho******************.com|amhvbi5kb2UxMkBzb21lZG9tYWluLmNvbQ==\"}";

        try {
            Customer customer = mapper.readValue(json, Customer.class);
            System.out.println(customer.toString());
            assertEquals("Jhon Doe", customer.getName());
            assertEquals("jhon.doe12@somedomain.com", customer.getEmail());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testMaskingAsObject() {
        ObjectMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        Company c = new Company("Acme enterprises", "9999888877776666");

        try {
            String jsonValue = mapper.writeValueAsString(c);
            System.out.println(jsonValue);
            assertTrue(jsonValue.contains("\"masked\":\"999*********6666\""));
            assertTrue(jsonValue.contains("\"enc\":\"OTk5OTg4ODg3Nzc3NjY2Ng==\""));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    void testUnmaskingFromObject() {
        ObjectMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        String json = "{\"name\":\"Acme enterprises\",\"card\":{\"masked\":\"999*********6666\",\"enc\":\"OTk5OTg4ODg3Nzc3NjY2Ng==\"}}";

        try {
            Company company = mapper.readValue(json, Company.class);
            System.out.println(company.toString());
            assertEquals("Acme enterprises", company.getName());
            assertEquals("9999888877776666", company.getCard());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testUnmaskingAsObjecMapperModule() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new StringDeserializer(new DummyDecipher()));
        mapper.registerModule(module);

        String json = "{\"name\":\"Jhon Doe\",\"email\":\"masked_pair=jho******************.com|amhvbi5kb2UxMkBzb21lZG9tYWluLmNvbQ==\"}";

        try {
            Customer customer = mapper.readValue(json, Customer.class);
            System.out.println(customer.toString());
            assertEquals("Jhon Doe", customer.getName());
            assertEquals("jhon.doe12@somedomain.com", customer.getEmail());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail();
        }

    }

    @Test
    void testNoMasking() {
        ObjectMapper mapper = new MaskingObjectMapper();
        Person p = new Person("Jhon Doe", "");
        try {
            String jsonValue = mapper.writeValueAsString(p);
            System.out.println(jsonValue);
            assertFalse(jsonValue.contains("*"));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    void testMaskingArrayOfStrings() {
        var myCipher = new DataCipher() {
            @Override
            public String cipher(String inputData) {
                return null;
            }
        };

        ObjectMapper mapper = new MaskingObjectMapper(myCipher, new DummyDecipher());
        Client c = new Client("Jhon Doe", new String[]{"1111222233334444", "5555666677778888"});
        try {
            String jsonValue = mapper.writeValueAsString(c);
            System.out.println(jsonValue);
            assertFalse(jsonValue.contains("*"));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

//    @Test
//    void testUnmaskingArrayMember() {
//        ObjectMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());
//
//        String json = "{\"name\":\"Jhon Doe\",\"card\":[\"masked_pair=************4444|MTExMTIyMjIzMzMzNDQ0NA==\",\"masked_pair=************8888|NTU1NTY2NjY3Nzc3ODg4OA==\"]}";
//
//        try {
//            Client client = mapper.readValue(json, Client.class);
//            System.out.println(client.toString());
//            assertEquals("Jhon Doe", client.getName());
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Person {
        private String name;

        @Mask(rightVisible=4)
        private String card;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Customer {
        private String name;

        @Mask(leftVisible = 3, rightVisible=4, queryOnly=false)
        private String email;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Company {
        private String name;

        @Mask(leftVisible = 3, rightVisible=4, queryOnly=false, format = DataMaskingConstants.ENCRYPTION_AS_OBJECT)
        private String card;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Client {
        private String name;

        @Mask(rightVisible=4, queryOnly = false)
        private String[] card;
    }


    public static class DummyCipher implements DataCipher {
        @Override
        public String cipher(String inputData) {
            return Base64.getEncoder().encodeToString(inputData.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static class DummyDecipher implements DataDecipher {
        @Override
        public String decipher(String inputData) {
            return new String(Base64.getDecoder().decode(inputData.getBytes(StandardCharsets.UTF_8)));
        }
    }

}
