package nl.topicus.onderwijs.maven;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class RequireManagedDependencies implements EnforcerRule {
	private DependencyNode getNode(EnforcerRuleHelper helper)
			throws EnforcerRuleException {
		try {
			MavenProject project = (MavenProject) helper.evaluate("${project}");
			DependencyGraphBuilder dependencyTreeBuilder = (DependencyGraphBuilder) helper
					.getComponent(DependencyGraphBuilder.class);
			DependencyNode node = dependencyTreeBuilder.buildDependencyGraph(
					project, null);
			return node;
		} catch (ExpressionEvaluationException e) {
			throw new EnforcerRuleException("Unable to lookup an expression "
					+ e.getLocalizedMessage(), e);
		} catch (ComponentLookupException e) {
			throw new EnforcerRuleException("Unable to lookup a component "
					+ e.getLocalizedMessage(), e);
		} catch (DependencyGraphBuilderException e) {
			throw new EnforcerRuleException("Could not build dependency graph "
					+ e.getLocalizedMessage(), e);
		}
	}

	public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
		try {
			DependencyNode node = getNode(helper);
			UnmanagedDependencyCollector visitor = new UnmanagedDependencyCollector(
					(MavenProject) helper.evaluate("${project}"), node);
			node.accept(visitor);
			boolean fail = false;
			for (CharSequence unmanaged : visitor.getUnmanagedDependencies()) {
				fail = true;
				helper.getLog()
						.warn("Unmanaged dependency found: " + unmanaged);
			}
			if (fail) {
				throw new EnforcerRuleException(
						"Failed while enforcing releasability. "
								+ "See above detailed error message.");
			}
		} catch (Exception e) {
			throw new EnforcerRuleException(e.getLocalizedMessage(), e);
		}
	}

	public String getCacheId() {
		return "";
	}

	public boolean isCacheable() {
		return false;
	}

	public boolean isResultValid(EnforcerRule rule) {
		return false;
	}
}
