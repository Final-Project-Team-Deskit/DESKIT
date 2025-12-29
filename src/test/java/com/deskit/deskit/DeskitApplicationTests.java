package com.deskit.deskit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Temporary: app config (DB/Redis/AI/Security) not stabilized yet. Re-enable after test profile is added.")
@SpringBootTest
class DeskitApplicationTests {

	@Test
	void contextLoads() {
	}

}
