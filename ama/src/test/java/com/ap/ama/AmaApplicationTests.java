package com.ap.ama;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// The application always runs with a profile (it selects the chat/embedding model),
// so the test does the same.
@ActiveProfiles("dev")
@SpringBootTest
class AmaApplicationTests {

	@Test
	void contextLoads() {
	}

}