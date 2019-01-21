package de.lit.jobscheduler;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class,
		TransactionalTestExecutionListener.class,
		DbUnitTestExecutionListener.class})
public abstract class SpringDbUnitTestCase {

	static {
		initSystemProperties();
	}

	public static void initSystemProperties() {
		try (InputStream in = SpringDbUnitTestCase.class.getResourceAsStream("/bootstrap.properties")) {
			if (in != null) {
				Properties props = new Properties();
				props.load(in);
				System.getProperties().putAll(props);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Autowired
	protected EntityManager em;

	@Autowired
	protected PlatformTransactionManager tx;

}
