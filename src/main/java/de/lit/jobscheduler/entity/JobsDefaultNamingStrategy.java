package de.lit.jobscheduler.entity;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Customized naming strategy: convert Java camelCase to underscore.
 * <pre>
 * Example: someColumnId -> SOME_COLUMN_ID
 * </pre>
 */
public class JobsDefaultNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

	protected String addUnderscores(String name) {
		StringBuilder buf = new StringBuilder(name.replace('.', '_'));
		for (int i = 1; i < buf.length() - 1; i++) {
			if ((Character.isLowerCase(buf.charAt(i - 1)) || Character.isDigit(buf.charAt(i - 1)))
					&& Character.isUpperCase(buf.charAt(i))
					&& Character.isLowerCase(buf.charAt(i + 1))) {
				buf.insert(i++, '_');
			}
		}
		return buf.toString().toLowerCase(Locale.ROOT);
	}

	@Override
	protected Identifier toIdentifier(String stringForm, MetadataBuildingContext buildingContext) {
		String name = addUnderscores(stringForm);
		if (name.length() > 30) {
			LoggerFactory.getLogger(getClass()).warn("Identifier {} longer then 30 chars", name);
		}
		return super.toIdentifier(name, buildingContext);
	}

	@Override
	public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
		String name = transformAttributePath( source.getAttributePath() ) + "Id";
		return toIdentifier(name, source.getBuildingContext());
	}
}
