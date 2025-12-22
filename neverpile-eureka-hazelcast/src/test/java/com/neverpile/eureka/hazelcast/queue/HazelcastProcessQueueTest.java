package com.neverpile.eureka.hazelcast.queue;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.neverpile.eureka.hazelcast.TestConfig;
import com.neverpile.eureka.queue.AbstractProcessQueueTest;

@SpringBootTest
@ContextConfiguration(classes = TestConfig.class)
@EnableAutoConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class HazelcastProcessQueueTest extends AbstractProcessQueueTest {

}
