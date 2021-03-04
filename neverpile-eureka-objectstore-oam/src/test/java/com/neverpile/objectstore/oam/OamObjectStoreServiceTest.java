package com.neverpile.objectstore.oam;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    properties = {
        "spring.jta.atomikos.properties.max-timeout=600000",
        "spring.jta.atomikos.properties.default-jta-timeout=600000", "spring.transaction.default-timeout=900"
    })
@ContextConfiguration(
    classes = TestConfiguration.class)
@EnableTransactionManagement
public class OamObjectStoreServiceTest extends AbstractOamObjectStoreServiceTest {

//  @Test
//  @Transactional
//  public void testThat_newElementCanBeSavedAndRetrieved() {
//    objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, defaultStream());
//  }


}
