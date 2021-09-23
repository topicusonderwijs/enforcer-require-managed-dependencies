package nl.topicus.onderwijs.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class RequireManagedDependencies implements EnforcerRule
{
	private List<String> excludes = new ArrayList<>();

	private String includedScopes = "compile,provided,runtime,test,system,import";

	private DependencyNode getNode(EnforcerRuleHelper helper) throws EnforcerRuleException
	{
		try
		{
			MavenProject project = (MavenProject) helper.evaluate("${project}");
			MavenSession session = (MavenSession) helper.evaluate("${session}");
			DependencyGraphBuilder dependencyTreeBuilder =
				helper.getComponent(DependencyGraphBuilder.class);
			ProjectBuildingRequest buildingRequest =
				new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
			buildingRequest.setProject(project);
			DependencyNode node = dependencyTreeBuilder.buildDependencyGraph(buildingRequest, null);
			return node;
		}
		catch (ExpressionEvaluationException e)
		{
			throw new EnforcerRuleException(
				"Unable to lookup an expression " + e.getLocalizedMessage(), e);
		}
		catch (ComponentLookupException e)
		{
			throw new EnforcerRuleException(
				"Unable to lookup a component " + e.getLocalizedMessage(), e);
		}
		catch (DependencyGraphBuilderException e)
		{
			throw new EnforcerRuleException(
				"Could not build dependency graph " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException
	{
		try
		{
			helper.getLog().debug("RequireManagedDependencies excluding " + excludes);
			helper.getLog().debug("RequireManagedDependencies with scopes " + includedScopes);
			DependencyNode node = getNode(helper);
			UnmanagedDependencyCollector visitor =
				new UnmanagedDependencyCollector((MavenProject) helper.evaluate("${project}"), node,
					excludes, Arrays.asList(includedScopes.split(",")));
			if (!node.accept(visitor))
			{
				throw visitor.getStoredException();
			}

			boolean fail = false;
			for (CharSequence unmanaged : visitor.getUnmanagedDependencies())
			{
				fail = true;
				helper.getLog().warn("Unmanaged dependency found: " + unmanaged);
			}
			if (fail)
			{
				throw new EnforcerRuleException(
					"Failed while enforcing releasability. See above detailed error message.");
			}
		}
		catch (Exception e)
		{
			throw new EnforcerRuleException(e.getLocalizedMessage(), e);
		}
	}

	@Override
	public String getCacheId()
	{
		return "";
	}

	@Override
	public boolean isCacheable()
	{
		return false;
	}

	@Override
	public boolean isResultValid(EnforcerRule rule)
	{
		return false;
	}
}
